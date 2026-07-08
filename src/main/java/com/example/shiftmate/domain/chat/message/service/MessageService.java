package com.example.shiftmate.domain.chat.message.service;

import com.example.shiftmate.domain.chat.chat.entity.Chat;
import com.example.shiftmate.domain.chat.chat.repository.ChatRepository;
import com.example.shiftmate.domain.chat.message.dto.request.MessageReqDto;
import com.example.shiftmate.domain.chat.message.dto.response.MessageResDto;
import com.example.shiftmate.domain.chat.message.entity.Message;
import com.example.shiftmate.domain.chat.message.repository.MessageRepository;
import com.example.shiftmate.domain.chat.participant.repository.ParticipantRepository;
import com.example.shiftmate.domain.storeMember.entity.StoreMember;
import com.example.shiftmate.domain.storeMember.repository.StoreMemberRepository;
import com.example.shiftmate.global.exception.CustomException;
import com.example.shiftmate.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final StoreMemberRepository storeMemberRepository;
    private final ParticipantRepository participantRepository;

    @Transactional
    public MessageResDto saveMessage(Long roomId, MessageReqDto reqDto, Long userId) {
        if (reqDto.getContent() == null || reqDto.getContent().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Chat chat = findChat(roomId);

        StoreMember sender = storeMemberRepository.findByStoreIdAndUserId(chat.getStore().getId(), userId)
            .orElseThrow(() -> new CustomException(ErrorCode.STORE_MEMBER_ACCESS_DENIED));

        validateParticipantAccess(chat.getId(), sender.getId());

        Message message = Message.of(reqDto.getContent(), chat, sender);
        return MessageResDto.from(messageRepository.save(message));
    }

    public java.util.List<MessageResDto> getMessages(Long storeId, Long chatId, Long userId) {
        Chat chat = chatRepository.findByIdAndStoreId(chatId, storeId)
            .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));

        StoreMember requester = storeMemberRepository.findByStoreIdAndUserId(storeId, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.STORE_MEMBER_ACCESS_DENIED));

        validateParticipantAccess(chat.getId(), requester.getId());

        return messageRepository.findMessagesByChatId(chatId).stream()
            .map(MessageResDto::from)
            .toList();
    }

    private Chat findChat(Long roomId) {
        return chatRepository.findById(roomId)
            .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
    }

    private void validateParticipantAccess(Long chatId, Long storeMemberId) {
        if (!participantRepository.existsByChatIdAndStoreMemberId(chatId, storeMemberId)) {
            throw new CustomException(ErrorCode.CHAT_PARTICIPANT_ACCESS_DENIED);
        }
    }
}
