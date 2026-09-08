package com.marketHub.marketplace.models;

import com.marketHub.marketplace.models.enums.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)

    @Column(name = "id")
    private Long id;
    @Column(name = "title")
    private String title;
    @Column(name = "description", columnDefinition = "text")
    private String description;
    @Column(name = "price")
    private int price;
    @Column(name = "city")
    private String city;
    @Enumerated(EnumType.STRING)
    private Category category;


    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "product")
    private List<Image> images = new ArrayList<>();
    private Long previewImageId;
    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    @JoinColumn
    private User user;
    private LocalDateTime dateOfCreated;
    private int quantity;
    // пометка что товар удален, т.к. физически удалить не возможно
    private boolean deleted = false;


    @PrePersist
    private  void init(){
        dateOfCreated = LocalDateTime.now();
    }

    public void  addImageToProduct(Image image){
        image.setProduct(this);
        images.add(image);
    }
}
