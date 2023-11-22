package ebe.P_Judakov.s.JAVABOT.service.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import ebe.P_Judakov.s.JAVABOT.domen.entity.jpa.StockDataEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StockDataService {

    private static final String API_BASE_URL = "https://alpha-vantage.p.rapidapi.com/query";
    private static final String API_FUNCTION_GLOBAL_QUOTE = "GLOBAL_QUOTE";

    private static final Logger LOGGER = LoggerFactory.getLogger(StockDataService.class);

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public StockDataService() {
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public StockDataEntity getStockData(String symbol) {
        try {
            // Формирование URL-запроса
            String apiUrl = String.format("%s?function=%s&symbol=%s", API_BASE_URL, API_FUNCTION_GLOBAL_QUOTE, symbol);

            // Создание запроса
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .get()
                    .addHeader("X-RapidAPI-Key", "4dfa492779msh47fb50b07bc7c09p11ff37jsn1c0c729af2c0")
                    .addHeader("X-RapidAPI-Host", "alpha-vantage.p.rapidapi.com")
                    .build();

            // Отправка запроса и получение ответа
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                // Обработка ошибок, вывод логов или выброс исключения
                LOGGER.error("Ошибка: {} - {}", response.code(), response.message());
                return null;
            }

            // Обработка ответа
            String responseBody = response.body().string();
            StockDataEntity stockData = objectMapper.readValue(responseBody, StockDataEntity.class);

            // Логируем содержимое ответа
            LOGGER.info("Содержимое ответа: {}", responseBody);
            return stockData;

        } catch (Exception e) {
            // Обработка исключений
            LOGGER.error("Ошибка при выполнении запроса для акции {}: {}", symbol, e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}