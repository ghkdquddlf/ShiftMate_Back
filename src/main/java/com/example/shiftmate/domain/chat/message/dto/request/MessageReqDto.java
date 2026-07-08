package com.example.shiftmate.domain.chat.message.dto.request;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MessageReqDto {

    private Long roomId;
    private String content;

}
