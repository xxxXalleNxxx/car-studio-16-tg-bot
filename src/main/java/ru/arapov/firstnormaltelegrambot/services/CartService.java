package ru.arapov.firstnormaltelegrambot.services;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.arapov.firstnormaltelegrambot.models.Cart;
import ru.arapov.firstnormaltelegrambot.models.CartItem;
import ru.arapov.firstnormaltelegrambot.models.Item;
import ru.arapov.firstnormaltelegrambot.repositories.CartItemRepository;
import ru.arapov.firstnormaltelegrambot.repositories.CartRepository;
import ru.arapov.firstnormaltelegrambot.repositories.ItemRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return cartRepository.save(newCart);
                        });
    }

    public void addItem(Long userId, Long itemId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new RuntimeException("Item Not found"));

        Optional<CartItem> existingItem = cartItemRepository.findByCartUserIdAndItemId(userId, itemId);

        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItemRepository.save(cartItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setItem(item);
            newItem.setQuantity(quantity);
            newItem.setPrice(item.getPrice());
            cartItemRepository.save(newItem);
        }
    }

    public boolean removeItemFromCart(Long userId, Long itemId) {
        Optional<CartItem> cartItem = cartItemRepository.findByCartUserIdAndItemId(userId, itemId);
        if (cartItem.isPresent()) {
            cartItemRepository.delete(cartItem.get());
            return true;
        }
        return false;
    }

    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId)
                .ifPresent(cart -> {
                    cart.getItems().clear();
                    cartRepository.save(cart);
                });
    }

    public List<CartItem> getCartItems(Long userId) {
        return cartItemRepository.findByCartUserId(userId);
    }

    public int getItemsCount(Long userId) {
        return cartItemRepository.findByCartUserId(userId).stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public BigDecimal calculateTotal(Long userId) {
        return cartItemRepository.findByCartUserId(userId).stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
