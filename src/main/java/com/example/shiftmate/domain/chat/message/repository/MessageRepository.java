package com.example.shiftmate.domain.chat.message.repository;

import com.example.shiftmate.domain.chat.message.entity.Message;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message,Long> {

    @EntityGraph(attributePaths = {"chat", "sender", "sender.user"})
    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.createdAt ASC, m.id ASC")
    List<Message> findMessagesByChatId(@Param("chatId") Long chatId);

    @EntityGraph(attributePaths = {"chat", "sender", "sender.user"})
    @Query("""
        SELECT m
        FROM Message m
        WHERE m.chat.id IN :chatIds
          AND m.id IN (
              SELECT MAX(m2.id)
              FROM Message m2
              WHERE m2.chat.id IN :chatIds
              GROUP BY m2.chat.id
          )
        """)
    List<Message> findLatestMessagesByChatIds(@Param("chatIds") List<Long> chatIds);
}
