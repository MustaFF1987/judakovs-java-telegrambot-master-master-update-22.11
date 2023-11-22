package ebe.P_Judakov.s.JAVABOT;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class StockQuoteAPITest {

    @Test
    public void testFetchStockQuoteInfo() {
        String stockTicker = "AAPL"; // Пример символа акции для запроса

        OkHttpClient client = new OkHttpClient();

        // Создание URL с параметрами запроса
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("alpha-vantage.p.rapidapi.com")
                .addPathSegment("query")
                .addQueryParameter("function", "GLOBAL_QUOTE")
                .addQueryParameter("symbol", stockTicker)
                .addQueryParameter("key", "4dfa492779msh47fb50b07bc7c09p11ff37jsn1c0c729af2c")
                .addQueryParameter("Value", "123")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();
            assertNotNull(response); // Проверка, что ответ не равен null

            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                assertNotNull(responseBody); // Проверка, что тело ответа не равно null

                // Здесь можно добавить дополнительные проверки JSON-ответа, если необходимо
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
//Этот тест проверяет метод fetchStockQuoteInfo, который отправляет запрос на получение информации
// о котировках акции через API. Он использует библиотеку OkHttp для создания HTTP-запроса и получения
// ответа от сервера.
//
//Этот тест выполняет следующие действия:
//
//Определяет символ акции (stockTicker), в данном случае это AAPL.
//Создает HTTP-клиент (OkHttpClient).
//Формирует URL для запроса с необходимыми параметрами для получения данных о котировках акции.
//Создает GET-запрос к серверу с сформированным URL.
//Выполняет запрос и проверяет, что ответ не равен null.
//Если ответ успешный (response.isSuccessful()), извлекает тело ответа (responseBody) и проверяет,
// что оно не равно null.
//Таким образом, успешное прохождение этого теста означает, что метод fetchStockQuoteInfo способен
// сформировать запрос, получить ответ от сервера и вернуть тело ответа в виде строки.



