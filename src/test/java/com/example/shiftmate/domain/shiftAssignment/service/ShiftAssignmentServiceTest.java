package com.example.shiftmate.domain.shiftAssignment.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.example.shiftmate.domain.employeePreference.entity.EmployeePreference;
import com.example.shiftmate.domain.employeePreference.entity.PreferenceType;
import com.example.shiftmate.domain.employeePreference.repository.EmployeePreferenceRepository;
import com.example.shiftmate.domain.shiftAssignment.repository.ShiftAssignmentRepository;
import com.example.shiftmate.domain.shiftTemplate.entity.DayType;
import com.example.shiftmate.domain.shiftTemplate.entity.ShiftTemplate;
import com.example.shiftmate.domain.shiftTemplate.entity.ShiftType;
import com.example.shiftmate.domain.shiftTemplate.repository.ShiftTemplateRepository;
import com.example.shiftmate.domain.store.entity.Store;
import com.example.shiftmate.domain.store.repository.StoreRepository;
import com.example.shiftmate.domain.storeMember.entity.Department;
import com.example.shiftmate.domain.storeMember.entity.MemberStatus;
import com.example.shiftmate.domain.storeMember.entity.StoreMember;
import com.example.shiftmate.domain.storeMember.entity.StoreRole;
import com.example.shiftmate.domain.storeMember.repository.StoreMemberRepository;
import com.example.shiftmate.domain.user.entity.AuthProvider;
import com.example.shiftmate.domain.user.entity.User;
import com.example.shiftmate.domain.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class ShiftAssignmentServiceTest {

    @Autowired private ShiftAssignmentService shiftAssignmentService;
    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private StoreMemberRepository storeMemberRepository;
    @Autowired private ShiftTemplateRepository shiftTemplateRepository;
    @Autowired private EmployeePreferenceRepository employeePreferenceRepository;
    @Autowired private ShiftAssignmentRepository shiftAssignmentRepository;

    private static final int MEMBER_COUNT = 10000; // 늘려가며 테스트
    private static final LocalDate TEST_WEEK_START = LocalDate.of(2026, 6, 15); // 월요일

    private User managerUser;
    private Store store;
    private StoreMember manager;
    private List<User> employeeUsers = new ArrayList<>();
    private List<StoreMember> employees = new ArrayList<>();
    private List<ShiftTemplate> templates = new ArrayList<>();



    @BeforeEach
    void setUp(){
        // 1. 매니저 유저 생성
        managerUser = userRepository.save(User.builder()
                                              .email("manager@test.com")
                                              .name("테스트매니저")
                                              .password("password")
                                              .phoneNumber("010-0000-0000")
                                              .provider(AuthProvider.LOCAL)
                                              .profileCompleted(true)
                                              .build());

        // 2. 가게 생성
        store = storeRepository.save(Store.builder()
                                         .name("테스트가게")
                                         .openTime(LocalTime.of(9, 0))
                                         .closeTime(LocalTime.of(22, 0))
                                         .nShifts(3)
                                         .brn("000-00-00000")
                                         .user(managerUser)
                                         .build());

        // 3. 매니저 StoreMember 생성
        manager = storeMemberRepository.save(StoreMember.builder()
                                                 .store(store)
                                                 .user(managerUser)
                                                 .role(StoreRole.MANAGER)
                                                 .department(Department.HALL)
                                                 .minHoursPerWeek(0)
                                                 .status(MemberStatus.ACTIVE)
                                                 .build());

        // 4. ShiftTemplate 3개 생성
        templates.add(shiftTemplateRepository.save(ShiftTemplate.builder()
                                                       .store(store).name("오픈")
                                                       .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(14, 0))
                                                       .requiredStaff(2).shiftType(ShiftType.NORMAL).dayType(
                DayType.WEEKDAY)
                                                       .build()));
        templates.add(shiftTemplateRepository.save(ShiftTemplate.builder()
                                                       .store(store).name("미들")
                                                       .startTime(LocalTime.of(13, 0)).endTime(LocalTime.of(18, 0))
                                                       .requiredStaff(2).shiftType(ShiftType.NORMAL).dayType(DayType.WEEKDAY)
                                                       .build()));
        templates.add(shiftTemplateRepository.save(ShiftTemplate.builder()
                                                       .store(store).name("마감")
                                                       .startTime(LocalTime.of(17, 0)).endTime(LocalTime.of(22, 0))
                                                       .requiredStaff(2).shiftType(ShiftType.NORMAL).dayType(DayType.WEEKDAY)
                                                       .build()));

        // 5. 직원 생성 (MEMBER_COUNT명)
        for (int i = 0; i < MEMBER_COUNT; i++) {
            User user = userRepository.save(User.builder()
                                                .email("employee" + i + "@test.com")
                                                .name("직원" + i)
                                                .password("password")
                                                .phoneNumber("010-" + String.format("%04d", i) + "-0000")
                                                .provider(AuthProvider.LOCAL)
                                                .profileCompleted(true)
                                                .build());
            employeeUsers.add(user);

            StoreMember employee = storeMemberRepository.save(StoreMember.builder()
                                                                  .store(store).user(user)
                                                                  .role(StoreRole.EMPLOYEE)
                                                                  .department(Department.HALL)
                                                                  .minHoursPerWeek(20)
                                                                  .status(MemberStatus.ACTIVE)
                                                                  .build());
            employees.add(employee);
        }

        // 6. EmployeePreference 생성 (직원 × 템플릿 × 7일)
        List<EmployeePreference> preferences = new ArrayList<>();
        for (StoreMember employee : employees) {
            for (ShiftTemplate template : templates) {
                for (DayOfWeek day : DayOfWeek.values()) {
                    preferences.add(EmployeePreference.builder()
                                        .member(employee)
                                        .shiftTemplate(template)
                                        .dayOfWeek(day)
                                        .type(PreferenceType.PREFERRED)
                                        .build());
                }
            }
        }
        employeePreferenceRepository.saveAll(preferences);
    }

    @AfterEach
    void cleanUp(){
        // @Modifying 쿼리 대신 find 후 deleteAll 사용 (트랜잭션 자체 관리)
        List<com.example.shiftmate.domain.shiftAssignment.entity.ShiftAssignment> assignments =
            shiftAssignmentRepository.findAllByStoreIdAndDateBetween(
                store.getId(), TEST_WEEK_START, TEST_WEEK_START.plusDays(6)
            ).orElse(List.of());
        shiftAssignmentRepository.deleteAll(assignments);

        List<EmployeePreference> prefs = new ArrayList<>();
        for (StoreMember employee : employees) {
            prefs.addAll(employeePreferenceRepository.findByMemberId(employee.getId()));
        }
        employeePreferenceRepository.deleteAll(prefs);

        storeMemberRepository.deleteAll(employees);
        storeMemberRepository.delete(manager);
        shiftTemplateRepository.deleteAll(templates);
        storeRepository.delete(store);
        userRepository.deleteAll(employeeUsers);
        userRepository.delete(managerUser);

        employeeUsers.clear();
        employees.clear();
        templates.clear();
    }

    @Test
    @DisplayName("직원 10000명 기준 스케줄 자동 생성 성능 측정")
    void createSchedule() {
        // given
        System.out.println("=== 테스트 시작 ===");
        System.out.println("직원 수: " + MEMBER_COUNT);
        System.out.println("템플릿 수: " + templates.size());
        System.out.println("총 Preference 수: " + (MEMBER_COUNT * templates.size() * 7));

        // when
        long start = System.currentTimeMillis();

        shiftAssignmentService.createSchedule(store.getId(), TEST_WEEK_START, managerUser.getId());

        long elapsed = System.currentTimeMillis() - start;

        // then
        System.out.println("=== 테스트 결과 ===");
        System.out.println("총 소요 시간: " + elapsed + "ms");

        long assignmentCount = shiftAssignmentRepository
                                   .findAllByStoreIdAndDateBetween(store.getId(), TEST_WEEK_START, TEST_WEEK_START.plusDays(6))
                                   .orElse(List.of())
                                   .size();
        System.out.println("생성된 스케줄 수: " + assignmentCount);
    }


}