package ebe.P_Judakov.s.JAVABOT.service.interfaces;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;

public interface IStockQuoteBot {
    void handleGetStockQuote(long chatId, String stockTicker, ReplyKeyboardMarkup keyboardMarkup, Map<Long, String> userState) throws TelegramApiException;

    void sendTextMessageWithKeyboard(Long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) throws TelegramApiException;

    String fetchStockQuoteInfo(String stockTicker);

    void sendErrorMessage(long chatId, String errorMessage);

    void onUpdatesReceived(List<Update> updates);

    void sendTextMessageWithTickerInput(long chatId, String message, ReplyKeyboardMarkup keyboardMarkup) throws TelegramApiException;

}
