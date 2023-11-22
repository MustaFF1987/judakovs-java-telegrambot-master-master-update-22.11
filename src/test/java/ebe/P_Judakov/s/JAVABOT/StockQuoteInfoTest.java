package ebe.P_Judakov.s.JAVABOT;
import com.squareup.okhttp.*;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import ebe.P_Judakov.s.JAVABOT.service.jpa.TelegramBotService;
import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;

public class StockQuoteInfoTest {
    private TelegramBotService.StockQuoteBot stockQuoteBot;

    @Test
    public void testFetchStockQuoteInfo() throws IOException, InterruptedException {
        stockQuoteBot = new TelegramBotService.StockQuoteBot(); // Создание экземпляра вашего класса

        MockWebServer mockWebServer = new MockWebServer(); // Создание мок-сервера

        // Начало работы мок-сервера
        mockWebServer.start();

        // Ожидаемый ответ сервера на запрос
        MockResponse mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody("{\"4dfa492779msh47fb50b07bc7c09p11ff37jsn1c0c729af2c\": \"123\"}");

        // Установка ожидаемого ответа для запроса к серверу
        mockWebServer.enqueue(mockResponse);

        // Получение URL для тестирования из мок-сервера
        HttpUrl baseUrl = mockWebServer.url("/query?function=GLOBAL_QUOTE&symbol=AAPL"); // Замените на ваш символ акции

        OkHttpClient client = new OkHttpClient();

        // Тестирование вашего метода с символом акции
        String result = stockQuoteBot.fetchStockQuoteInfo(baseUrl.toString());

        // Ожидаемый запрос к серверу
        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        // Проверка корректности запроса и ответа
        assertEquals("/query?function=GLOBAL_QUOTE&symbol=AAPL", recordedRequest.getPath()); // Замените на ваш символ акции
        assertNotNull(result);
        assertEquals("{\"4dfa492779msh47fb50b07bc7c09p11ff37jsn1c0c729af2c\": \"123\"}", result);

        // Остановка работы мок-сервера
        mockWebServer.shutdown();
    }
}





//ресурс по указанному адресу ? или доступ к нему ?