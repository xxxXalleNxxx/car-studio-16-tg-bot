package ru.arapov.firstnormaltelegrambot;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.arapov.firstnormaltelegrambot.factories.KeyboardFactory;
import ru.arapov.firstnormaltelegrambot.models.CartItem;
import ru.arapov.firstnormaltelegrambot.models.Category;
import ru.arapov.firstnormaltelegrambot.models.Item;
import ru.arapov.firstnormaltelegrambot.repositories.CategoryRepository;
import ru.arapov.firstnormaltelegrambot.repositories.ItemRepository;
import ru.arapov.firstnormaltelegrambot.services.CartService;
import ru.arapov.firstnormaltelegrambot.services.RegistryService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.arapov.firstnormaltelegrambot.factories.KeyboardFactory.getMainMenuKeyboard;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {

    private final String botName;

    @Autowired
    private KeyboardFactory keyboardFactory;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CartService cartService;

    public TelegramBot(
            @Value("${bot.token}") String botToken,
            @Value("${bot.name}") String botName) {
        super(botToken);
        this.botName = botName;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getFrom().getId();

            switch (messageText) {

                case "/start":
                    registryService.registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getFrom().getFirstName());
                    break;

                case "/data":
                    prepareAndSendMessage(chatId, registryService.getUserRepository(chatId));
                    break;

                case "/help":
                    handleHelpCommand(chatId);
                    break;

                default:
                    prepareAndSendMessage(chatId, "Такой команды нет!\nДля просмотра всех команд введите /help");
                    break;
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            Long userId = update.getCallbackQuery().getFrom().getId();

            switch (callbackData) {

                case "items_list":
                    String messageText = """
                            🛍️ *Каталог товаров*
                                    
                            Выбери товар из списка ниже:
                            ⚡️ Быстрая доставка
                            ✅ Гарантия качества
                            """;
                    executeEditMessageText(messageText, chatId, messageId, keyboardFactory.createItemsKeyboard());
                    break;
                case "cart_view":
                    showCart(userId, chatId, (int) messageId);
                    break;

                // Очистка корзины
                case "cart_clear":
                    cartService.clearCart(userId);
                    executeEditMessageText("Корзина очищена 🧹", chatId, messageId, getMainMenuKeyboard());
                    break;

                // Оформление заказа
                case "cart_order":
                    handleOrder(userId, chatId, (int) messageId);
                    break;

                // Главное меню
                case "main_menu":
                    executeEditMessageText("Главное меню:", chatId, messageId, getMainMenuKeyboard());
                    break;
                case "categories_list":
                    executeEditMessageText("Выберите категорию товаров:", chatId, messageId, keyboardFactory.createCategoryKeyboard());
                    break;
                case "category_all":
                    executeEditMessageText("Все товары:", chatId, messageId,
                            keyboardFactory.createItemsByCategoriesKeyboard(-1L));
                    break;
                case "process_payment":
                    handleProcessPayment(userId, chatId);
                    break;

                case "cancel_payment":
                    executeEditMessageText("Оплата отменена", chatId, messageId, getMainMenuKeyboard());
                    break;
                default:
                    if (callbackData.startsWith("item_detail_")) {
                        Long itemId = Long.parseLong(callbackData.split("_")[2]);
                        Item item = itemRepository.findById(itemId).orElseThrow();
                        Long categoryId = item.getCategory().getId();
                        String itemText = "Товар: " + item.getName() + "\nЦена: " + item.getPrice() + "₽";

                        executeEditMessageText(itemText, chatId, messageId, keyboardFactory.createItemDetailKeyboard(itemId, categoryId));
                    } else if (callbackData.startsWith("back_to_category_")) {
                        Long categoryId = Long.parseLong(callbackData.split("_")[3]);
                        Category category = categoryRepository.findById(categoryId).orElseThrow();

                        executeEditMessageText("Категория: " + category.getName(),
                                chatId, messageId, keyboardFactory.createItemsByCategoriesKeyboard((categoryId)));
                    } else if (callbackData.startsWith("cart_add_")) {
                        Long itemId = Long.parseLong(callbackData.split("_")[2]);
                        cartService.addItem(userId, itemId, 1);
                        prepareAndSendMessage(chatId, "✅ Товар добавлен в корзину!");
                    } else if (callbackData.startsWith("cart_remove_")) {
                        Long itemId = Long.parseLong(callbackData.split("_")[2]);
                        boolean removed = cartService.removeItemFromCart(userId, itemId);

                        if (removed) {
                            showCart(userId, chatId, (int) messageId);
                        } else {
                            prepareAndSendMessage(chatId, "❌ Товар не найден в корзине");
                        }
                    } else if (callbackData.startsWith("category_")) {
                        Long categoryId = Long.parseLong(callbackData.split("_")[1]);
                        Category category = categoryRepository.findById(categoryId).orElseThrow();
                        executeEditMessageText("Категория:" + category.getName(), chatId, messageId, keyboardFactory.createItemsByCategoriesKeyboard(categoryId));
                    }
                    break;
            }
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Привет, " + name + "!" + "\uD83D\uDC4B" + "\n" + "Если возникли вопросы\uD83D\uDC49 /help");

        sendMessage(chatId, answer);
    }

    // метод специально для начального меню, в котором установлено изначально несколько кнопок при старте
    private void sendMessage(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        message.setReplyMarkup(getMainMenuKeyboard());
        executeMessage(message);
    }

    // метод для обработки кнопок, путем изменения сообщения. Иначе будет text = null, что хуевенько
    private void executeEditMessageText(String text, long chatId, long messageId, InlineKeyboardMarkup keyboard) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    // метод execute сразу обернут в обработчик
    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred", e.getMessage());
        }
    }

    // подготовка обычного сообщения с текстом
    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(textToSend)
                .build();
        executeMessage(message);
    }

    //TODO ПОКА ЭТО ЗДЕСЬ, НО НАДО ВЫНЕСТИ В ОТДЕЛЬНЫЙ СЕРВИС!!!

    private void showCart(Long userId, Long chatId, Integer messageId) {
        int itemsCount = cartService.getItemsCount(userId);
        BigDecimal total = cartService.calculateTotal(userId);

        if (itemsCount > 0) {
            String cartText = "🛒 *Твоя корзина*\n\n" +
                    "Товаров: " + itemsCount + "\n" +
                    "Общая сумма: " + total + "₽";
            executeEditMessageText(cartText, chatId, messageId, keyboardFactory.createCartKeyboard(userId));
        } else {
            executeEditMessageText("Корзина пуста 🫙", chatId, messageId, getMainMenuKeyboard());
        }
    }

    private void handleOrder(Long userId, Long chatId, Integer messageId) {
        int itemsCount = cartService.getItemsCount(userId);

        if (itemsCount > 0) {
            BigDecimal total = cartService.calculateTotal(userId);

            // Создаем клавиатуру с кнопкой оплаты
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton payButton = new InlineKeyboardButton();
            payButton.setText("💳 Оплатить " + total + "₽");
            payButton.setCallbackData("process_payment"); // ← Вот этот callback!

            InlineKeyboardButton cancelButton = new InlineKeyboardButton();
            cancelButton.setText("❌ Отменить");
            cancelButton.setCallbackData("cancel_payment"); // ← И этот!

            rows.add(List.of(payButton));
            rows.add(List.of(cancelButton));

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            keyboard.setKeyboard(rows);

            executeEditMessageText(
                    "✅ Заказ оформлен!\nСумма к оплате: " + total + "₽",
                    chatId,
                    messageId,
                    keyboard
            );

        } else {
            prepareAndSendMessage(chatId, "❌ Корзина пуста!");
        }
    }


    private void handleProcessPayment(Long userId, Long chatId) {
        try {
            // Получаем корзину пользователя
            BigDecimal total = cartService.calculateTotal(userId);

            // Создаем инвойс
            SendInvoice invoice = createInvoiceFromCart(chatId, userId);

            // Отправляем инвойс
            execute(invoice);

        } catch (Exception e) {
            log.error(e.getMessage());
            prepareAndSendMessage(chatId, "❌ Ошибка при создании счета оплаты");
        }
    }

    private SendInvoice createInvoiceFromCart(Long chatId, Long userId) {
        List<CartItem> cartItems = cartService.getCartItems(userId);

        List<LabeledPrice> prices = cartItems.stream()
                .map(cartItem -> new LabeledPrice(
                        cartItem.getItem().getName() + " x" + cartItem.getQuantity(), // ← quantity в названии
                        cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())) // ← умножаем цену на quantity!
                                .multiply(BigDecimal.valueOf(100)).intValue() // ← переводим в копейки
                ))
                .collect(Collectors.toList());

        // Проверяем что считает правильно
        System.out.println("🟢 Cart items: " + cartItems.size());
        for (CartItem item : cartItems) {
            System.out.println("🟢 " + item.getItem().getName() + " x" + item.getQuantity() +
                    " = " + item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())) + "₽");
        }

        SendInvoice invoice = new SendInvoice();
        invoice.setChatId(String.valueOf(chatId));
        invoice.setTitle("💳 Оплата корзины");
        invoice.setDescription("Оплата товаров из вашей корзины");
        invoice.setPayload("cart_" + System.currentTimeMillis() + "_" + userId);
        invoice.setProviderToken("1744374395:TEST:f69ed41d386338b7d598");
        invoice.setCurrency("RUB");
        invoice.setPrices(prices);

        invoice.setNeedPhoneNumber(false);
        invoice.setNeedEmail(false);
        invoice.setNeedShippingAddress(false);
        invoice.setIsFlexible(false);

        return invoice;
    }

    private void handleHelpCommand(Long chatId) {
        String helpText = """
                🚗 *CarStudio16 - Автоаксессуары*
                        
                *⚡️ Быстрое оформление заказа:*
                1. Выбери товары в каталоге
                2. Добавь в корзину
                3. Оплати безопасно через Telegram
                        
                *📦 Доставка по России - 2-5 дней*
                *✅ Гарантия 1 год на все товары*
                        
                *💬 Нужна помощь?*
                Пиши нам: @carStudioSupport
                        
                Для начала работы нажмите /start
                """;

        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(helpText)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            log.error("help command error", e.getMessage());
        }
    }
}
