package com.example.shiftmate.domain.chat.message.controller;

import com.example.shiftmate.domain.chat.message.dto.response.MessageResDto;
import com.example.shiftmate.domain.chat.message.service.MessageService;
import com.example.shiftmate.global.common.dto.ApiResponse;
import com.example.shiftmate.global.security.CustomUserDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stores/{storeId}/chat/{chatId}/messages")
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MessageResDto>>> getMessages(
        @PathVariable Long storeId,
        @PathVariable Long chatId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            messageService.getMessages(storeId, chatId, userDetails.getId())
        ));
    }
}
