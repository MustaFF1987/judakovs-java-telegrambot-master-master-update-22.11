package ebe.P_Judakov.s.JAVABOT;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApiTest {

    @Test
    void testApiAvailability() {
        String baseUrl = "https://alpha-vantage.p.rapidapi.com/query?function=GLOBAL_QUOTE&symbol=TSLA"; // Замените на ваш URL

        // Отправка GET-запроса по указанному URL
        Response response = RestAssured.get(baseUrl);

        // Проверка статуса ответа
        int statusCode = response.getStatusCode();
        assertEquals(200, statusCode, "Некорректный статус ответа");

        // Проверка наличия ожидаемого контента или фразы в ответе
        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("ожидаемый_контент_или_фраза"),
                "Ожидаемый контент отсутствует в ответе");
    }
}

