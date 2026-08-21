package com.marketHub.marketplace.services;

import com.marketHub.marketplace.models.Order;
import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.OrderStatus;
import com.marketHub.marketplace.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;


import org.springframework.stereotype.Service;
import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserService userService;


    public void saveOrder(User buyer, Product product) {
        Order order = new Order();
        order.setBuyer(buyer);
        order.setProduct(product);

        orderRepository.save(order);
    }

    public List<Order> getMyPurchases(Principal principal) {
        User buyer = userService.getUserByPrincipal(principal);
        return orderRepository.findByBuyerId(buyer.getId());
    }

    public List<Order> getMySales(Principal principal) {
        User seller = userService.getUserByPrincipal(principal);
        return orderRepository.findByProduct_User_Id(seller.getId());
    }

    public void updateStatus(Long orderId, OrderStatus status, Principal principal) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return;

        User currentUser = userService.getUserByPrincipal(principal);
        if (!order.getProduct().getUser().getId().equals(currentUser.getId())) {
            return;
        }

        order.setStatus(status);
        orderRepository.save(order);
    }

}
