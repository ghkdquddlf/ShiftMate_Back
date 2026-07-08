package com.example.shiftmate.domain.chat.chat.controller;

import com.example.shiftmate.domain.chat.chat.dto.request.CreateChatReqDto;
import com.example.shiftmate.domain.chat.chat.dto.response.ChatRoomResDto;
import com.example.shiftmate.domain.chat.chat.dto.response.CreateChatResDto;
import com.example.shiftmate.domain.chat.chat.service.ChatService;
import com.example.shiftmate.global.common.dto.ApiResponse;
import com.example.shiftmate.global.security.CustomUserDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stores/{storeId}/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateChatResDto>> createChatroom (
        @PathVariable Long storeId,
        @RequestBody CreateChatReqDto reqDto,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return ResponseEntity.ok(ApiResponse.success(
            chatService.createChatroom(storeId, reqDto , userDetails.getId())
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomResDto>>> getChatRooms(
        @PathVariable Long storeId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            chatService.getChatRooms(storeId, userDetails.getId())
        ));
    }
}
