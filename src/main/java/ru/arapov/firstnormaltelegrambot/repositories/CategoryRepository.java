package ru.arapov.firstnormaltelegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.arapov.firstnormaltelegrambot.models.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);
}
