package ebe.P_Judakov.s.JAVABOT;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import ebe.P_Judakov.s.JAVABOT.service.jpa.TelegramBotService;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;
public class TestProcessUserTicker {
    private final OkHttpClient client = new OkHttpClient();

    // Тест проверяет обработку тикера от пользователя
    @Test
    public void testProcessUserTicker() {
        String ticker = "AAPL"; // Пример тикера акции для теста

        try {
            String response = processUserTicker(ticker);

            assertNotNull(response); // Проверка на то, что ответ не равен null

            // Добавьте дополнительные проверки, если необходимо
            // Например, проверка содержимого ответа или его формата
        } catch (IOException e) {
            fail("Исключение при выполнении запроса: " + e.getMessage());
        }
    }

    // Метод для обработки тикера от пользователя
    public String processUserTicker(String ticker) throws IOException {
        Request request = new Request.Builder()
                .url("https://alpha-vantage.p.rapidapi.com/query?function=GLOBAL_QUOTE&symbol=" + ticker)
                .addHeader("X-RapidAPI-Key", "4dfa492779msh47fb50b07bc7c09p11ff37jsn1c0c729af2c0")
                .addHeader("X-RapidAPI-Host", "alpha-vantage.p.rapidapi.com")
                .build();

        Response response = client.newCall(request).execute();

        // Обрабатываем тело ответа, если необходимо
        return response.body().string();
    }

    @Test
    public void testProcessUserTicker2() {
        // Создание объекта класса, содержащего методы для тестирования
        TelegramBotService.StockQuoteBot stockQuoteBot = new TelegramBotService.StockQuoteBot();

        // Предположим, что пользователь вводит тикер "AAPL"
        String ticker = "AAPL";

        // Вызов метода processUserTicker для получения информации о котировках акции
        String result = stockQuoteBot.processUserTicker(ticker);

        // Проверка, что результат не равен null и содержит ожидаемые данные
        assertNotNull(result);
        assertTrue(result.contains("AAPL")); // Здесь нужно указать ожидаемую информацию в ответе
    }
}
