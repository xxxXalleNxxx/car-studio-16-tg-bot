package ru.arapov.firstnormaltelegrambot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.arapov.firstnormaltelegrambot.models.User;
import ru.arapov.firstnormaltelegrambot.repositories.UserRepository;

import java.sql.Timestamp;

@Service
@Slf4j
public class RegistryService {

    @Autowired
    private UserRepository userRepository;

    private static final String ERROR_TEXT = "user is not found!";

    public void registerUser(Message msg) {

        if (userRepository.findUserByChatId(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUsername(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved" + user);
        }
    }

    // спорный момент с проверкой на существующего пользователя, потому что если нет, то куда он этот error text пошлет, хз!
    public String getUserRepository(long chatId) {
        if (userRepository.findUserByChatId(chatId).isPresent()) {
            return userRepository.findUserByChatId(chatId).get().toString();

        }
        else {
            return ERROR_TEXT;
        }
    }
}
