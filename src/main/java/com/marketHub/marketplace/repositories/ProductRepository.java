package com.marketHub.marketplace.repositories;

import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.enums.Category;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // каталог с необязательными фильтрами (title/category/цена) и сортировкой —
    // один гибкий запрос вместо кучи derived-методов на каждую комбинацию фильтров
    @Query("""
            select p from Product p
            where p.deleted = false
              and p.quantity > 0
              and (:title is null or lower(p.title) like lower(concat('%', :title, '%')))
              and (:category is null or p.category = :category)
              and (:minPrice is null or p.price >= :minPrice)
              and (:maxPrice is null or p.price <= :maxPrice)
            """)
    List<Product> search(@Param("title") String title,
                          @Param("category") Category category,
                          @Param("minPrice") Integer minPrice,
                          @Param("maxPrice") Integer maxPrice,
                          Sort sort);
}
