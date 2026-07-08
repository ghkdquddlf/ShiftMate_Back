package com.example.shiftmate.domain.chat.participant.repository;

import com.example.shiftmate.domain.chat.participant.entity.Participant;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    boolean existsByChatIdAndStoreMemberId(Long chatId, Long storeMemberId);

    boolean existsByChatIdAndStoreMemberUserId(Long chatId, Long userId);

    @EntityGraph(attributePaths = {
        "chat",
        "chat.participants",
        "chat.participants.storeMember",
        "chat.participants.storeMember.user"
    })
    @Query("SELECT DISTINCT p FROM Participant p WHERE p.storeMember.id = :storeMemberId")
    List<Participant> findByStoreMemberIdWithChat(@Param("storeMemberId") Long storeMemberId);
}
