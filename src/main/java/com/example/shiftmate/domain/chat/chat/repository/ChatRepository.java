package com.example.shiftmate.domain.chat.chat.repository;

import com.example.shiftmate.domain.chat.chat.entity.Chat;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat,Long> {

    Optional<Chat> findByIdAndStoreId(Long chatId, Long storeId);
}
