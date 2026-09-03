package ru.creditbot;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ru.creditbot.bot.CreditTelegramBot;

public class Main {

    public static void main(String[] args) {

        String botToken =
                System.getenv("BOT_TOKEN");

        if (botToken == null ||
                botToken.isBlank()) {

            System.out.println(
                    "Ошибка: переменная BOT_TOKEN не найдена."
            );

            return;
        }

        botToken =
                botToken.trim();

        try (TelegramBotsLongPollingApplication botsApplication =
                     new TelegramBotsLongPollingApplication()) {

            botsApplication.registerBot(
                    botToken,
                    new CreditTelegramBot(botToken)
            );

            System.out.println(
                    "Кредитный бот успешно запущен!"
            );

            Thread.currentThread().join();

        } catch (Exception e) {

            System.out.println(
                    "Ошибка при запуске бота:"
            );

            e.printStackTrace();
        }
    }
}