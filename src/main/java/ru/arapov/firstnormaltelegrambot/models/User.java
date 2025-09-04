package ru.arapov.firstnormaltelegrambot.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.sql.Timestamp;

@Entity
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "tg_users")
public class User {

    @Id
    Long chatId;

    String firstName;

    String lastName;

    String username;

    Timestamp registeredAt;

    @Override
    public String toString() {
        return "Your data \n{" + "\n" +
                "chatId=" + chatId + ",\n" +
                "firstName='" + firstName + '\'' + ",\n" +
                "lastName='" + lastName + '\'' + ",\n" +
                "username='" + username + '\'' + ",\n" +
                "registeredAt=" + registeredAt + "\n" +
                '}';
    }
}
