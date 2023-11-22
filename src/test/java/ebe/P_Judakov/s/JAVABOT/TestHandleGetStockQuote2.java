package ebe.P_Judakov.s.JAVABOT;
import ebe.P_Judakov.s.JAVABOT.service.jpa.TelegramBotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
public class TestHandleGetStockQuote2 {
    public class StockQuoteBotTest2 {
        private TelegramBotService.StockQuoteBot bot;
        private long chatId;
        private ReplyKeyboardMarkup keyboardMarkup;
        private Map<Long, String> userState;

        @BeforeEach
        public void setUp() {
            bot = new TelegramBotService.StockQuoteBot(); // Подставьте ваш класс
            chatId = 123456789L;
            keyboardMarkup = new ReplyKeyboardMarkup();
            userState = new HashMap<>();
        }

        @Test
        public void testHandleGetStockQuote() {
            // Предположим, что ваши методы возвращают строки для тестирования
            // Замените на вашу логику и проверки

            // Шаг 1: Первый вызов метода handleGetStockQuote
            bot.handleGetStockQuote(chatId, "AAPL", keyboardMarkup, userState);
            assertTrue(userState.containsKey(chatId));
            assertEquals("AWAITING_STOCK_TICKER", userState.get(chatId));

            // Шаг 2: Повторный вызов метода handleGetStockQuote, когда состояние уже установлено
            bot.handleGetStockQuote(chatId, "AAPL", keyboardMarkup, userState);
            assertEquals("AWAITING_STOCK_TICKER", userState.get(chatId)); // Состояние не должно измениться, так как мы не вводили тикер

            // Шаг 3: Проверка вызова методов при вводе тикера
            String tickerInput = "TSLA"; // Предполагаемый ввод пользователя
            userState.put(chatId, "AWAITING_STOCK_TICKER"); // Меняем состояние на ожидание ввода тикера
            bot.handleGetStockQuote(chatId, tickerInput, keyboardMarkup, userState);

            // Здесь можно добавить дополнительные проверки, основанные на вашей логике
            // Например, можно проверить, что userInputTicker == tickerInput и т.д.
        }

    }
}
