package com.example.shiftmate.domain.chat.message.controller;

import com.example.shiftmate.domain.chat.message.dto.request.MessageReqDto;
import com.example.shiftmate.domain.chat.message.dto.response.MessageResDto;
import com.example.shiftmate.domain.chat.message.service.MessageService;
import com.example.shiftmate.global.security.CustomUserDetails;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;

    @MessageMapping("/chat/{roomId}")
    @SendTo("/subscribe/chat/{roomId}")
    public MessageResDto sendMessage(
        @DestinationVariable Long roomId,
        @Payload MessageReqDto reqDto,
        Principal principal
    ) {
        // 발신자 즉 userDetails의 주인이 해당 채티방의 참여자인지 검증 하는로직 필요
        Authentication authentication = (Authentication) principal;
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return messageService.saveMessage(roomId, reqDto, userDetails.getId() );
    }
}
