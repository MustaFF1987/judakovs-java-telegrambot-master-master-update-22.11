package ebe.P_Judakov.s.JAVABOT.scheduler;

import ebe.P_Judakov.s.JAVABOT.repository.interfaces.SubscribedChannelRepository;
import ebe.P_Judakov.s.JAVABOT.service.jpa.ApiClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
@EnableScheduling
@EnableAsync
public class ScheduleExecutor extends TelegramLongPollingBot {

    @Autowired
    SubscribedChannelRepository subscribedChannelRepository;

    @Autowired
    ApiClientService apiClientService;

//    @Scheduled(cron = "0 0 9 * * ?") // Каждый день в 09:00
//    private void handleDailySubscription(Long chatId) {
//    }

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

