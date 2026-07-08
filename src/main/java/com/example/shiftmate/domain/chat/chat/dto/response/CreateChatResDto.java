package com.example.shiftmate.domain.chat.chat.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class CreateChatResDto {
    private final Long chatId;
}
