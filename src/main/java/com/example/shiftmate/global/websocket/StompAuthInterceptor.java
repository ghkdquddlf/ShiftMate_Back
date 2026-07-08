package com.example.shiftmate.global.websocket;

import com.example.shiftmate.domain.chat.participant.repository.ParticipantRepository;
import com.example.shiftmate.global.exception.CustomException;
import com.example.shiftmate.global.exception.ErrorCode;
import com.example.shiftmate.global.security.CustomUserDetails;
import com.example.shiftmate.global.security.CustomUserDetailsService;
import com.example.shiftmate.global.security.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String CHAT_SUBSCRIBE_PREFIX = "/subscribe/chat/";

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;
    private final ParticipantRepository participantRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            message, StompHeaderAccessor.class
        );

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String bearerToken = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
            String token = resolveToken(bearerToken);

            Claims claims = jwtProvider.parseClaims(token);
            String category = claims.get("category", String.class);
            if (!TOKEN_TYPE_ACCESS.equals(category)) {
                throw new CustomException(ErrorCode.UNSUPPORTED_TOKEN);
            }

            String email = claims.get("email", String.class);
            if (email == null || email.isBlank()) {
                throw new CustomException(ErrorCode.MALFORMED_TOKEN);
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );

            accessor.setUser(authentication);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor);
        }

        return message;
    }

    private String resolveToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith(BEARER_PREFIX)) {
            throw new CustomException(ErrorCode.EMPTY_TOKEN);
        }
        return bearerToken.substring(BEARER_PREFIX.length()).trim();
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(CHAT_SUBSCRIBE_PREFIX)) {
            return;
        }

        Authentication authentication = extractAuthentication(accessor.getUser());
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long roomId = extractRoomId(destination);

        if (!participantRepository.existsByChatIdAndStoreMemberUserId(roomId, userDetails.getId())) {
            throw new CustomException(ErrorCode.CHAT_PARTICIPANT_ACCESS_DENIED);
        }
    }

    private Authentication extractAuthentication(java.security.Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            throw new CustomException(ErrorCode.NOT_AUTHORIZED);
        }
        return authentication;
    }

    private Long extractRoomId(String destination) {
        String roomId = destination.substring(CHAT_SUBSCRIBE_PREFIX.length());
        try {
            return Long.parseLong(roomId);
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

}
