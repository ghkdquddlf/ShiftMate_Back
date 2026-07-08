package com.example.shiftmate.domain.chat.chat.dto.request;

import com.example.shiftmate.domain.chat.chat.entity.ChatType;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class CreateChatReqDto {

    private ChatType chatType;
    private List<Long> participantIds;
    private String roomName;
}
