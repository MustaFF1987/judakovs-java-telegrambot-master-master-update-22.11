package ebe.P_Judakov.s.JAVABOT;
import ebe.P_Judakov.s.JAVABOT.service.jpa.TelegramBotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StockQuoteBotTest {

    private TelegramBotService.StockQuoteBot stockQuoteBot;
    private long chatId = 123456789;
    private ReplyKeyboardMarkup keyboardMarkup;
    private Map<Long, String> userState;

    @BeforeEach
    public void setUp() {
        stockQuoteBot = new TelegramBotService.StockQuoteBot();
        keyboardMarkup = new ReplyKeyboardMarkup();
        userState = new HashMap<>();
    }

    @Test
    public void testHandleGetStockQuote() {
        String stockTicker = "AAPL"; // Пример тикера акции

        stockQuoteBot.handleGetStockQuote(chatId, stockTicker, keyboardMarkup, userState);

        // Проверяем, что состояние пользователя изменилось после запроса акции
        assertEquals("AWAITING_STOCK_TICKER", userState.get(chatId));
    }
}


