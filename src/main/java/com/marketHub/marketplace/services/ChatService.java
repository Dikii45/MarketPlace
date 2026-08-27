package com.marketHub.marketplace.services;

import com.marketHub.marketplace.models.ChatMessage;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.repositories.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    // после скольких секунд без активности считаем пользователя оффлайн
    private static final int ONLINE_WINDOW_SECONDS = 300;

    private final ChatMessageRepository chatMessageRepository;

    public List<ChatMessage> getConversation(User a, User b) {
        return chatMessageRepository.findConversation(a, b);
    }

    public List<ChatMessage> getConversationAfter(User a, User b, Long afterId) {
        return chatMessageRepository.findConversationAfter(a, b, afterId);
    }

    public ChatMessage sendMessage(User sender, User recipient, String text) {
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setText(text.trim());
        return chatMessageRepository.save(message);
    }

    @Transactional
    public void markRead(User reader, User other) {
        chatMessageRepository.markRead(reader, other);
    }

    public boolean isOnline(User user) {
        if (user.getLastActiveAt() == null) return false;
        return ChronoUnit.SECONDS.between(user.getLastActiveAt(), LocalDateTime.now()) < ONLINE_WINDOW_SECONDS;
    }
}
