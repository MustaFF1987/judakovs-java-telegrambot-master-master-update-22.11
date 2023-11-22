package ebe.P_Judakov.s.JAVABOT;
import ebe.P_Judakov.s.JAVABOT.service.jpa.TelegramBotService;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.junit.jupiter.api.Assertions.*;

public class TestSendTextMessageWithKeyboard {
    @Test
    public void testSendTextMessageWithKeyboard() {
        long chatId = 12345L;
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true); // Установите параметры клавиатуры по вашему выбору

        // Предположим, у вас есть какое-то ожидаемое значение
        String expectedValue = "Ожидаемое значение";

        TelegramBotService.StockQuoteBot bot = new TelegramBotService.StockQuoteBot();

        try {
            // Отправляем сообщение с пустой клавиатурой
            bot.sendTextMessageWithKeyboard(chatId, "Test message", null);

            // Проверяем, что сообщение не было отправлено из-за пустой клавиатуры
            assertNull(bot.getLastMessageSent());
        } catch (TelegramApiException e) {
            // Если возникла ошибка, сообщение не было отправлено
            assertNull(bot.getLastMessageSent());
        }

        try {
            bot.sendTextMessageWithKeyboard(chatId, "Введите тикер акции:", keyboardMarkup);

            // Получаем фактическое значение после отправки сообщения с клавиатурой
            String actualValue = bot.getUserInputTicker();

            // Проверка, что сообщение было отправлено
            assertNotNull(bot.getLastMessageSent()); // Проверка на то, что сообщение было отправлено
            assertEquals("Введите тикер акции:", bot.getLastMessageSent()); // Проверка текста сообщения

            // Дополнительные проверки соответствующие вашим требованиям
            assertEquals(expectedValue, actualValue); // Пример дополнительной проверки
        } catch (Exception e) {
            // Обработка исключения
            e.printStackTrace();
        }
    }
}



