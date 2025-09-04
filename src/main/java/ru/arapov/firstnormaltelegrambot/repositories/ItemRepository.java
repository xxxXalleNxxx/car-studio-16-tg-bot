package ru.arapov.firstnormaltelegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.arapov.firstnormaltelegrambot.models.Category;
import ru.arapov.firstnormaltelegrambot.models.Item;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByOrderByNameAsc();

    List<Item> findByCategoryId(Long categoryId);

    List<Item> findByCategoryName(String categoryName);
}
