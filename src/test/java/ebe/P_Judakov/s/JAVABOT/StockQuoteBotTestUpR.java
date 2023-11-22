package ebe.P_Judakov.s.JAVABOT;

import ebe.P_Judakov.s.JAVABOT.service.jpa.TelegramBotService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class StockQuoteBotTestUpR {

    @Test
    public void testOnUpdateReceived() throws TelegramApiException {
        // Создаем мок бота
        TelegramBotService.StockQuoteBot bot = mock(TelegramBotService.StockQuoteBot.class);

        // Создаем Update с Message
        Update update = new Update();
        Message message = new Message();
        message.setText("/getSchtok");
        update.setMessage(message);

        // Подготавливаем параметры
        long chatId = 123L;
        String stockTicker = "AAPL";
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        // Вызываем метод, который тестируем
        bot.onUpdateReceived(update);

        // Проверяем, что методы были вызваны с ожидаемыми параметрами
        ArgumentCaptor<Long> chatIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> stockTickerCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ReplyKeyboardMarkup> keyboardMarkupCaptor = ArgumentCaptor.forClass(ReplyKeyboardMarkup.class);

        verify(bot).sendTextMessageWithTickerInput(chatIdCaptor.capture(), eq("Введите тикер акции:"), keyboardMarkupCaptor.capture());
        verify(bot).handleGetStockQuote(chatIdCaptor.capture(), stockTickerCaptor.capture(), any(), any());

        // Проверяем параметры, переданные в методы
        assertEquals(chatId, chatIdCaptor.getValue());
        assertEquals(stockTicker, stockTickerCaptor.getValue());
        // Можно добавить дополнительные проверки для keyboardMarkupCaptor, если нужно
    }



}




