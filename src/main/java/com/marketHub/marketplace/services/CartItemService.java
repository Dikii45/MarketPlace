package com.marketHub.marketplace.services;

import com.marketHub.marketplace.models.CartItem;
import com.marketHub.marketplace.models.Order;
import com.marketHub.marketplace.models.OrderItem;
import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.OrderStatus;
import com.marketHub.marketplace.models.enums.PaymentMethod;
import com.marketHub.marketplace.models.enums.PaymentStatus;
import com.marketHub.marketplace.repositories.CartItemRepository;
import com.marketHub.marketplace.repositories.OrderItemRepository;
import com.marketHub.marketplace.repositories.OrderRepository;
import com.marketHub.marketplace.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;


    // отдаёт корзину, попутно вычищая записи на товары, которые продавец успел удалить
    @Transactional
    public List<CartItem> getCartItems(User user) {
        List<CartItem> items = cartItemRepository.findAllByUser(user);

        List<CartItem> stale = items.stream().filter(i -> i.getProduct().isDeleted()).toList();
        if (stale.isEmpty()) {
            return items;
        }

        cartItemRepository.deleteAll(stale);
        return items.stream().filter(i -> !i.getProduct().isDeleted()).toList();
    }


    // подсчет суммы (ИТОГО)
    public int getCartTotal(User user) {

        int total = 0;
        for (CartItem cartItem : getCartItems(user)) {
            total += cartItem.getProduct().getPrice() * cartItem.getQuantity();
        }

        return total;
    }


    // добавление продукта в корзину
    public void addProductToCart(User user, Product product) {
        if (product.getQuantity() <= 0) return;

        CartItem existing = cartItemRepository.findByUserAndProduct(user, product).orElse(null);
        if (existing != null) {
            //проверка если в корзине лежит меньше чем на складе(у продовеца), тогда все ок
            if (existing.getQuantity() < product.getQuantity()) {
                existing.setQuantity(existing.getQuantity() + 1);
                cartItemRepository.save(existing);
            }
            return;
        }

        CartItem cartItem = new CartItem();
        cartItem.setUser(user);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItemRepository.save(cartItem);
    }

    public void updateQuantity(User user, Product product, int quantity) {
        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product).orElse(null);
        if (cartItem == null) return;

        int clamped = Math.max(1, Math.min(quantity, product.getQuantity()));
        cartItem.setQuantity(clamped);
        cartItemRepository.save(cartItem);
    }


    // Удаление из корзины
    @Transactional
    public void removeFromCart(User user, Product product) {
        cartItemRepository.deleteByUserAndProduct(user, product);
    }


    //оформление заказа. Возвращает false, если на складе не хватает остатка хотя бы по одной позиции —
    //в этом случае заказ вообще не создаётся (нельзя заказать МЕНЬШЕ, чем выбрал покупатель)
    @Transactional
    public boolean checkout(User user, String address, PaymentMethod paymentMethod) {
        List<CartItem> cartItems = getCartItems(user);
        if (cartItems.isEmpty()) return false;


        //либо заказываем ровно то количество, что выбрал покупатель, либо не заказываем вообще
        //(getCartItems уже вычистил позиции на удалённые продавцом товары)
        for (CartItem cartItem : cartItems) {
            if (cartItem.getQuantity() > cartItem.getProduct().getQuantity()) {
                return false;
            }
        }

        //заполняем ордер
        Order order = new Order();
        order.setBuyer(user);
        order.setDeliveryAddress(address);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);

        // на каждый CartItem созадем orderItem(для того что бы продовец и покупатель мог отслеживать)
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setStatus(OrderStatus.NEW);
            orderItem.setQuantity(quantity);
            orderItemRepository.save(orderItem);

            //списываем остаток ровно на заказанное количество
            product.setQuantity(product.getQuantity() - quantity);
            productRepository.save(product);
        }

        cartItemRepository.deleteAllByUser(user);
        return true;
    }
}
