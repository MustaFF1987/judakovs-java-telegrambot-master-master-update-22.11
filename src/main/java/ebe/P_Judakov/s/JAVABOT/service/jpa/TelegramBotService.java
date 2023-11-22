package ebe.P_Judakov.s.JAVABOT.service.jpa;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import ebe.P_Judakov.s.JAVABOT.controller.CombinedController;
import ebe.P_Judakov.s.JAVABOT.domen.entity.jpa.EmptyBot;
import ebe.P_Judakov.s.JAVABOT.domen.entity.jpa.JpaMessage;
import ebe.P_Judakov.s.JAVABOT.repository.interfaces.SubscribedChannelRepository;
import ebe.P_Judakov.s.JAVABOT.service.interfaces.IStockQuoteBot;
import ebe.P_Judakov.s.JAVABOT.service.interfaces.UserService;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class TelegramBotService extends TelegramLongPollingBot implements ebe.P_Judakov.s.JAVABOT.service.interfaces.TelegramBotService {


    @Qualifier("telegramBotService")
    private SubscribedChannelRepository subscribedChannelRepository;

    private JpaUserService jpaUserService;


    // переменная для хранения ввода с клавиатуры
    private ReplyKeyboardMarkup keyboardMarkup;
    private UserService userService;

    private CombinedController combinedController;
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramBotService.class);

    public void setCombinedController(CombinedController combinedController) {
        this.combinedController = combinedController;
    }

    public void setUserService(JpaUserService userService) {
        this.userService = userService;
    }

    // Переменная состояния
    private Map<Long, String> userState = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        // Логируем приходящие обновления
        LOGGER.info("Received update: {}", update);

        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (text.startsWith("/start") && keyboardMarkup == null) {
                // Инициализация клавиатуры при старте чата
                keyboardMarkup = createKeyboardMarkup();
            }

            if (keyboardMarkup != null) {
                try {
                    handleCommands(update);
                } catch (TelegramApiException e) {
                    LOGGER.error("Error processing Telegram API request", e);
                    throw new RuntimeException(e);
                }
            } else {
                // Без клавиатуры
                try {
                    processIncomingMessage(update);
                } catch (TelegramApiException e) {
                    LOGGER.error("Error processing Telegram API request", e);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // Вызывается при регистрации бота в Telegram.
    // Этот метод вызывается автоматически библиотекой TelegramBots
    // после успешной регистрации бота и позволяет выполнять дополнительные действия или настройки в момент регистрации.
    @Override
    public void onRegister() {
        super.onRegister();
    }

    private void handleCommands(Update update) throws TelegramApiException {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if ("/start".equals(text)) {
            sendWelcomeMessage(chatId, keyboardMarkup);
        } else if ("/help".equals(text)) {
            String responseText = "Список доступных команд: /start, /help, /stop, /getStock";
            sendTextMessageWithKeyboard(chatId, responseText, keyboardMarkup);
        } else if ("Subscribe".equals(text)) {
            handleSubscribe(chatId);
        } else if ("Unsubscribe".equals(text)) {
            handleUnsubscribe(chatId);
        } else if ("/stop".equals(text)) {
            sendStopMessage(chatId, keyboardMarkup);
        } else if (text.startsWith("/getStock")) {
            String stockTicker = getStockTickerFromMessage(text);
            if (stockTicker != null) {
                handleGetStockQuote(update, chatId, stockTicker, keyboardMarkup, userState);
            } else {
                sendTextMessageWithKeyboard(chatId, "Некорректный формат команды /getStock", keyboardMarkup);
            }
        }
    }

    // Обработка получения котировок акций
    public void handleGetStockQuote(Update update, long chatId, String stockTicker, ReplyKeyboardMarkup keyboardMarkup, Map<Long, String> userState) {
        try {
            if (!userState.containsKey(chatId)) {
                sendTextMessageWithKeyboard(chatId, "Введите тикер акции:", keyboardMarkup);
                userState.put(chatId, "AWAITING_STOCK_TICKER");
            } else if (userState.get(chatId).equals("AWAITING_STOCK_TICKER")) {
                String userInputTicker = getUserInputTicker(update); // Получение ввода пользователя из объекта Update
                String processedTicker = processUserTicker(userInputTicker); // Обработка ввода

                sendStockQuoteInfo(chatId, processedTicker, keyboardMarkup); // Отправка информации о котировках
            }
        } catch (Exception e) {
            e.printStackTrace(); // Обработка ошибок
        }
    }

    // Метод для получения текстового сообщения пользователя из объекта Update
    private String getUserInputTicker(Update update) {
        return update.getMessage().getText();
    }

    // Метод для извлечения тикера из текста сообщения
    private String getStockTickerFromMessage(String text) {
        try {
            // Regex выражение для извлечения тикера из текста сообщения
            Pattern pattern = Pattern.compile("/getStock\\s+(\\S+)");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                // Получаем найденное значение тикера
                return matcher.group(1);
            }
        } catch (Exception e) {
            // Обработка ошибок
            e.printStackTrace();
        }
        return null;
    }

    private String processUserTicker(String userInputTicker) {
        // Преобразование тикера акции в верхний регистр (пример логики обработки)
        return userInputTicker.toUpperCase(); // Возвращаем тикер акции в верхнем регистре
        }

    // Пример реализации метода sendStockQuoteInfo (метод отправки информации о котировках акции пользователю)
    private void sendStockQuoteInfo(long chatId, String stockTicker, ReplyKeyboardMarkup keyboardMarkup) {
        try {
            // Ваш код для отправки запроса к API для получения информации о котировках акции
            // В данном примере, используется заглушка и вывод в консоль

            System.out.println("Запрос к API для получения информации о котировках акции с тикером: " + stockTicker);

            // sendTextMessageWithKeyboard(chatId, stockQuoteInfo, keyboardMarkup);

        } catch (Exception e) {
            e.printStackTrace(); // Обработка ошибок при запросе к API или отправке сообщения
        }
    }

    private void handleSubscribe(Long chatId) throws TelegramApiException {
        SubscriptionManager.subscribe(chatId);
        String responseText = "Вы подписались на уведомления от бота.";
        keyboardMarkup = removeSubscribeButton(keyboardMarkup);
        sendTextMessageWithKeyboard(chatId, responseText, keyboardMarkup);
        jpaUserService.setUserRole(chatId, "USER"); // Устанавливаем роль пользователя
        addEmptyBotToChat(chatId.toString());
    }

    private void handleUnsubscribe(Long chatId) throws TelegramApiException {
        SubscriptionManager.unsubscribe(chatId);
        String responseText = "Вы отписались от уведомлений от бота, вы можете подписаться снова.";
        keyboardMarkup = updateKeyboardWithSubscribeButton(keyboardMarkup); // Обновляем клавиатуру
        sendTextMessageWithKeyboard(chatId, responseText, keyboardMarkup);
    }

    private ReplyKeyboardMarkup createKeyboardWithSubscribeButton() {
        // Создаем новую клавиатуру с кнопкой "Подписаться"
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        // Создаем строки и добавляем кнопку "Подписаться" и "Отписаться"
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Subscribe");
        row.add("Unsubscribe");
        keyboard.add(row);

        // Устанавливаем клавиатуру в клавиатуре разметке
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup updateKeyboardWithSubscribeButton(ReplyKeyboardMarkup keyboardMarkup) {
        // Проверяем, существует ли клавиатура
        if (keyboardMarkup == null) {
            // Создаем новую клавиатуру, если она не была создана
            keyboardMarkup = createKeyboardWithSubscribeButton();
        } else {
            boolean subscribeButtonExists = false;
            // Проверяем, существует ли кнопка "Подписаться" в текущей клавиатуре
            List<KeyboardRow> keyboard = keyboardMarkup.getKeyboard();
            for (KeyboardRow row : keyboard) {
                for (KeyboardButton button : row) {
                    if ("Subscribe".equals(button.getText())) {
                        subscribeButtonExists = true;
                        break;
                    }
                }
                if (subscribeButtonExists) {
                    break;
                }
            }
            // Если кнопка "Подписаться" еще не существует, добавляем ее в клавиатуру
            if (!subscribeButtonExists) {
                KeyboardRow subscribeRow = new KeyboardRow();
                subscribeRow.add("Subscribe");
                keyboardMarkup.getKeyboard().add(subscribeRow);
            }
        }
        return keyboardMarkup;
    }

    // Метод для добавления пустого бота в чат
    public void addEmptyBotToChat(String chatId) {
        // токен пустого бота
        String emptyBotToken = "6553161657:AAGK4LG72f3lDBcJWW-SUBTodrM_2HBvsUc";

        TelegramBotsApi telegramBotsApi = null;
        try {
            telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        try {
            TelegramLongPollingBot emptyBot = new EmptyBot(emptyBotToken);
            telegramBotsApi.registerBot(emptyBot);

            // Добаление пустого бота в чат
            sendTextMessageWithKeyboard(Long.parseLong(chatId), "Пустой бот добавлен в чат.", keyboardMarkup);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            // Обработка ошибок при добавлении пустого бота
        }
    }

    //  метод removeSubscribeButton для удаления кнопки "Подписаться" из клавиатуры.
    private ReplyKeyboardMarkup removeSubscribeButton(ReplyKeyboardMarkup keyboardMarkup) {
        List<KeyboardRow> keyboard = keyboardMarkup.getKeyboard();

        // Перебираем строки клавиатуры и удаляем кнопку "Subscribe"
        for (KeyboardRow row : keyboard) {
            row.removeIf(button -> "Subscribe".equals(button.getText()));
        }
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup createKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        // Создаем строки и кнопки
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Subscribe");
        row.add("Unsubscribe");
        keyboard.add(row);

        // Устанавливаем клавиатуру в клавиатуре разметке
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }


    // Метод для отправки приветственного сообщения
    private void sendWelcomeMessage(Long chatId, ReplyKeyboardMarkup keyboardMarkup) throws TelegramApiException {
        String welcomeText = "Добро пожаловать! Я ваш телеграм-бот.";
        welcomeText += " Начните взаимодействие с командой /help.";
        sendTextMessageWithKeyboard(chatId, welcomeText, keyboardMarkup);
    }


    // Метод для отправки сообщения при завершении работы с ботом с клавиатурой
    public void sendStopMessage(Long chatId, ReplyKeyboardMarkup keyboardMarkup) throws TelegramApiException {
        String stopText = "Вы завершили работу с ботом.";
        stopText += " До скорых встреч!";

        sendTextMessageWithKeyboard(chatId, stopText, keyboardMarkup);
    }

    @Override
    public String getBotUsername() {
        // Имя бота, зарегистрированный в Telegram
        return "PA_YU_Unikorpa_Telegram_Bot";
    }

    @Override
    public String getBotToken() {
        // Токен бота, который получили при регистрации в Telegram
        return "6669687693:AAGVbVs_AHL22m0w-7rjPW8_h_alNLV6jBo";
    }

    // метод для инициализации бота
    @Override
    public void init() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(this);
            System.out.println("Бот зарегистрирован и готов к работе, введите команду /start.");
        } catch (TelegramApiException e) {
            e.printStackTrace();
            // Обработка ошибок при регистрации бота
        }
    }
    @Override
    public Message execute(SendMessage message) {
        try {
            return super.execute(message); // Отправка сообщения и возвращение результата
        } catch (Exception e) {
            e.printStackTrace();
            // Обработка ошибок при отправке сообщения
            System.out.println("Ошибка при отправке сообщения");
            return null; // Возвращаем null в случае ошибки
        }
    }

    // Метод для отправки текстовых сообщений с клавиатурой
    public void sendTextMessageWithKeyboard(Long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        // Устанавливаем клавиатуру в сообщение
        message.setReplyMarkup(keyboardMarkup);

        Message sentMessage = execute(message);
        // Обработка успешной отправки сообщения (sentMessage содержит информацию о сообщении)
        System.out.println("Сообщение успешно отправлено: " + sentMessage);
    }


    // Метод для обработки входящих сообщений
    public void processIncomingMessage(Update update) throws TelegramApiException {
        if (update.hasMessage()) {
            Long chatId = update.getMessage().getChatId();

            if (keyboardMarkup != null) {
                if (update.getMessage().hasText()) {
                    Message message = update.getMessage();
                    String text = message.getText();

                    // Ваш код для обработки текстовых сообщений
                    String responseText = "Ваше сообщение: " + text;

                    // Отправка ответа пользователю с использованием клавиатуры
                    sendTextMessageWithKeyboard(chatId, responseText, keyboardMarkup);
                } else if (update.getMessage().hasPhoto()) {
                    // Обработка сообщений с фотографиями
                    Message message = update.getMessage();
                    String caption = message.getCaption();

                    // Ваш код для обработки сообщений с фотографиями
                    String responseText = "Вы отправили фотографию с подписью: " + caption;

                    // Отправка ответа пользователю с использованием клавиатуры
                    sendTextMessageWithKeyboard(chatId, responseText, keyboardMarkup);
                }
            } else {
            }
        }
    }

    @Override
    public void onUpdateReceived(org.hibernate.sql.Update update) {

    }

    @Override
    public void processIncomingMessage(org.hibernate.sql.Update update) throws TelegramApiException {
    }

    public static class StockQuoteBot extends TelegramLongPollingBot implements IStockQuoteBot {

        // Метод для обработки команды /getStock
        // метод организует процесс взаимодействия с пользователем: запрос ввода тикера,
        // ожидание ввода и обработка полученной информации о котировках акции для последующей
        // отправки пользователю.
        public void handleGetStockQuote(long chatId, String stockTicker, ReplyKeyboardMarkup keyboardMarkup, Map<Long, String> userState) {
            // Обработка запроса пользователя для получения информации о котировках акции
            try {
                if (!userState.containsKey(chatId)) {
                    // Если состояние чата не содержит информации о текущем запросе
                    sendTextMessageWithKeyboard(chatId, "Введите тикер акции:", keyboardMarkup);
                    userState.put(chatId, "AWAITING_STOCK_TICKER");
                } else if (userState.get(chatId).equals("AWAITING_STOCK_TICKER")) {
                    // Метод обработки тикера акции от пользователя
                    String userInputTicker = getUserInputTicker(); // Шаг 1
                    // Метод для обработки тикера от пользователя к контроллеру
                    String processedTicker = processUserTicker(userInputTicker); // Шаг 2
                    // Метод для отправки информации о котировках акции пользователю
                    sendStockQuoteInfo(chatId, processedTicker, keyboardMarkup); // Шаг 3
                }
            } catch (Exception e) {
                // Обработка ошибок
                e.printStackTrace();
            }
        }


        // Метод обработки тикера акции от пользователя,
        // что метод запрашивает у пользователя ввод тикера акции
        public String getUserInputTicker() {
            // Создаем объект Scanner для чтения с клавиатуры
            Scanner scanner = new Scanner(System.in);

            // Получаем ввод от пользователя
            System.out.println("Введите тикер акции:");
            String userInput = scanner.nextLine();

            // Закрываем Scanner
            scanner.close();

            // Возвращаем введенное значение пользователя
            return userInput;
        }

        private final OkHttpClient client = new OkHttpClient();

        // Метод для обработки тикера от пользователя к контроллеру
        public String processUserTicker(String ticker) {
            try {
                return fetchStockQuoteInfo(ticker);
            } catch (Exception e) {
                return "Ошибка при выполнении запроса";
            }
        }

        private JpaMessage lastSentMessage;

        // Метод для получения последнего отправленного сообщения
        public String getLastMessageSent() {
            if (lastSentMessage != null) {
                return lastSentMessage.getText();
            } else {
                return null;
            }
        }

        public void setLastSentMessage(JpaMessage message) {
            this.lastSentMessage = message;
        }

        // Метод для отправки текстовых сообщений с клавиатурой
        public void sendTextMessageWithKeyboard(Long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) throws TelegramApiException {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);

            if (keyboardMarkup != null) {
                message.setReplyMarkup(keyboardMarkup);
            } else {
                // Если keyboardMarkup равен null, создаем и используем клавиатуру по умолчанию
                ReplyKeyboardMarkup defaultKeyboard = new ReplyKeyboardMarkup();
                defaultKeyboard.setResizeKeyboard(true);
                // Добавьте кнопки по умолчанию, если нужно
                // defaultKeyboard.setKeyboard(...);

                // Устанавливаем клавиатуру по умолчанию
                message.setReplyMarkup(defaultKeyboard);
            }

            Message sentMessage = execute(message);
            // Обработка успешной отправки сообщения
            System.out.println("Сообщение успешно отправлено: " + sentMessage);
        }


        /**
         * Метод для получения информации о котировках акций по символу через API.
         *
         * @param stockTicker символ акции для получения котировок
         * @return строка с информацией о котировках акции в формате JSON или null в случае ошибки
         */
        // Метод для запроса информации о котировках акции через API
        public String fetchStockQuoteInfo(String stockTicker) {
            try {
                // Базовый URL для запроса к API
                String baseUrl = "https://alpha-vantage.p.rapidapi.com/query";

                // Задание функции запроса (GLOBAL_QUOTE) и символа акции
                String function = "GLOBAL_QUOTE";
                String symbol = stockTicker;

                // Создание клиента HTTP запросов
                OkHttpClient client = new OkHttpClient();

                // Построение URL запроса с необходимыми параметрами
                HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl)
                        .newBuilder()
                        .addQueryParameter("function", function)
                        .addQueryParameter("symbol", symbol)
                        .addQueryParameter("key", "4dfa492779msh47fb50b07bc7c09p11ff37jsn1c0c729af2c")
                        .addQueryParameter("Value", "123");

                String url = urlBuilder.build().toString();

                // Формирование и отправка GET запроса
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                // Получение ответа от сервера
                Response response = client.newCall(request).execute();

                // Проверка успешности запроса и получение информации о котировках
                if (response.isSuccessful()) {
                    return response.body().string();
                } else {
                    return null;
                }
            } catch (IOException e) {
                // Вывод информации об ошибке в случае возникновения исключения
                e.printStackTrace();
                return null;
            }
        }


        // Метод для отправки информации о котировках акции пользователю
        public void sendStockQuoteInfo(long chatId, String stockQuoteInfo, ReplyKeyboardMarkup keyboardMarkup) throws TelegramApiException {
            // Обработка JSON-ответа для отправки пользователю
            JSONObject jsonResponse = new JSONObject(stockQuoteInfo);
            JSONObject globalQuote = jsonResponse.getJSONObject("Global Quote");

            // Получение значений котировок акции
            String symbol = globalQuote.getString("01. symbol");
            String open = globalQuote.getString("02. open");
            String high = globalQuote.getString("03. high");
            String low = globalQuote.getString("04. low");
            String price = globalQuote.getString("05. price");
            String volume = globalQuote.getString("06. volume");
            String latestTradingDay = globalQuote.getString("07. latest trading day");
            String previousClose = globalQuote.getString("08. previous close");
            String change = globalQuote.getString("09. change");
            String changePercent = globalQuote.getString("10. change percent");

            // Формирование сообщения для отправки пользователю
            String message = String.format(
                    "Symbol: %s\nOpen: %s\nHigh: %s\nLow: %s\nPrice: %s\nVolume: %s\nLatest Trading Day: %s\nPrevious Close: %s\nChange: %s\nChange Percent: %s",
                    symbol, open, high, low, price, volume, latestTradingDay, previousClose, change, changePercent
            );

            // Отправка сообщения пользователю
            sendCustomKeyboardMessage(chatId, message, keyboardMarkup);
        }



        // Отправка сообщения с кастомной клавиатурой пользователю
        private void sendCustomKeyboardMessage(long chatId, String message, ReplyKeyboardMarkup keyboardMarkup) throws TelegramApiException {
            // Создаем объект для отправки сообщения
            SendMessage sendMessage = new SendMessage();

            // Устанавливаем идентификатор чата и текст сообщения
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(message);

            // Устанавливаем клавиатуру в сообщение
            sendMessage.setReplyMarkup(keyboardMarkup);

            // Отправляем сообщение и получаем информацию о нем
            Message sentMessage = execute(sendMessage);

            try {
                // Отправляем сообщение и получаем информацию о нем
                execute(sendMessage);

                // Обработка успешной отправки сообщения
                System.out.println("Сообщение о катеровках акции успешно отправлено.");
            } catch (TelegramApiException e) {
                // Обработка ошибок при отправке сообщения
                e.printStackTrace();
            }
        }
        private ReplyKeyboardMarkup keyboardMarkup;

        public void onUpdateReceived(Update update) {
            Map<Long, String> userState = new HashMap<>(); // Создаем локальную переменную userState
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();

                if (text.equals("/getStock")) {
                    sendEnterStockTickerMessage(chatId, null );
                } else {
                    // Обработка тикера акции, если он был введен после команды /getSchtok
                    handleGetStockQuote(chatId, text, keyboardMarkup, userState);
                }
            }
        }
        public void sendTextMessage2(long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
            SendMessage message = new SendMessage();

            if (keyboardMarkup != null) {
                message.setReplyMarkup(keyboardMarkup);
            }

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        // Метод для отправки сообщения "Введите тикер акции"
        public void sendEnterStockTickerMessage(long chatId, Object o) {
            sendTextMessage2(chatId, "Введите тикер акции:", null);
        }

        private Map<Long, String> userStates = new HashMap<>();

        // Метод для отправки сообщения с ошибкой
        public void sendErrorMessage(long chatId, String errorMessage) {
            try {
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText(errorMessage);
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpdatesReceived(List<Update> updates) {
            super.onUpdatesReceived(updates);
        }

        // Переименованный метод для отправки сообщения с клавиатурой
        // испрвила на public для тестирования
        public void sendTextMessageWithTickerInput(long chatId, String message, ReplyKeyboardMarkup keyboardMarkup) throws TelegramApiException {
            SendMessage messageWithKeyboard = new SendMessage();
            messageWithKeyboard.setChatId(String.valueOf(chatId));
            messageWithKeyboard.setText(message);
            messageWithKeyboard.setReplyMarkup(keyboardMarkup);
            execute(messageWithKeyboard);
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
}
