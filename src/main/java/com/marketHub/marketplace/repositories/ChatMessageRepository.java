package com.marketHub.marketplace.repositories;

import com.marketHub.marketplace.models.ChatMessage;
import com.marketHub.marketplace.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("select m from ChatMessage m where (m.sender = :a and m.recipient = :b) or (m.sender = :b and m.recipient = :a) order by m.sentAt asc, m.id asc")
    List<ChatMessage> findConversation(@Param("a") User a, @Param("b") User b);

    @Query("select m from ChatMessage m where ((m.sender = :a and m.recipient = :b) or (m.sender = :b and m.recipient = :a)) and m.id > :afterId order by m.sentAt asc, m.id asc")
    List<ChatMessage> findConversationAfter(@Param("a") User a, @Param("b") User b, @Param("afterId") Long afterId);

    @Modifying
    @Query("update ChatMessage m set m.read = true where m.recipient = :reader and m.sender = :other and m.read = false")
    int markRead(@Param("reader") User reader, @Param("other") User other);
}
