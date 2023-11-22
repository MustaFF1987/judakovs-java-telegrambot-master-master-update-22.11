package ebe.P_Judakov.s.JAVABOT;

import ebe.P_Judakov.s.JAVABOT.service.jpa.TelegramBotService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGetUserInputTicker {

    @Test
    public void testGetUserInputTicker() {
        // Создание экземпляра вашего класса StockQuoteBot
        TelegramBotService.StockQuoteBot bot = new TelegramBotService.StockQuoteBot();

        // Вызов метода getUserInputTicker()
        String userInputTicker = bot.getUserInputTicker();

        // Проверка, что метод возвращает ожидаемое значение тикера
        assertEquals("AAPL", userInputTicker); // Подставьте ожидаемое значение тикера
    }
}

