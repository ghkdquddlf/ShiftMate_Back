package com.example.shiftmate.domain.chat.message.entity;

import com.example.shiftmate.domain.chat.chat.entity.Chat;
import com.example.shiftmate.domain.storeMember.entity.StoreMember;
import com.example.shiftmate.global.common.entity.BaseCreateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Message extends BaseCreateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;//메시지 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat; // 메시지가 해당하는 채팅방 아이디

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private StoreMember sender; // 메시지를 입력한 가게 사용자 , user xx, storeMember oo

    public static Message of(String content, Chat chat, StoreMember sender) {
        return Message.builder()
            .content(content)
            .chat(chat)
            .sender(sender)
            .build();
    }

}
