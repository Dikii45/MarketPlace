package com.marketHub.marketplace.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User sender;

    @ManyToOne
    private User recipient;

    @Column(columnDefinition = "text")
    private String text;

    private LocalDateTime sentAt;

    @Column(name = "is_read")
    private boolean read = false;

    @PrePersist
    private void init() {
        sentAt = LocalDateTime.now();
    }
}
