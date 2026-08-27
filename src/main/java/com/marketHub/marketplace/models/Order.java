package com.marketHub.marketplace.models;

import com.marketHub.marketplace.models.enums.OrderStatus;
import com.marketHub.marketplace.models.enums.PaymentMethod;
import com.marketHub.marketplace.models.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User buyer;

    private LocalDateTime dateOfCreated;
    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @OneToMany(mappedBy = "order")
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    private  void init(){
        dateOfCreated = LocalDateTime.now();
    }

}
