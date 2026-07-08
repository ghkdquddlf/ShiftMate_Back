package com.example.shiftmate.domain.chat.message.dto.response;


import com.example.shiftmate.domain.chat.message.entity.Message;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
public class MessageResDto {

    private final Long messageId;
    private final String content;
    private final Long sendingId;
    private final String senderName;
    private final Long chatRoomId;
    private final LocalDateTime createTime;

    public static MessageResDto from(Message saveMessage) {
        return MessageResDto.builder()
                   .messageId(saveMessage.getId())
                   .content(saveMessage.getContent())
                   .sendingId(saveMessage.getSender().getId())
                   .senderName(saveMessage.getSender().getUser().getName())
                   .chatRoomId(saveMessage.getChat().getId())
                   .createTime(saveMessage.getCreatedAt())
                   .build();
    }
}
