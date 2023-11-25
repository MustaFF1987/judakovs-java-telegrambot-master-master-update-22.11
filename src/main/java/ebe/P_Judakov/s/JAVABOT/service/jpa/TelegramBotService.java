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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class TelegramBotService extends TelegramLongPollingBot implements ebe.P_Judakov.s.JAVABOT.service.interfaces.TelegramBotService {

    /**
     * Репозиторий для управления подписанными каналами в базе данных.
     */
    @Qualifier("telegramBotService")
    private SubscribedChannelRepository subscribedChannelRepository;

    /**
     * Сервис для управления пользователями в базе данных.
     */
    private JpaUserService jpaUserService;

    /**
     * Переменная, используемая для хранения и управления клавиатурой ввода.
     */
    private ReplyKeyboardMarkup keyboardMarkup;

    /**
     * Сервис для работы с пользователями в системе.
     */
    private UserService userService;

    /**
     * Контроллер, обеспечивающий комбинированную логику в приложении.
     */
    private CombinedController combinedController;

    /**
     * Логгер для записи информационных сообщений и ошибок в приложении.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramBotService.class);

    /**
     * Состояние пользователей, хранящее текущее состояние каждого чата (пользователя) в виде ключ-значение.
     * Ключ - идентификатор чата (Long), Значение - состояние чата в виде строки.
     */
    private Map<Long, String> userState = new HashMap<>();

    /**
     * Обрабатывает обновления, полученные из Telegram API.
     * Если приходит сообщение с текстом, выполняет обработку команд или входящих сообщений пользователя.
     *
     * @param update Объект Update, содержащий информацию о полученном обновлении в Telegram.
     *               Update содержит данные о сообщениях, состояниях чатов и других событиях.
     *               Используется для анализа и обработки информации от пользователя.
     */
    @Override
    public void onUpdateReceived(Update update) {
        // Логируем приходящие обновления
        LOGGER.info("Received update: {}", update);

        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
//          Long chatId = update.getMessage().getChatId();

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

    /**
     * Вызывается при регистрации бота в Telegram.
     * Этот метод вызывается автоматически библиотекой TelegramBots
     * после успешной регистрации бота и позволяет выполнять дополнительные действия или настройки в момент регистрации.
     *
     * Переопределение данного метода позволяет выполнить дополнительные настройки бота после успешной регистрации
     * в Telegram API перед началом обработки входящих сообщений и обновлений.
     */
    @Override
    public void onRegister() {
        super.onRegister();
    }

    /**
     * Обрабатывает команды и текстовые сообщения, полученные в обновлении (Update).
     * В зависимости от содержания сообщения выполняет соответствующие действия, связанные с командами бота.
     *
     * @param update Объект Update, содержащий информацию о сообщении и обновлении в Telegram.
     *               Update используется для анализа и обработки входящих сообщений и команд от пользователя.
     * @throws TelegramApiException Исключение, которое может быть выброшено в процессе взаимодействия с API Telegram.
     */

    private boolean isStopped = false; // Флаг для отслеживания состояния остановки бота

    private boolean isSubscribed = false; // Флаг для отслеживания состояния подписки

    private void handleCommands(Update update) throws TelegramApiException {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if ("/start".equals(text)) {
            if (isStopped) {
                isStopped = false; // Если бот был остановлен, и пользователь нажал /start, сбрасываем флаг остановки
            }
            sendWelcomeMessage(chatId, keyboardMarkup);
        } else if ("/help".equals(text)) {
            String responseText = "Список доступных команд: /start, /help, /stop, /getStock";
            if (!isStopped) {
                if (isSubscribed) {
                    responseText += ", /unsubscribe";
                } else {
                    responseText += ", /subscribe";
                }
            }
            sendTextMessageWithKeyboard(chatId, responseText, keyboardMarkup);
        } else if ("/subscribe".equals(text) || "Subscribe".equals(text)) {
            isSubscribed = true; // Установка флага подписки
            if (!isStopped) {
                handleSubscribeSchedule(chatId);
            }
        } else if ("/unsubscribe".equals(text) || "Unsubscribe".equals(text)) {
            isSubscribed = false; // Сброс флага подписки
            if (!isStopped) {
                handleUnsubscribe(chatId);
            }
        } else if ("Daily".equals(text)) {
            if (!isStopped) {
                handleDailySubscription(chatId);
                removeButtonFromKeyboard("Daily");
                sendUpdatedKeyboard(chatId, "Weekly", "Monthly");
            }
        } else if ("Weekly".equals(text)) {
            if (!isStopped) {
                handleWeeklySubscription(chatId);
                removeButtonFromKeyboard("Weekly");
                sendUpdatedKeyboard(chatId, "Daily", "Monthly");
            }
        } else if ("Monthly".equals(text)) {
            if (!isStopped) {
                handleMonthlySubscription(chatId);
                removeButtonFromKeyboard("Monthly");
                sendUpdatedKeyboard(chatId, "Daily", "Weekly");
            }
        } else if (userState.containsKey(chatId) && userState.get(chatId).equals("AWAITING_STOCK_TICKER")) {
            if (!isStopped) {
                handleStockTickerInput(chatId, text);
                userState.remove(chatId);
            }
        } else if ("/getStock".equals(text)) {
            if (!isStopped) {
                userState.put(chatId, "AWAITING_STOCK_TICKER");
                sendTextMessageWithKeyboard(chatId, "Введите название тикера акции:", keyboardMarkup);
            }
        } else if ("/stop".equals(text)) {
            isStopped = true; // Устанавливаем флаг остановки после нажатия /stop
            sendTextMessageWithKeyboard(chatId, "Бот остановлен. Нажмите /start для возобновления работы.", keyboardMarkup);
        } else {
            if (!isStopped) {
                sendTextMessageWithKeyboard(chatId, "Введите корректную команду", keyboardMarkup);
            } else {
                sendTextMessageWithKeyboard(chatId, "Бот остановлен. Нажмите /start для возобновления работы.", keyboardMarkup);
            }
        }
    }



    // Метод для удаления кнопки из клавиатуры
    private void removeButtonFromKeyboard(String buttonText) {
        if (keyboardMarkup != null && keyboardMarkup.getKeyboard() != null) {
            List<KeyboardRow> keyboard = keyboardMarkup.getKeyboard();

            for (KeyboardRow row : keyboard) {
                row.remove(buttonText);
            }
        }
    }

        // Метод для отправки обновленной клавиатуры пользователю без нажатой кнопки
        private void sendUpdatedKeyboard(Long chatId, String... buttons) {
            if (keyboardMarkup != null) {
                ReplyKeyboardMarkup updatedKeyboard = new ReplyKeyboardMarkup();
                updatedKeyboard.setSelective(true);
                updatedKeyboard.setResizeKeyboard(true);
                updatedKeyboard.setOneTimeKeyboard(false);

                List<KeyboardRow> keyboard = new ArrayList<>();
                KeyboardRow row = new KeyboardRow();

                // Добавляем кнопки к новой клавиатуре
                for (String button : buttons) {
                    row.add(button);
                }

                keyboard.add(row);
                updatedKeyboard.setKeyboard(keyboard);

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Выберите новую подписку или нажмите /unsubscribe");
                message.setReplyMarkup(updatedKeyboard);

                try {
                    execute(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    private Map<Long, Boolean> subscriptionStatus = new HashMap<>();

    private void handleSubscribeSchedule(Long chatId) throws TelegramApiException {
        // Подписываем пользователя на уведомления
        SubscriptionManager.subscribe(chatId);

        // Формируем текст ответа и отправляем его с клавиатурой частоты уведомлений
        String responseText = "Вы подписались на уведомления от бота. Выберите частоту уведомлений:";
        ReplyKeyboardMarkup frequencyKeyboard = createFrequencyKeyboard();
        sendTextMessageWithKeyboard(chatId, responseText, frequencyKeyboard);
        // Сохраняем состояние подписки для данного чата
        subscriptionStatus.put(chatId, true);
        // Устанавливаем роль пользователя в системе
        jpaUserService.setUserRole(chatId, "USER");
        // Добавляем пустого бота в чат (возможно, для каких-то дополнительных действий)
        addEmptyBotToChat(chatId.toString());
    }

    private ReplyKeyboardMarkup createFrequencyKeyboard() {
        ReplyKeyboardMarkup frequencyKeyboard = new ReplyKeyboardMarkup();
        frequencyKeyboard.setSelective(true);
        frequencyKeyboard.setResizeKeyboard(true);
        frequencyKeyboard.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Daily");
        row.add("Weekly");
        row.add("Monthly");

        keyboard.add(row);
        frequencyKeyboard.setKeyboard(keyboard);

        return frequencyKeyboard;
    }

    private void handleDailySubscription(Long chatId) {
        // Обработка подписки на уведомления ежедневно
        // Вызов метода, который будет отправлять уведомления ежедневно
        // sendDailyNotifications(chatId);

        // Отправка сообщения подтверждения подписки
        String confirmationMessage = "Вы успешно подписались на ежедневные уведомления от бота.";
        try {
            sendTextMessage(chatId, confirmationMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void handleWeeklySubscription(Long chatId) {
        // Обработка подписки на уведомления еженедельно
        // Вызов метода, который будет отправлять уведомления ежедневно
        // sendDailyNotifications(chatId);

        // Отправка сообщения подтверждения подписки
        String confirmationMessage = "Вы успешно подписались на еженедельные уведомления от бота.";
        try {
            sendTextMessage(chatId, confirmationMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleMonthlySubscription(Long chatId) {
        // Обработка подписки на уведомления ежемесячно
        // Вызов метода, который будет отправлять уведомления ежедневно
        // sendDailyNotifications(chatId);

        // Отправка сообщения подтверждения подписки
        String confirmationMessage = "Вы успешно подписались на ежедневные уведомления от бота.";
        try {
            sendTextMessage(chatId, confirmationMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Обрабатывает введенный тикер акции после команды /getStock.
     *
     * @param chatId      Идентификатор чата пользователя.
     * @param stockTicker Тикер акции, введенный пользователем.
     */
    private void handleStockTickerInput(Long chatId, String stockTicker) {
        // Обработка введенного тикера акции
        String stockQuoteInfo = fetchStockQuoteInfo(stockTicker);

        if (stockQuoteInfo != null) {
            // Если удалось получить информацию о котировках, отправляем пользователю
            sendStockQuoteInfo(chatId, stockQuoteInfo, keyboardMarkup);
        } else {
            // Если возникла ошибка при получении информации о котировках
            try {
                sendTextMessageWithKeyboard(chatId, "Ошибка при получении информации о котировках", keyboardMarkup);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Выполняет запрос информации о котировках акции через API, используя указанный тикер акции.
     *
     * @param stockTicker Символ акции для получения котировок.
     *                    Это строковое значение, представляющее символ или код акции, для которой необходимо получить котировки.
     * @return Строка с информацией о котировках акции в формате JSON или null в случае возникновения ошибки при запросе.
     *         Возвращает JSON-строку с данными о котировках акции, если запрос выполнен успешно,
     *         или null в случае ошибки или некорректного выполнения запроса.
     */
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
                    .addQueryParameter("symbol", symbol);

            String url = urlBuilder.build().toString();

            // Формирование и отправка GET запроса
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("X-RapidAPI-Key", "4dfa492779msh47fb50b07bc7c09p11ff37jsn1c0c729af2c0") // Замените на свой ключ
                    .addHeader("X-RapidAPI-Host", "alpha-vantage.p.rapidapi.com")
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

    /**
     * Метод-заглушка для отправки информации о котировках акции пользователю.
     * Пример реализации метода отправки информации о котировках акции, который может быть изменен
     * для реальной отправки запроса к API для получения информации о котировках.
     *
     * @param chatId          Идентификатор чата пользователя, куда будет отправлена информация.
     * @param stockTicker     Символ (тикер) акции, для которой нужно отправить информацию о котировках.
     * @param keyboardMarkup  Объект клавиатуры, которая может быть привязана к сообщению (может быть null).
     *                        Используется для отправки сообщения с клавиатурой или без нее.
     */
    private void sendStockQuoteInfo(long chatId, String stockTicker, ReplyKeyboardMarkup keyboardMarkup) {
        try {
            // Ваш код для отправки запроса к API для получения информации о котировках акции
            // В данном примере, используется заглушка и вывод в консоль
            System.out.println("Запрос к API для получения информации о котировках акции с тикером " + stockTicker);

            // Проверка наличия актуальной клавиатуры
            if (keyboardMarkup != null) {
                // Отправка сообщения с использованием клавиатуры
                sendTextMessageWithKeyboard(chatId, "Информации о котировках акции " + stockTicker + " :", keyboardMarkup);
                System.out.println("Отправка информации о котировках акции " + stockTicker + " прошла успешно");
            } else {
                // Если клавиатура не установлена, отправляем сообщение без нее
                sendTextMessage(chatId, "Информации о котировках акции " + stockTicker + " :");
                System.out.println("Отправка информации о котировках акции " + stockTicker + " прошла успешно");
            }
        } catch (Exception e) {
            // Обработка ошибок при запросе к API или отправке сообщения
            e.printStackTrace();
        }
    }


    /**
     * Метод для отправки текстового сообщения пользователю без клавиатуры.
     *
     * @param chatId  Идентификатор чата пользователя, куда будет отправлено сообщение.
     * @param message Текст сообщения для отправки.
     */
    private void sendTextMessage(long chatId, String message) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(message);
            execute(sendMessage);
        } catch (Exception e) {
            // Обработка возможных ошибок при отправке сообщения
            e.printStackTrace();
        }
    }

    /**
     * Метод для отправки текстового сообщения пользователю с использованием клавиатуры.
     *
     * @param chatId         Идентификатор чата пользователя, куда будет отправлено сообщение.
     * @param message        Текст сообщения для отправки.
     * @param keyboardMarkup Объект ReplyKeyboardMarkup, представляющий клавиатуру для сообщения.
     */
    private void sendTextMessageWithKeyboard(long chatId, String message, ReplyKeyboardMarkup keyboardMarkup) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(message);
            sendMessage.setReplyMarkup(keyboardMarkup);
            execute(sendMessage);
        } catch (Exception e) {
            // Обработка возможных ошибок при отправке сообщения с клавиатурой
            e.printStackTrace();
        }
    }

    /**
     * Метод для обработки отписки пользователя от уведомлений от бота.
     *
     * @param chatId Идентификатор чата пользователя, который отписывается от уведомлений.
     * @throws TelegramApiException Исключение, которое может быть выброшено при работе с API Telegram.
     */
    private void handleUnsubscribe(Long chatId) throws TelegramApiException {
        // Отписываем пользователя от уведомлений
        SubscriptionManager.unsubscribe(chatId);

        // Формируем текст ответа и отправляем его с обновленной клавиатурой, включая кнопку подписки
        String responseText = "Вы отписались от уведомлений от бота, вы можете подписаться снова.";
        keyboardMarkup = updateKeyboardWithSubscribeButton(keyboardMarkup); // Обновляем клавиатуру с кнопкой подписки
        sendTextMessageWithKeyboard(chatId, responseText, keyboardMarkup);
    }


    /**
     * Создает новую клавиатуру с кнопками "Подписаться" и "Отписаться".
     *
     * @return Новая клавиатура с кнопками "Подписаться" и "Отписаться".
     */
    private ReplyKeyboardMarkup createKeyboardWithSubscribeButton() {
        // Создаем новую клавиатуру с кнопкой "Подписаться" и "Отписаться"
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        // Создаем строки и добавляем кнопки "Подписаться" и "Отписаться"
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Subscribe");
        row.add("Unsubscribe");
        keyboard.add(row);

        // Устанавливаем клавиатуру в разметке клавиатуры
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }


    /**
     * Обновляет текущую клавиатуру, добавляя кнопку "Подписаться", если она отсутствует.
     *
     * @param keyboardMarkup Текущая клавиатура, которую необходимо обновить.
     * @return Обновленная клавиатура с кнопкой "Подписаться".
     */
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

    /**
     * Удаляет кнопку "Подписаться" из клавиатуры.
     *
     * @param keyboardMarkup Текущая клавиатура, из которой нужно удалить кнопку "Подписаться".
     * @return Обновленная клавиатура без кнопки "Подписаться".
     */
    private ReplyKeyboardMarkup removeSubscribeButton(ReplyKeyboardMarkup keyboardMarkup) {
        List<KeyboardRow> keyboard = keyboardMarkup.getKeyboard();

        // Перебираем строки клавиатуры и удаляем кнопку "Subscribe"
        for (KeyboardRow row : keyboard) {
            row.removeIf(button -> "Subscribe".equals(button.getText()));
        }
        return keyboardMarkup;
    }

    /**
     * Создает клавиатуру с кнопками "Subscribe" и "Unsubscribe".
     *
     * @return Новая клавиатура с кнопками "Subscribe" и "Unsubscribe".
     */
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


    /**
     * Отправляет приветственное сообщение пользователю.
     *
     * @param chatId          Идентификатор чата с пользователем.
     * @param keyboardMarkup  Клавиатура, используемая при отправке сообщения.
     * @throws TelegramApiException В случае возникновения ошибки при отправке сообщения через Telegram API.
     */
    private void sendWelcomeMessage(Long chatId, ReplyKeyboardMarkup keyboardMarkup) throws TelegramApiException {
        String welcomeText = "Добро пожаловать! Я ваш телеграм-бот.";
        welcomeText += " Начните взаимодействие с командой /help.";
        sendTextMessageWithKeyboard(chatId, welcomeText, keyboardMarkup);
    }



    /**
     * Отправляет сообщение о завершении работы с ботом вместе с клавиатурой.
     *
     * @param chatId          Идентификатор чата с пользователем.
     * @param keyboardMarkup  Клавиатура, используемая при отправке сообщения.
     * @throws TelegramApiException В случае возникновения ошибки при отправке сообщения через Telegram API.
     */
    public void sendStopMessage(Long chatId, ReplyKeyboardMarkup keyboardMarkup) throws TelegramApiException {
        String stopText = "Вы завершили работу с ботом.";
        stopText += " До скорых встреч!";

        sendTextMessageWithKeyboard(chatId, stopText, keyboardMarkup);
    }


    /**
     * Возвращает имя бота, зарегистрированного в Telegram.
     *
     * @return Имя зарегистрированного в Telegram бота.
     */
    @Override
    public String getBotUsername() {
        return "PA_YU_Unikorpa_Telegram_Bot";
    }


    /**
     * Возвращает токен бота, полученный при регистрации в Telegram.
     *
     * @return Токен зарегистрированного в Telegram бота.
     */
    @Override
    public String getBotToken() {
        return "6669687693:AAGVbVs_AHL22m0w-7rjPW8_h_alNLV6jBo";
    }


    /**
     * Инициализирует бота, регистрируя его в Telegram.
     * Готовит бота к работе и выводит информацию о готовности к работе.
     */
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

    /**
     * Отправляет текстовое сообщение через Telegram API.
     *
     * @param message Сообщение, которое необходимо отправить.
     * @return Объект сообщения, содержащий информацию о сообщении, отправленном через API.
     */
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


    /**
     * Отправляет текстовое сообщение с клавиатурой через Telegram API.
     *
     * @param chatId          Идентификатор чата, куда отправляется сообщение.
     * @param text            Текст сообщения.
     * @param keyboardMarkup  Клавиатура, используемая при отправке сообщения.
     * @throws TelegramApiException В случае возникновения ошибки при отправке сообщения через Telegram API.
     */
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


    /**
     * Обрабатывает входящие сообщения Telegram.
     *
     * @param update Объект Update, содержащий информацию о входящем сообщении.
     * @throws TelegramApiException В случае возникновения ошибки при обработке входящего сообщения через Telegram API.
     */
    public void processIncomingMessage(Update update) throws TelegramApiException {

        // Проверяем, есть ли сообщение в обновлении
        if (update.hasMessage()) {
            Long chatId = update.getMessage().getChatId(); // Получаем идентификатор чата

            // Проверяем наличие клавиатуры
            if (keyboardMarkup != null) {
                Message message = update.getMessage();

                // Проверяем, содержит ли сообщение текст
                if (message.hasText()) {
                    String text = message.getText(); // Получаем текст сообщения

                    // Обработка текстовых сообщений
                    String responseText = "Ваше сообщение: " + text;

                    // Отправка ответа пользователю с клавиатурой
                    sendTextMessageWithKeyboard(chatId, responseText, keyboardMarkup);
                } else if (message.hasPhoto()) { // Проверяем, содержит ли сообщение фотографию
                    String caption = message.getCaption(); // Получаем подпись к фотографии

                    // Обработка сообщений с фотографиями
                    String responseText = "Вы отправили фотографию с подписью: " + caption;

                    // Отправка ответа пользователю с клавиатурой
                    sendTextMessageWithKeyboard(chatId, responseText, keyboardMarkup);
                }
            } else {
                // Действия, если клавиатура отсутствует
            }
        }
    }

    /**
     * Добавляет пустого бота в чат по идентификатору чата.
     *
     * @param chatId Идентификатор чата, в который добавляется пустой бот.
     * @throws RuntimeException В случае возникновения ошибки при регистрации пустого бота в чате.
     */
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

            // Добавление пустого бота в чат
            sendTextMessageWithKeyboard(Long.parseLong(chatId), "Пустой бот добавлен в чат.", keyboardMarkup);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            // Обработка ошибок при добавлении пустого бота
        }
    }

    /**
     * Вызывается при получении обновления от Telegram.
     *
     * @param update Объект, содержащий информацию об обновлении.
     */
    @Override
    public void onUpdateReceived(org.hibernate.sql.Update update) {
    }

    /**
     * Метод для обработки входящих сообщений.
     *
     * @param update Объект, содержащий информацию об обновлении.
     * @throws TelegramApiException В случае возникновения ошибки при обработке входящего сообщения.
     */
    @Override
    public void processIncomingMessage(org.hibernate.sql.Update update) throws TelegramApiException {
    }



    public static class StockQuoteBot extends TelegramLongPollingBot implements IStockQuoteBot {

        /**
         * Метод для обработки команды /getStock.
         * Организует процесс взаимодействия с пользователем: запрос ввода тикера,
         * ожидание ввода и обработка полученной информации о котировках акции для последующей
         * отправки пользователю.
         *
         * @param chatId         Идентификатор чата пользователя.
         * @param stockTicker    Тикер акции, который пользователь вводит.
         * @param keyboardMarkup Клавиатура для отправки сообщений с кнопками пользователю.
         * @param userState      Состояние пользователя, содержащее информацию о текущем запросе.
         */
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


        /**
         * Метод для получения ввода тикера акции от пользователя.
         * Запрашивает у пользователя ввод тикера акции с помощью консоли.
         *
         * @return Введенный пользователем тикер акции.
         */
        public String getUserInputTicker() {
            Scanner scanner = new Scanner(System.in); // Создаем объект Scanner для чтения с клавиатуры

            System.out.println("Введите тикер акции:"); // Получаем ввод от пользователя
            String userInput = scanner.nextLine();

            scanner.close(); // Закрываем Scanner
            return userInput; // Возвращаем введенное значение пользователя
        }

        /**
         * Метод для обработки тикера акции от пользователя к контроллеру.
         * Выполняет запрос информации о котировках по указанному тикеру.
         *
         * @param ticker Тикер акции, введенный пользователем.
         * @return Информация о котировках акции или сообщение об ошибке при выполнении запроса.
         */
        public String processUserTicker(String ticker) {
            try {
                return fetchStockQuoteInfo(ticker); // Вызов метода для получения информации о котировках по тикеру
            } catch (Exception e) {
                return "Ошибка при выполнении запроса"; // Обработка ошибок
            }
        }

        /**
         * Класс для управления сообщениями, отправляемыми ботом.
         */
        private JpaMessage lastSentMessage;

        /**
         * Метод для получения текста последнего отправленного сообщения.
         *
         * @return Текст последнего отправленного сообщения или null, если сообщение не было отправлено.
         */
        public String getLastMessageSent() {
            if (lastSentMessage != null) {
                return lastSentMessage.getText();
            } else {
                return null;
            }
        }

        /**
         * Метод для установки последнего отправленного сообщения.
         *
         * @param message Последнее отправленное сообщение для установки.
         */
        public void setLastSentMessage(JpaMessage message) {
            this.lastSentMessage = message;
        }

        /**
         * Метод для отправки текстовых сообщений с клавиатурой.
         *
         * @param chatId         Идентификатор чата, куда будет отправлено сообщение.
         * @param text           Текст сообщения.
         * @param keyboardMarkup Разметка клавиатуры.
         * @throws TelegramApiException Если возникла ошибка при отправке сообщения через Telegram API.
         */
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


        /**
         * Метод для отправки информации о котировках акции пользователю через кастомную клавиатуру.
         *
         * @param chatId         Идентификатор чата, куда будет отправлено сообщение.
         * @param stockQuoteInfo Информация о котировках акции в формате JSON.
         * @param keyboardMarkup Разметка клавиатуры.
         * @throws TelegramApiException Если возникла ошибка при отправке сообщения через Telegram API.
         */
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

            // Формирование сообщения с информацией о котировках для отправки пользователю
            String message = String.format(
                    "Symbol: %s\nOpen: %s\nHigh: %s\nLow: %s\nPrice: %s\nVolume: %s\nLatest Trading Day: %s\nPrevious Close: %s\nChange: %s\nChange Percent: %s",
                    symbol, open, high, low, price, volume, latestTradingDay, previousClose, change, changePercent
            );

            // Вывод информации о котировках в логи
            System.out.println(message);

            // Отправка сообщения пользователю с использованием кастомной клавиатуры
            sendCustomKeyboardMessage(chatId, message, keyboardMarkup);
        }


        /**
         * Метод для отправки сообщения с кастомной клавиатурой пользователю.
         *
         * @param chatId         Идентификатор чата, куда будет отправлено сообщение.
         * @param message        Текст сообщения для отправки.
         * @param keyboardMarkup Разметка клавиатуры.
         * @throws TelegramApiException Если возникла ошибка при отправке сообщения через Telegram API.
         */
        private void sendCustomKeyboardMessage(long chatId, String message, ReplyKeyboardMarkup keyboardMarkup) throws TelegramApiException {
            try {
                // Создаем объект для отправки сообщения
                SendMessage sendMessage = new SendMessage();

                // Устанавливаем идентификатор чата и текст сообщения
                sendMessage.setChatId(String.valueOf(chatId));
                sendMessage.setText(message);

                // Устанавливаем клавиатуру в сообщение
                sendMessage.setReplyMarkup(keyboardMarkup);

                // Отправляем сообщение и получаем информацию о нем
                Message sentMessage = execute(sendMessage);

                // Обработка успешной отправки сообщения
                System.out.println("Сообщение о котировках акции успешно отправлено: " + sentMessage);
            } catch (TelegramApiException e) {
                // Обработка ошибок при отправке сообщения
                e.printStackTrace();
            }
        }

        /**
         * Представляет объект разметки клавиатуры, используемый для отправки сообщений с клавиатурой в Telegram.
         * Этот объект содержит информацию о том, какая клавиатура будет отображаться при отправке сообщений ботом.
         */
        private ReplyKeyboardMarkup keyboardMarkup;

        /**
         * Обрабатывает входящие обновления от Telegram.
         * Осуществляет проверку текстовых сообщений и вызывает методы в зависимости от содержания сообщения.
         *
         * @param update Объект, содержащий данные об обновлении от Telegram.
         */
        public void onUpdateReceived(Update update) {
            Map<Long, String> userState = new HashMap<>(); // Создаем локальную переменную userState
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();

                if (text.equals("/getStock")) {
                    sendEnterStockTickerMessage(chatId, null);
                } else {
                    // Обработка тикера акции, если он был введен после команды /getStock
                    handleGetStockQuote(chatId, text, keyboardMarkup, userState);
                }
            }
        }

        /**
         * Отправляет текстовое сообщение с опциональной клавиатурой пользователю.
         *
         * @param chatId         Идентификатор чата, куда будет отправлено сообщение.
         * @param text           Текст сообщения для отправки.
         * @param keyboardMarkup Разметка клавиатуры (может быть null).
         */
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


        /**
         * Отправляет сообщение с предложением ввести тикер акции.
         *
         * @param chatId Идентификатор чата, куда будет отправлено сообщение.
         * @param o      Объект (может быть null).
         */
        public void sendEnterStockTickerMessage(long chatId, Object o) {
            sendTextMessage2(chatId, "Введите тикер акции:", null);
        }


        /**
         * Хранит состояния пользователей бота, связанные с идентификаторами чатов.
         * Ключи представляют идентификаторы чатов, а значения - состояния пользователей.
         * Используется для отслеживания состояний пользователей в процессе общения с ботом.
         */
        private Map<Long, String> userStates = new HashMap<>();


        /**
         * Отправляет сообщение с текстом ошибки.
         *
         * @param chatId       Идентификатор чата, куда будет отправлено сообщение.
         * @param errorMessage Текст ошибки для отправки.
         */
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


        /**
         * Переопределенный метод для обработки списка обновлений.
         * Этот метод вызывается при получении списка обновлений и обрабатывает их базово.
         *
         * @param updates Список обновлений, полученных ботом
         */
        @Override
        public void onUpdatesReceived(List<Update> updates) {
            super.onUpdatesReceived(updates);
        }

        /**
         * Отправляет текстовое сообщение с клавиатурой в указанный чат.
         *
         * @param chatId         Идентификатор чата, куда отправляется сообщение
         * @param message        Текстовое сообщение для отправки
         * @param keyboardMarkup Разметка клавиатуры для сообщения
         * @throws TelegramApiException Исключение, которое может возникнуть при отправке сообщения
         */
        public void sendTextMessageWithTickerInput(long chatId, String message, ReplyKeyboardMarkup keyboardMarkup) throws TelegramApiException {
            SendMessage messageWithKeyboard = new SendMessage();
            messageWithKeyboard.setChatId(String.valueOf(chatId));
            messageWithKeyboard.setText(message);
            messageWithKeyboard.setReplyMarkup(keyboardMarkup);
            execute(messageWithKeyboard);
        }

        /**
         * Возвращает имя бота.
         *
         * @return Имя бота, если оно было установлено, иначе null
         */
        @Override
        public String getBotUsername() {
            return null;
        }

        /**
         * Вызывается при регистрации бота в Telegram.
         * Этот метод вызывается автоматически после успешной регистрации бота и может быть использован для дополнительных действий или настроек.
         */
        @Override
        public void onRegister() {
            super.onRegister();
        }
    }
}
