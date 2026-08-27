package com.marketHub.marketplace.services;

import com.marketHub.marketplace.models.Order;
import com.marketHub.marketplace.models.OrderItem;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.OrderStatus;
import com.marketHub.marketplace.repositories.OrderItemRepository;
import com.marketHub.marketplace.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;

    public List<Order> getMyPurchases(Principal principal) {
        User buyer = userService.getUserByPrincipal(principal);
        return orderRepository.findByBuyerId(buyer.getId());
    }

    public List<OrderItem> getMySales(Principal principal) {
        User seller = userService.getUserByPrincipal(principal);
        return orderItemRepository.findByProduct_User_Id(seller.getId());
    }

    public void updateStatus(Long orderItemId, OrderStatus status, Principal principal) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId).orElse(null);
        if (orderItem == null) return;

        User currentUser = userService.getUserByPrincipal(principal);

        // проверка: владелец товара в этой строке заказа = principal
        if (!orderItem.getProduct().getUser().getId().equals(currentUser.getId())) {
            return;
        }

        // запрет на изменение на предыдущий/тот же статус
        boolean canMoveTo;
        switch (orderItem.getStatus()) {
            case NEW:
                canMoveTo = status != OrderStatus.NEW;
                break;
            case CONFIRMED:
                canMoveTo = status != OrderStatus.CONFIRMED && status != OrderStatus.NEW;
                break;
            case SENT:
                canMoveTo = status == OrderStatus.RECEIVED || status == OrderStatus.CANCELLED;
                break;
            case RECEIVED:
                canMoveTo = status == OrderStatus.CANCELLED;
                break;
            default:
                // CANCELLED — терминальный статус, дальше меняться не может
                canMoveTo = false;
        }

        if (!canMoveTo) return;

        orderItem.setStatus(status);
        orderItemRepository.save(orderItem);
    }
}
