package com.example.shiftmate.global.config;

import com.example.shiftmate.global.websocket.StompAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


// websocket broker 설정
@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {


    private final StompAuthInterceptor stompAuthInterceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/subscribe"); // subscribe prefix : 즉 클라이언트가 구독을 지정할 주소
        // enableSimpleBroker -> 스프링이 제공하는 인메모리 방식의 브로커 사용
        registry.setApplicationDestinationPrefixes("/publish");// publish prefix : 즉 클라이언트가 메시지를 보낼(발행) 주소

    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp") // 웹소켓 연결 주소 , 오프닝 헨드셰이크 과정에서 사용할 endpoint
            .setAllowedOriginPatterns("*"); // CORS 주소 설정 -> 모두 허용
    }



}
