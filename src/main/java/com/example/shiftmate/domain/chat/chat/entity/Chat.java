package com.example.shiftmate.domain.chat.chat.entity;

import com.example.shiftmate.domain.chat.message.entity.Message;
import com.example.shiftmate.domain.chat.participant.entity.Participant;
import com.example.shiftmate.domain.store.entity.Store;
import com.example.shiftmate.global.common.entity.BaseCreateEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PRIVATE)
public class Chat extends BaseCreateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Builder.Default
    @OneToMany(mappedBy = "chat")
    private List<Participant> participants = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "chat")
    private List<Message> messages = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private ChatType chatType;

    private String roomName;


    public static Chat of(Store store, ChatType chatType, String roomName) {
        return Chat.builder()
            .store(store)
            .chatType(chatType)
            .roomName(roomName)
            .build();
    }
}
