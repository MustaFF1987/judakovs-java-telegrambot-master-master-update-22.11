package ebe.P_Judakov.s.JAVABOT;
import ebe.P_Judakov.s.JAVABOT.service.jpa.TelegramBotService;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestHandleGetStockQuote {
/*
    @Mock
    private ReplyKeyboardMarkup keyboardMarkup;

    @Mock
    private Map<Long, String> userState;

    @Mock
    private StockQuoteBot yourTelegramBot; // Подставьте ваш класс Telegram бота

    @InjectMocks
    private StockQuoteBot yourClassUnderTest; // Подставьте ваш класс, содержащий метод handleGetStockQuote

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testHandleGetStockQuote() throws TelegramApiException {
        // Подготовка данных для теста
        long chatId = 12345L;
        String stockTicker = "AAPL";
        when(userState.containsKey(chatId)).thenReturn(false);

        // Вызов метода, который мы хотим протестировать
        yourClassUnderTest.handleGetStockQuote(chatId, stockTicker, keyboardMarkup, userState);

        // Проверка, что был вызван метод sendTextMessageWithKeyboard
        verify(yourTelegramBot).sendTextMessageWithKeyboard(eq(chatId), anyString(), eq(keyboardMarkup));

        // Проверка, что состояние пользователя было изменено
        verify(userState).put(eq(chatId), anyString());
    }
*/
    @Test
    public void testHandleGetStockQuote() {
        TelegramBotService.StockQuoteBot bot = new TelegramBotService.StockQuoteBot(); // Создание экземпляра класса
        long chatId = 12345L;
        String stockTicker = "AAPL";
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        Map<Long, String> userState = new HashMap<>();

        // Вызов метода, который мы хотим протестировать
        bot.handleGetStockQuote(chatId, stockTicker, keyboardMarkup, userState);

        // Проверки на ожидаемое поведение метода
        assertEquals("AWAITING_STOCK_TICKER", userState.get(chatId));
    }


}
