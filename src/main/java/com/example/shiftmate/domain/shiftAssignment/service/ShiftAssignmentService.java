package com.example.shiftmate.domain.shiftAssignment.service;

import com.example.shiftmate.domain.employeePreference.entity.EmployeePreference;
import com.example.shiftmate.domain.employeePreference.entity.PreferenceType;
import com.example.shiftmate.domain.employeePreference.repository.EmployeePreferenceRepository;
import com.example.shiftmate.domain.shiftAssignment.dto.response.MyScheduleResDto;
import com.example.shiftmate.domain.shiftAssignment.dto.response.ScheduleResDto;
import com.example.shiftmate.domain.shiftAssignment.entity.ShiftAssignment;
import com.example.shiftmate.domain.shiftAssignment.repository.ShiftAssignmentRepository;
import com.example.shiftmate.domain.shiftTemplate.entity.ShiftTemplate;
import com.example.shiftmate.domain.shiftTemplate.repository.ShiftTemplateRepository;
import com.example.shiftmate.domain.store.entity.Store;
import com.example.shiftmate.domain.store.repository.StoreRepository;
import com.example.shiftmate.domain.storeMember.entity.MemberStatus;
import com.example.shiftmate.domain.storeMember.entity.StoreMember;
import com.example.shiftmate.domain.storeMember.entity.StoreRole;
import com.example.shiftmate.domain.storeMember.repository.StoreMemberRepository;
import com.example.shiftmate.global.exception.CustomException;
import com.example.shiftmate.global.exception.ErrorCode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftAssignmentService {

    private static final int DAYS_IN_WEEK = 7;
    private static final int DEFAULT_REQUIRED_STAFF = 1;

    private final EmployeePreferenceRepository employeePreferenceRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;
    private final StoreRepository storeRepository;
    private final StoreMemberRepository storeMemberRepository;

    @Transactional
    public void createSchedule(Long storeId, LocalDate weekStartDate, Long userId) {

        if (!storeMemberRepository.existsByStoreIdAndUserIdAndRoleAndDeletedAtIsNull(storeId, userId, StoreRole.MANAGER)) {
            throw new CustomException(ErrorCode.NOT_AUTHORIZED);
        }

        if (shiftAssignmentRepository.existsByStoreIdAndWorkDate(storeId, weekStartDate)) {
            throw new CustomException(ErrorCode.WEEK_ALREADY_EXISTS);
        }

        if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new CustomException(ErrorCode.NOT_MONDAY_START_DATE);
        }

        List<ShiftTemplate> templates = shiftTemplateRepository.findByStoreId(storeId).orElseThrow(
            () -> new CustomException(ErrorCode.TEMPLATE_NOT_FOUND)
        );
        if (templates.isEmpty()) {
            throw new CustomException(ErrorCode.TEMPLATE_NOT_FOUND);
        }

        List<Long> templateIds = templates.stream()
            .map(ShiftTemplate::getId)
            .toList();

        List<EmployeePreference> preferences = employeePreferenceRepository
            .findByTypeNotAndShiftTemplate_IdIn(PreferenceType.UNAVAILABLE, templateIds);

        Map<Long, StoreMember> memberById = new HashMap<>();
        for (EmployeePreference preference : preferences) {
            // N+1 발생 지점
            StoreMember member = preference.getMember();
            if (member == null || !isSchedulableMember(member)) {
                continue;
            }
            memberById.put(member.getId(), member);
        }

        Map<Long, Integer> minimumMinutesByMember = new HashMap<>();
        memberById.forEach((memberId, member) -> {
            int minimumHours = Optional.ofNullable(member.getMinHoursPerWeek()).orElse(0);
            minimumMinutesByMember.put(memberId, Math.max(0, minimumHours) * 60);
        });

        Map<SlotKey, Map<Long, PreferenceType>> preferenceLookup = buildPreferenceLookup(
            preferences, templateIds
        );
        List<SlotAssignmentTarget> slots = buildWeeklyTargets(
            templates, weekStartDate, preferenceLookup, memberById
        );

        List<ShiftAssignment> finalAssignments = new ArrayList<>();
        Map<Long, Integer> assignedMinutesByMember = new HashMap<>();
        Map<Long, Map<LocalDate, List<MemberWindow>>> assignedWindowsByMember = new HashMap<>();

        assignMinimumHoursFirst(
            slots,
            minimumMinutesByMember,
            assignedMinutesByMember,
            assignedWindowsByMember,
            finalAssignments
        );

        fillRemainingSlots(
            slots,
            minimumMinutesByMember,
            assignedMinutesByMember,
            assignedWindowsByMember,
            finalAssignments
        );

        shiftAssignmentRepository.saveAll(finalAssignments);
    }

    private Map<SlotKey, Map<Long, PreferenceType>> buildPreferenceLookup(
        List<EmployeePreference> preferences,
        List<Long> templateIds
    ) {
        Map<SlotKey, Map<Long, PreferenceType>> lookup = new HashMap<>();
        Map<Long, Boolean> validTemplateIds = templateIds.stream()
            .collect(Collectors.toMap(id -> id, id -> Boolean.TRUE));

        for (EmployeePreference preference : preferences) {
            // N+1 발생 지점
            ShiftTemplate template = preference.getShiftTemplate();
            // N+1 발생 지점
            StoreMember member = preference.getMember();
            DayOfWeek dayOfWeek = preference.getDayOfWeek();
            PreferenceType type = preference.getType();

            if (template == null || member == null || dayOfWeek == null || type == null) {
                continue;
            }
            if (!Boolean.TRUE.equals(validTemplateIds.get(template.getId()))) {
                continue;
            }
            if (!isSchedulableMember(member)) {
                continue;
            }

            SlotKey slotKey = new SlotKey(template.getId(), dayOfWeek);
            Map<Long, PreferenceType> byMember = lookup.computeIfAbsent(
                slotKey, ignored -> new HashMap<>()
            );
            byMember.merge(member.getId(), type, this::pickHigherPriority);
        }

        return lookup;
    }

    private List<SlotAssignmentTarget> buildWeeklyTargets(
        List<ShiftTemplate> templates,
        LocalDate weekStartDate,
        Map<SlotKey, Map<Long, PreferenceType>> preferenceLookup,
        Map<Long, StoreMember> memberById
    ) {
        List<SlotAssignmentTarget> slots = new ArrayList<>();

        for (ShiftTemplate template : templates) {
            int requiredStaff = resolveRequiredStaff(template);
            if (requiredStaff <= 0) {
                continue;
            }

            for (int dayOffset = 0; dayOffset < DAYS_IN_WEEK; dayOffset++) {
                LocalDate workDate = weekStartDate.plusDays(dayOffset);
                DayOfWeek dayOfWeek = workDate.getDayOfWeek();
                SlotKey slotKey = new SlotKey(template.getId(), dayOfWeek);
                Map<Long, PreferenceType> memberPreferences = preferenceLookup.getOrDefault(
                    slotKey,
                    Map.of()
                );

                List<SlotCandidate> candidates = memberPreferences.entrySet().stream()
                    .map(entry -> {
                        StoreMember member = memberById.get(entry.getKey());
                        if (member == null) {
                            return null;
                        }
                        return new SlotCandidate(member, entry.getValue());
                    })
                    .filter(candidate -> candidate != null)
                    .toList();

                slots.add(new SlotAssignmentTarget(template, workDate, requiredStaff, candidates));
            }
        }

        slots.sort(
            Comparator.comparingInt((SlotAssignmentTarget slot) -> slot.candidates().size())
                .thenComparingInt((SlotAssignmentTarget slot) -> -slot.requiredStaff())
                .thenComparing(SlotAssignmentTarget::workDate)
                .thenComparing(slot -> slot.template().getStartTime())
        );

        return slots;
    }

    private void assignMinimumHoursFirst(
        List<SlotAssignmentTarget> slots,
        Map<Long, Integer> minimumMinutesByMember,
        Map<Long, Integer> assignedMinutesByMember,
        Map<Long, Map<LocalDate, List<MemberWindow>>> assignedWindowsByMember,
        List<ShiftAssignment> finalAssignments
    ) {
        boolean progress;

        do {
            progress = false;
            List<Long> membersWithDeficit = minimumMinutesByMember.keySet().stream()
                .filter(memberId -> getRemainingDeficitMinutes(
                    memberId,
                    minimumMinutesByMember,
                    assignedMinutesByMember
                ) > 0)
                .sorted((left, right) -> Integer.compare(
                    getRemainingDeficitMinutes(right, minimumMinutesByMember, assignedMinutesByMember),
                    getRemainingDeficitMinutes(left, minimumMinutesByMember, assignedMinutesByMember)
                ))
                .toList();

            for (Long memberId : membersWithDeficit) {
                AssignmentOption option = pickBestAssignmentForMember(
                    memberId,
                    slots,
                    assignedWindowsByMember
                );

                if (option == null) {
                    continue;
                }

                assignMember(
                    option.slot(),
                    option.candidate(),
                    assignedMinutesByMember,
                    assignedWindowsByMember,
                    finalAssignments
                );
                progress = true;
            }
        } while (progress);
    }

    private AssignmentOption pickBestAssignmentForMember(
        Long memberId,
        List<SlotAssignmentTarget> slots,
        Map<Long, Map<LocalDate, List<MemberWindow>>> assignedWindowsByMember
    ) {
        AssignmentOption bestOption = null;

        for (SlotAssignmentTarget slot : slots) {
            if (!slot.hasVacancy()) {
                continue;
            }

            SlotCandidate candidate = slot.findCandidate(memberId);
            if (candidate == null) {
                continue;
            }
            if (!canAssign(slot, candidate.member(), assignedWindowsByMember)) {
                continue;
            }

            int availableCandidates = countAssignableCandidates(slot, assignedWindowsByMember);
            int urgencyScore = availableCandidates - slot.remaining();
            int preferenceScore = preferencePriority(candidate.preferenceType());
            int durationMinutes = calculateDurationMinutes(
                slot.template().getStartTime(),
                slot.template().getEndTime()
            );

            AssignmentOption currentOption = new AssignmentOption(
                slot,
                candidate,
                urgencyScore,
                preferenceScore,
                durationMinutes
            );

            if (bestOption == null) {
                bestOption = currentOption;
                continue;
            }

            if (currentOption.urgencyScore() < bestOption.urgencyScore()) {
                bestOption = currentOption;
                continue;
            }
            if (currentOption.urgencyScore() > bestOption.urgencyScore()) {
                continue;
            }

            if (currentOption.preferenceScore() > bestOption.preferenceScore()) {
                bestOption = currentOption;
                continue;
            }
            if (currentOption.preferenceScore() < bestOption.preferenceScore()) {
                continue;
            }

            if (currentOption.durationMinutes() > bestOption.durationMinutes()) {
                bestOption = currentOption;
            }
        }

        return bestOption;
    }

    private void fillRemainingSlots(
        List<SlotAssignmentTarget> slots,
        Map<Long, Integer> minimumMinutesByMember,
        Map<Long, Integer> assignedMinutesByMember,
        Map<Long, Map<LocalDate, List<MemberWindow>>> assignedWindowsByMember,
        List<ShiftAssignment> finalAssignments
    ) {
        for (SlotAssignmentTarget slot : slots) {
            while (slot.hasVacancy()) {
                SlotCandidate selected = pickBestCandidateForSlot(
                    slot,
                    minimumMinutesByMember,
                    assignedMinutesByMember,
                    assignedWindowsByMember
                );

                if (selected == null) {
                    break;
                }

                assignMember(
                    slot,
                    selected,
                    assignedMinutesByMember,
                    assignedWindowsByMember,
                    finalAssignments
                );
            }
        }
    }

    private SlotCandidate pickBestCandidateForSlot(
        SlotAssignmentTarget slot,
        Map<Long, Integer> minimumMinutesByMember,
        Map<Long, Integer> assignedMinutesByMember,
        Map<Long, Map<LocalDate, List<MemberWindow>>> assignedWindowsByMember
    ) {
        return slot.candidates().stream()
            .filter(candidate -> canAssign(slot, candidate.member(), assignedWindowsByMember))
            .sorted(buildCandidateComparator(minimumMinutesByMember, assignedMinutesByMember))
            .findFirst()
            .orElse(null);
    }

    private Comparator<SlotCandidate> buildCandidateComparator(
        Map<Long, Integer> minimumMinutesByMember,
        Map<Long, Integer> assignedMinutesByMember
    ) {
        return Comparator
            .comparingInt((SlotCandidate candidate) -> getRemainingDeficitMinutes(
                candidate.member().getId(),
                minimumMinutesByMember,
                assignedMinutesByMember
            ))
            .reversed()
            .thenComparing(
                Comparator.comparingInt(
                    (SlotCandidate candidate) -> preferencePriority(candidate.preferenceType())
                ).reversed()
            )
            .thenComparingInt(candidate ->
                assignedMinutesByMember.getOrDefault(candidate.member().getId(), 0))
            .thenComparingLong(candidate -> candidate.member().getId());
    }

    private void assignMember(
        SlotAssignmentTarget slot,
        SlotCandidate candidate,
        Map<Long, Integer> assignedMinutesByMember,
        Map<Long, Map<LocalDate, List<MemberWindow>>> assignedWindowsByMember,
        List<ShiftAssignment> finalAssignments
    ) {
        LocalTime startTime = slot.template().getStartTime();
        LocalTime endTime = slot.template().getEndTime();

        LocalDateTime updatedStartTime = slot.workDate().atTime(startTime);
        LocalDateTime updatedEndTime = slot.workDate().atTime(endTime);
        if (updatedEndTime.isBefore(updatedStartTime)) {
            updatedEndTime = updatedEndTime.plusDays(1);
        }

        ShiftAssignment assignment = ShiftAssignment.builder()
            .member(candidate.member())
            .shiftTemplate(slot.template())
            .workDate(slot.workDate())
            .updatedStartTime(updatedStartTime)
            .updatedEndTime(updatedEndTime)
            .build();

        finalAssignments.add(assignment);
        slot.assignOne();

        int durationMinutes = calculateDurationMinutes(startTime, endTime);
        assignedMinutesByMember.merge(candidate.member().getId(), durationMinutes, Integer::sum);

        assignedWindowsByMember
            .computeIfAbsent(candidate.member().getId(), ignored -> new HashMap<>())
            .computeIfAbsent(slot.workDate(), ignored -> new ArrayList<>())
            .add(new MemberWindow(startTime, endTime));
    }

    private int getRemainingDeficitMinutes(
        Long memberId,
        Map<Long, Integer> minimumMinutesByMember,
        Map<Long, Integer> assignedMinutesByMember
    ) {
        int minimum = minimumMinutesByMember.getOrDefault(memberId, 0);
        int assigned = assignedMinutesByMember.getOrDefault(memberId, 0);
        return Math.max(0, minimum - assigned);
    }

    private int countAssignableCandidates(
        SlotAssignmentTarget slot,
        Map<Long, Map<LocalDate, List<MemberWindow>>> assignedWindowsByMember
    ) {
        int assignable = 0;
        for (SlotCandidate candidate : slot.candidates()) {
            if (canAssign(slot, candidate.member(), assignedWindowsByMember)) {
                assignable++;
            }
        }
        return assignable;
    }

    private boolean canAssign(
        SlotAssignmentTarget slot,
        StoreMember member,
        Map<Long, Map<LocalDate, List<MemberWindow>>> assignedWindowsByMember
    ) {
        Map<LocalDate, List<MemberWindow>> byDate = assignedWindowsByMember.get(member.getId());
        if (byDate == null) {
            return true;
        }

        List<MemberWindow> windows = byDate.get(slot.workDate());
        if (windows == null || windows.isEmpty()) {
            return true;
        }

        LocalTime slotStart = slot.template().getStartTime();
        LocalTime slotEnd = slot.template().getEndTime();
        for (MemberWindow window : windows) {
            if (isOverlapping(window.startTime(), window.endTime(), slotStart, slotEnd)) {
                return false;
            }
        }

        return true;
    }

    private boolean isOverlapping(
        LocalTime leftStart,
        LocalTime leftEnd,
        LocalTime rightStart,
        LocalTime rightEnd
    ) {
        return leftStart.isBefore(rightEnd) && rightStart.isBefore(leftEnd);
    }

    private boolean isSchedulableMember(StoreMember member) {
        if (member.getDeletedAt() != null) {
            return false;
        }
        return member.getStatus() == null || member.getStatus() == MemberStatus.ACTIVE;
    }

    private int resolveRequiredStaff(ShiftTemplate template) {
        Integer requiredStaff = template.getRequiredStaff();
        if (requiredStaff == null) {
            return DEFAULT_REQUIRED_STAFF;
        }
        return Math.max(0, requiredStaff);
    }

    private int calculateDurationMinutes(LocalTime startTime, LocalTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            minutes += 24 * 60;
        }
        return (int) minutes;
    }

    private PreferenceType pickHigherPriority(PreferenceType current, PreferenceType candidate) {
        return preferencePriority(candidate) > preferencePriority(current) ? candidate : current;
    }

    private int preferencePriority(PreferenceType preferenceType) {
        if (preferenceType == PreferenceType.PREFERRED) {
            return 2;
        }
        if (preferenceType == PreferenceType.NATURAL) {
            return 1;
        }
        return 0;
    }

    public List<ScheduleResDto> getSchedule(Long storeId, LocalDate weekStartDate) {

        Store store = storeRepository.findById(storeId).orElseThrow(
            () -> new CustomException(ErrorCode.STORE_NOT_FOUND)
        );

        LocalDate weekEndDate = weekStartDate.plusDays(6);

        List<ShiftAssignment> assignments = shiftAssignmentRepository
            .findAllByStoreIdAndDateBetween(store.getId(), weekStartDate, weekEndDate).orElseThrow(
                () -> new CustomException(ErrorCode.SHIFT_ASSIGNMENT_NOT_FOUND)
            );

        return assignments.stream()
            .map(ScheduleResDto::from)
            .collect(Collectors.toList());

    }

    public List<MyScheduleResDto> getScheduleByMember(Long storeId, Long userId) {

        // 가게 존재 여부 확인
        storeRepository.findById(storeId).orElseThrow(
            () -> new CustomException(ErrorCode.STORE_NOT_FOUND)
        );

        // 해당 가게의 직원인지 확인 (storeId와 userId 모두 검증)
        StoreMember storeMember = storeMemberRepository.findByStore_IdAndUser_Id(storeId, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.STORE_MEMBER_NOT_FOUND));

        // 해당 직원의 모든 스케줄 조회
        List<ShiftAssignment> assignments = shiftAssignmentRepository
            .findAllByStoreIdAndMemberId(storeId, storeMember.getId());

        return assignments.stream()
            .map(MyScheduleResDto::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSchedule(Long storeId, LocalDate weekStartDate, Long userId) {
        if (!storeMemberRepository.existsByStoreIdAndUserIdAndRoleAndDeletedAtIsNull(storeId, userId, StoreRole.MANAGER)) {
            throw new CustomException(ErrorCode.NOT_AUTHORIZED);
        }
        if (!storeRepository.existsById(storeId)) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }

        LocalDate weekEndDate = weekStartDate.plusDays(6);

        if (!shiftAssignmentRepository.existsByStoreIdAndWorkDateBetween(storeId, weekStartDate, weekEndDate)) {
            throw new CustomException(ErrorCode.SHIFT_ASSIGNMENT_NOT_FOUND);
        }

        shiftAssignmentRepository.deleteByStoreIdAndWorkDateBetween(storeId, weekStartDate, weekEndDate);
    }

    private record SlotKey(Long templateId, DayOfWeek dayOfWeek) {
    }

    private record MemberWindow(LocalTime startTime, LocalTime endTime) {
    }

    private record SlotCandidate(StoreMember member, PreferenceType preferenceType) {
    }

    private record AssignmentOption(
        SlotAssignmentTarget slot,
        SlotCandidate candidate,
        int urgencyScore,
        int preferenceScore,
        int durationMinutes
    ) {
    }

    private static class SlotAssignmentTarget {
        private final ShiftTemplate template;
        private final LocalDate workDate;
        private final int requiredStaff;
        private final List<SlotCandidate> candidates;
        private int assignedCount;

        private SlotAssignmentTarget(
            ShiftTemplate template,
            LocalDate workDate,
            int requiredStaff,
            List<SlotCandidate> candidates
        ) {
            this.template = template;
            this.workDate = workDate;
            this.requiredStaff = requiredStaff;
            this.candidates = candidates;
            this.assignedCount = 0;
        }

        private ShiftTemplate template() {
            return template;
        }

        private LocalDate workDate() {
            return workDate;
        }

        private int requiredStaff() {
            return requiredStaff;
        }

        private List<SlotCandidate> candidates() {
            return candidates;
        }

        private boolean hasVacancy() {
            return assignedCount < requiredStaff;
        }

        private int remaining() {
            return requiredStaff - assignedCount;
        }

        private void assignOne() {
            assignedCount++;
        }

        private SlotCandidate findCandidate(Long memberId) {
            for (SlotCandidate candidate : candidates) {
                if (candidate.member().getId().equals(memberId)) {
                    return candidate;
                }
            }
            return null;
        }
    }
}
