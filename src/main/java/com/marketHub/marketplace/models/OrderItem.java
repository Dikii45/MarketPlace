package com.marketHub.marketplace.models;

import com.marketHub.marketplace.models.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Product product;  // на какой товар ссылается (не копия!)

    @ManyToOne
    private Order order;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.NEW;

    private int quantity;

}
