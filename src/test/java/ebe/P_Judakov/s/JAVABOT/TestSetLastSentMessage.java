package ebe.P_Judakov.s.JAVABOT;
import ebe.P_Judakov.s.JAVABOT.domen.entity.jpa.JpaChat;
import ebe.P_Judakov.s.JAVABOT.domen.entity.jpa.JpaMessage;
import ebe.P_Judakov.s.JAVABOT.domen.entity.jpa.JpaUser;
import ebe.P_Judakov.s.JAVABOT.service.jpa.TelegramBotService;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class TestSetLastSentMessage {
    @Test
    public void testSetLastSentMessage() {
        TelegramBotService.StockQuoteBot bot = new TelegramBotService.StockQuoteBot();
        //JpaMessage message = new JpaMessage(1, 1001, "Test message", new Date(), new JpaUser(), new JpaChat());

        // Создаем фиктивное сообщение и устанавливаем его как последнее отправленное
        JpaMessage message = new JpaMessage(1, 1001, "Test message", new Date(), new JpaUser(), new JpaChat());
        bot.setLastSentMessage(message);

        // Проверяем, что последнее отправленное сообщение соответствует ожидаемому
        String lastMessage = bot.getLastMessageSent();
        assertEquals("Test message", lastMessage);
    }
    @Test
    public void testGetLastMessageSent() {
        TelegramBotService.StockQuoteBot bot = new TelegramBotService.StockQuoteBot();

        // Проверяем, что getLastMessageSent() возвращает null при инициализации
        assertNull(bot.getLastMessageSent());

        // После отправки сообщения без клавиатуры, ожидаем, что getLastMessageSent() вернет текст сообщения
        try {
            bot.sendTextMessageWithKeyboard(12345L, "Test message", null);
            assertEquals("Test message", bot.getLastMessageSent());
        } catch (TelegramApiException e) {
            // Обработка исключения, если она нужна
            e.printStackTrace();
        }
    }


}

