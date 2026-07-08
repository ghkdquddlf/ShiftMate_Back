package com.example.shiftmate.domain.chat.chat.dto.response;

import com.example.shiftmate.domain.storeMember.entity.StoreMember;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatParticipantSummaryResDto {

    private final Long storeMemberId;
    private final Long userId;
    private final String userName;

    public static ChatParticipantSummaryResDto from(StoreMember storeMember) {
        return ChatParticipantSummaryResDto.builder()
            .storeMemberId(storeMember.getId())
            .userId(storeMember.getUser().getId())
            .userName(storeMember.getUser().getName())
            .build();
    }
}
