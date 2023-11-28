package ebe.P_Judakov.s.JAVABOT.scheduler;

import ebe.P_Judakov.s.JAVABOT.repository.interfaces.SubscribedChannelRepository;
import ebe.P_Judakov.s.JAVABOT.service.jpa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


@Component
@EnableScheduling
@EnableAsync
public class ScheduleExecutor extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramBotService.class);

    @Autowired
    SubscribedChannelRepository subscribedChannelRepository;

    @Autowired
    ApiClientService apiClientService;

    SubscriptionManager subscriptionManager;

    StockDataService stockDataService;

    // Создание планировщика задач
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Scheduled(cron = "0 0 9 * * ?") // Каждый день в 09:00
    private void scheduleDailyStockInfo() {
        Long chatId = subscriptionManager.getChatIdForDailyStockInfo();
        String stockTicker = stockDataService.getStockTickerForDailyStockInfo();

        if (chatId != null && stockTicker != null) {
            handleDailySubscription(chatId, stockTicker);
        } else {
            LOGGER.error("ChatId or stockTicker for daily subscription not found!");

            // Если тикер акции не найден, отправляем уведомление пользователю
            TelegramMessageSender messageSender = new TelegramMessageSender();
            messageSender.sendMessage(chatId, "Тикер акции не был найден. Пожалуйста, попробуйте еще раз.");

        }
    }

    private void handleDailySubscription(Long chatId, String stockTicker) {
    }


//    @Scheduled(cron = "0 0 9 ? * MON") // Каждый понедельник в 09:00
//    private void handleWeeklySubscription(Long chatId) {
//    }

//    @Scheduled(cron = "0 0 09 1 * ?") // 1-ое число каждого месяца в 09:00
//    private void handleMonthlySubscription(Long chatId) {
//    }

    @Override
    public void onUpdateReceived(Update update) {
    }
    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    @Override
    public String getBotUsername() {
        return null;
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }


}

