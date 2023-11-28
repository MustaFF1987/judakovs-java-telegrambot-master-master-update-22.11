package ebe.P_Judakov.s.JAVABOT.service.jpa;

public class TelegramMessageSender {

    // реализация отправки сообщения через Telegram API
    public void sendMessage(Long chatId, String message) {
        System.out.println("Отправка сообщения пользователю с chatId: " + chatId + ", сообщение: " + message);
    }
}
