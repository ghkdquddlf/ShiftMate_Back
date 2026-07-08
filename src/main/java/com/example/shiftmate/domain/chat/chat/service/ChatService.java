package com.example.shiftmate.domain.chat.chat.service;


import com.example.shiftmate.domain.chat.chat.dto.request.CreateChatReqDto;
import com.example.shiftmate.domain.chat.chat.dto.response.ChatParticipantSummaryResDto;
import com.example.shiftmate.domain.chat.chat.dto.response.ChatRoomResDto;
import com.example.shiftmate.domain.chat.chat.dto.response.CreateChatResDto;
import com.example.shiftmate.domain.chat.chat.entity.Chat;
import com.example.shiftmate.domain.chat.chat.entity.ChatType;
import com.example.shiftmate.domain.chat.chat.repository.ChatRepository;
import com.example.shiftmate.domain.chat.message.entity.Message;
import com.example.shiftmate.domain.chat.message.repository.MessageRepository;
import com.example.shiftmate.domain.chat.participant.entity.Participant;
import com.example.shiftmate.domain.chat.participant.repository.ParticipantRepository;
import com.example.shiftmate.domain.store.entity.Store;
import com.example.shiftmate.domain.store.repository.StoreRepository;
import com.example.shiftmate.domain.storeMember.entity.StoreMember;
import com.example.shiftmate.domain.storeMember.repository.StoreMemberRepository;
import com.example.shiftmate.global.exception.CustomException;
import com.example.shiftmate.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ParticipantRepository participantRepository;
    private final StoreRepository storeRepository;
    private final StoreMemberRepository storeMemberRepository;

    @Transactional
    public CreateChatResDto createChatroom(Long storeId, CreateChatReqDto reqDto, Long id) {
        Store store = findStoreById(storeId);
        StoreMember requester = findStoreMember(storeId, id, ErrorCode.STORE_MEMBER_ACCESS_DENIED);

        List<Long> participantStoreMemberIds = normalizeParticipantIds(reqDto.getParticipantIds(), requester.getId());
        validateChatRequest(reqDto.getChatType(), participantStoreMemberIds);

        List<StoreMember> storeMembers = findByStoreMembers(participantStoreMemberIds, storeId);
        Chat chat = chatRepository.save(Chat.of(store, reqDto.getChatType(), reqDto.getRoomName()));

        List<Participant> participants = new ArrayList<>();
        participants.add(Participant.of(chat, requester));
        storeMembers.forEach(storeMember -> participants.add(Participant.of(chat, storeMember)));
        participantRepository.saveAll(participants);

        return CreateChatResDto.of(chat.getId());
    }

    public List<ChatRoomResDto> getChatRooms(Long storeId, Long userId) {
        StoreMember requester = findStoreMember(storeId, userId, ErrorCode.STORE_MEMBER_ACCESS_DENIED);
        List<Participant> requesterChats = participantRepository.findByStoreMemberIdWithChat(requester.getId());

        if (requesterChats.isEmpty()) {
            return List.of();
        }

        Map<Long, Chat> chatsById = new LinkedHashMap<>();
        requesterChats.forEach(participant -> chatsById.putIfAbsent(participant.getChat().getId(), participant.getChat()));

        List<Long> chatIds = new ArrayList<>(chatsById.keySet());
        Map<Long, Message> latestMessagesByChatId = messageRepository.findLatestMessagesByChatIds(chatIds).stream()
            .collect(java.util.stream.Collectors.toMap(message -> message.getChat().getId(), message -> message));

        return chatsById.values().stream()
            .sorted(Comparator.comparing(
                (Chat chat) -> latestMessagesByChatId.containsKey(chat.getId())
                    ? latestMessagesByChatId.get(chat.getId()).getCreatedAt()
                    : chat.getCreatedAt()
            ).reversed())
            .map(chat -> ChatRoomResDto.from(
                chat,
                resolveRoomName(chat, requester.getId()),
                latestMessagesByChatId.get(chat.getId()),
                chat.getParticipants().stream()
                    .map(Participant::getStoreMember)
                    .map(ChatParticipantSummaryResDto::from)
                    .toList()
            ))
            .toList();
    }

    private List<Long> normalizeParticipantIds(List<Long> participantIds, Long requesterStoreMemberId) {
        if (participantIds == null || participantIds.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return participantIds.stream()
            .filter(Objects::nonNull)
            .filter(participantId -> !participantId.equals(requesterStoreMemberId))
            .distinct()
            .toList();
    }

    private void validateChatRequest(ChatType chatType, List<Long> participantStoreMemberIds) {
        if (chatType == null || participantStoreMemberIds.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (chatType == ChatType.DIRECT && participantStoreMemberIds.size() != 1) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (chatType == ChatType.GROUP && participantStoreMemberIds.size() < 2) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private List<StoreMember> findByStoreMembers(List<Long> participantStoreMemberIds, Long storeId) {
        return participantStoreMemberIds.stream()
            .map(participantStoreMemberId -> findStoreMemberById(participantStoreMemberId, storeId))
            .toList();
    }

    private StoreMember findStoreMember(Long storeId, Long userId, ErrorCode errorCode) {
        return storeMemberRepository.findByStoreIdAndUserId(storeId, userId)
            .orElseThrow(() -> new CustomException(errorCode));
    }

    private StoreMember findStoreMemberById(Long storeMemberId, Long storeId) {
        StoreMember storeMember = storeMemberRepository.findByIdAndDeletedAtIsNull(storeMemberId)
            .orElseThrow(() -> new CustomException(ErrorCode.STORE_MEMBER_NOT_FOUND));

        if (!storeMember.getStore().getId().equals(storeId)) {
            throw new CustomException(ErrorCode.STORE_MEMBER_STORE_ID_MISMATCH);
        }

        return storeMember;
    }

    private Store findStoreById(Long storeId) {
        return storeRepository.findById(storeId)
            .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
    }

    private String resolveRoomName(Chat chat, Long requesterStoreMemberId) {
        if (chat.getRoomName() != null && !chat.getRoomName().isBlank()) {
            return chat.getRoomName();
        }

        List<String> otherParticipantNames = chat.getParticipants().stream()
            .map(Participant::getStoreMember)
            .filter(storeMember -> !storeMember.getId().equals(requesterStoreMemberId))
            .map(storeMember -> storeMember.getUser().getName())
            .distinct()
            .toList();

        if (otherParticipantNames.isEmpty()) {
            return "채팅방";
        }

        return String.join(", ", otherParticipantNames);
    }
}
