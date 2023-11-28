package ebe.P_Judakov.s.JAVABOT.service.jpa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TelegramMessageHandler {

    private Connection connection;
    private static final Logger LOGGER = Logger.getLogger(TelegramMessageHandler.class.getName());

    public TelegramMessageHandler() {
        // Инициализация подключения к базе данных
        this.connection = establishDatabaseConnection();
    }

    public void handleMessage(Long chatId, String messageText) {
        // Обработка сообщения от Telegram бота

        // Запись сообщения в базу данных
        saveMessageToDatabase(chatId.toString(), messageText);
    }

    private Connection establishDatabaseConnection() {
        // Установка соединения с базой данных
        try {
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/telegram_bot_10-170123-e-be", "root", "123Unikorpa12");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private void saveMessageToDatabase(String chatId, String messageText) {
        // Проверяем наличие активного соединения с базой данных
        if (connection != null) {
            try {
                String sql = "INSERT INTO message (chat_id, text) VALUES (?, ?)";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, chatId);
                statement.setString(2, messageText);
                statement.executeUpdate();
                LOGGER.info("Сообщение сохранено в базе данных: chatId=" + chatId + ", text=" + messageText);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Ошибка при сохранении сообщения в базе данных", e);
            }
        } else {
            LOGGER.warning("Отсутствует активное соединение с базой данных.");
        }
    }
}

