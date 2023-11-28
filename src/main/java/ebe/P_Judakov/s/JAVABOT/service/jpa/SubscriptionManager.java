package ebe.P_Judakov.s.JAVABOT.service.jpa;

import ebe.P_Judakov.s.JAVABOT.domen.entity.interfaces.SubscribedChannel;
import ebe.P_Judakov.s.JAVABOT.repository.interfaces.SubscribedChannelRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;



    public class SubscriptionManager {

        private SubscribedChannelRepository subscribedChannelRepository;

        private static Set<Long> subscribers = new HashSet<>();

        public static void subscribe(Long chatId) {
            subscribers.add(chatId);
        }

        public static void unsubscribe(Long chatId) {
            subscribers.remove(chatId);
        }

        public SubscriptionManager(SubscribedChannelRepository subscribedChannelRepository) {
            this.subscribedChannelRepository = subscribedChannelRepository;
        }

        public Long getChatIdForDailyStockInfo() {
            Long chatId = null;

            // Логика получения chatId для ежедневной информации
            // Например, предположим, что вам нужно получить первый подписанный канал
            List<SubscribedChannel> subscribedChannels = subscribedChannelRepository.findAll();

            // Проверяем, не пустой ли список подписанных каналов
            if (!subscribedChannels.isEmpty()) {
                // Извлекаем chatId из первого элемента списка
                chatId = (long) subscribedChannels.get(0).getChatId();
            }
            return chatId;
        }

    }