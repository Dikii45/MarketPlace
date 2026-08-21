package com.marketHub.marketplace.models;

import com.marketHub.marketplace.models.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.NEW;
    private LocalDateTime dateOfCreated;

    @ManyToOne
    private Product product;
    @ManyToOne
    private User buyer;

    @PrePersist
    private  void init(){
        dateOfCreated = LocalDateTime.now();
    }

}
