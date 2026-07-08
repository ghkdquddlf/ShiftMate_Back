package com.example.shiftmate.domain.chat.chat.dto.response;

import com.example.shiftmate.domain.chat.chat.entity.Chat;
import com.example.shiftmate.domain.chat.chat.entity.ChatType;
import com.example.shiftmate.domain.chat.message.entity.Message;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatRoomResDto {

    private final Long chatId;
    private final ChatType chatType;
    private final String roomName;
    private final String lastMessage;
    private final LocalDateTime lastMessageAt;
    private final Integer participantCount;
    private final List<ChatParticipantSummaryResDto> participants;

    public static ChatRoomResDto from(
        Chat chat,
        String resolvedRoomName,
        Message lastMessage,
        List<ChatParticipantSummaryResDto> participants
    ) {
        return ChatRoomResDto.builder()
            .chatId(chat.getId())
            .chatType(chat.getChatType())
            .roomName(resolvedRoomName)
            .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
            .lastMessageAt(lastMessage != null ? lastMessage.getCreatedAt() : null)
            .participantCount(participants.size())
            .participants(participants)
            .build();
    }
}
