package ru.arapov.firstnormaltelegrambot.factories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.arapov.firstnormaltelegrambot.models.CartItem;
import ru.arapov.firstnormaltelegrambot.models.Category;
import ru.arapov.firstnormaltelegrambot.models.Item;
import ru.arapov.firstnormaltelegrambot.repositories.CategoryRepository;
import ru.arapov.firstnormaltelegrambot.repositories.ItemRepository;
import ru.arapov.firstnormaltelegrambot.services.CartService;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeyboardFactory {

    private final ItemRepository itemRepository;

    private final CategoryRepository categoryRepository;
    private final CartService cartService;

    @Autowired
    public KeyboardFactory(ItemRepository itemRepository, CategoryRepository categoryRepository, CartService cartService) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.cartService = cartService;
    }


    public static InlineKeyboardMarkup getMainMenuKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton categoriesButton = new InlineKeyboardButton();
        categoriesButton.setText("🗂️ Категории товаров");
        categoriesButton.setCallbackData("categories_list");

        InlineKeyboardButton cartButton = new InlineKeyboardButton();
        cartButton.setText("🛒 Корзина");
        cartButton.setCallbackData("cart_view");

        rows.add(List.of(categoriesButton));
        rows.add(List.of(cartButton));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup createItemsKeyboard() {
        List<Item> items = itemRepository.findByOrderByNameAsc();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Item item : items) {
            InlineKeyboardButton itemButton = new InlineKeyboardButton();
            itemButton.setText(item.getName() + " - " + item.getPrice() + " рублей");
            itemButton.setCallbackData("item_detail_" + item.getId());

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(itemButton);
            rows.add(row);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        InlineKeyboardButton backToMainButton = new InlineKeyboardButton();

        backToMainButton.setText("️◀️ В главное меню");
        backToMainButton.setCallbackData("main_menu");
        rows.add(List.of(backToMainButton));

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup createCartKeyboard(Long userId) {
        List<CartItem> cartItems = cartService.getCartItems(userId);
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Товары в корзине (каждый товар - отдельная кнопка для удаления)
        for (CartItem cartItem : cartItems) {
            InlineKeyboardButton itemButton = new InlineKeyboardButton();
            itemButton.setText("🗑️ " + cartItem.getItem().getName() + " x" + cartItem.getQuantity());
            itemButton.setCallbackData("cart_remove_" + cartItem.getItem().getId());
            rows.add(List.of(itemButton));
        }

        // Кнопка очистки корзины
        InlineKeyboardButton clearButton = new InlineKeyboardButton();
        clearButton.setText("🧹 Очистить корзину");
        clearButton.setCallbackData("cart_clear");

        // Кнопка оформления заказа
        InlineKeyboardButton orderButton = new InlineKeyboardButton();
        orderButton.setText("🚀 Оформить заказ");
        orderButton.setCallbackData("cart_order");

        // Кнопка назад в меню
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ В главное меню");
        backButton.setCallbackData("main_menu");

        // Добавляем кнопки управления
        rows.add(List.of(clearButton));
        rows.add(List.of(orderButton));
        rows.add(List.of(backButton));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup createCategoryKeyboard() {

        List<Category> categories = categoryRepository.findAll();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Category category : categories) {
            InlineKeyboardButton categoryButton = new InlineKeyboardButton();
            categoryButton.setText(category.getName());
            categoryButton.setCallbackData("category_" + category.getId());
            rows.add(List.of(categoryButton));
        }

        InlineKeyboardButton mainButton = new InlineKeyboardButton();
        mainButton.setText("◀️ Главное меню");
        mainButton.setCallbackData("main_menu");
        rows.add(List.of(mainButton));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup createItemDetailKeyboard(Long itemId, Long categoryId) {

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton addToCartButton = new InlineKeyboardButton();
        addToCartButton.setText("🛒 Добавить в корзину");
        addToCartButton.setCallbackData("cart_add_" + itemId);

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("back_to_category_" + categoryId);
        rows.add(List.of(backButton));

        rows.add(List.of(addToCartButton));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup createItemsByCategoriesKeyboard(Long categoryId) {

        List<Item> items;

        if (categoryId == -1L) {
            items = itemRepository.findAll();
        } else {
            items = itemRepository.findByCategoryId(categoryId);
        }

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Item item : items) {
            InlineKeyboardButton itemButton = new InlineKeyboardButton();
            itemButton.setText(item.getName() + " - " + item.getPrice() + "₽");
            itemButton.setCallbackData("item_detail_" + item.getId());
            rows.add(List.of(itemButton));
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ К категориям");
        backButton.setCallbackData("categories_list");
        rows.add(List.of(backButton));

        return new InlineKeyboardMarkup(rows);
    }


}
