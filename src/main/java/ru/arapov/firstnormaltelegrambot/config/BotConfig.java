package ru.arapov.firstnormaltelegrambot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.arapov.firstnormaltelegrambot.TelegramBot;
import ru.arapov.firstnormaltelegrambot.factories.KeyboardFactory;
import ru.arapov.firstnormaltelegrambot.repositories.ItemRepository;

import java.util.List;

@Slf4j
@Configuration
public class BotConfig {

    @Autowired
    public TelegramBotsApi telegramBotsApi(TelegramBot telegramBot) throws TelegramApiException {
        TelegramBotsApi botApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botApi.registerBot(telegramBot);
        } catch (TelegramApiException e) {
            log.error("Error occurred", e.getMessage());
        }
        return botApi;
    }
}
