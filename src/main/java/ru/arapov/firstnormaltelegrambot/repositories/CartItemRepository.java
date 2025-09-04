package ru.arapov.firstnormaltelegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.arapov.firstnormaltelegrambot.models.CartItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartUserId(Long userId);

    Optional<CartItem> findByCartUserIdAndItemId(Long userId, Long itemId);


}
