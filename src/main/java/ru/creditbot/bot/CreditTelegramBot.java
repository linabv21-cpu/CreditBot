package ru.creditbot.bot;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import ru.creditbot.calculator.PaymentCalculatorFactory;
import ru.creditbot.model.LoanRequest;
import ru.creditbot.model.Payment;
import ru.creditbot.model.PaymentType;
import ru.creditbot.repository.InMemoryLoanRequestRepository;
import ru.creditbot.repository.LoanRequestRepository;
import ru.creditbot.service.AnalyticsService;
import ru.creditbot.service.LoanService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreditTelegramBot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;

    private final Map<Long, UserSession> userSessions = new HashMap<>();

    private final Set<Long> authorizedManagers = new HashSet<>();

    private final LoanRequestRepository repository;
    private final LoanService loanService;
    private final AnalyticsService analyticsService;

    private final String managerLogin;
    private final String managerPassword;

    public CreditTelegramBot(String botToken) {

        telegramClient = new OkHttpTelegramClient(botToken);

        PaymentCalculatorFactory factory =
                new PaymentCalculatorFactory();

        repository =
                new InMemoryLoanRequestRepository();

        loanService =
                new LoanService(factory, repository);

        analyticsService =
                new AnalyticsService(repository);

        managerLogin =
                System.getenv("MANAGER_LOGIN");

        managerPassword =
                System.getenv("MANAGER_PASSWORD");
    }

    @Override
    public void consume(Update update) {

        if (!update.hasMessage() ||
                !update.getMessage().hasText()) {

            return;
        }

        String text =
                update.getMessage().getText().trim();

        long chatId =
                update.getMessage().getChatId();

        UserSession currentSession =
                userSessions.get(chatId);

        if (currentSession == null ||
                currentSession.getState() !=
                        UserState.WAITING_FOR_MANAGER_PASSWORD) {

            System.out.println(
                    "Получено сообщение: " + text
            );
        }

        // =========================
        // /start
        // =========================

        if (text.equals("/start")) {

            String startMessage =
                    "👋 Добро пожаловать в кредитный калькулятор!\n\n" +
                            "Доступные команды:\n" +
                            "/calculate — рассчитать кредит\n" +
                            "/history — история расчётов\n" +
                            "/manager — вход для менеджера";

            sendMessage(
                    chatId,
                    startMessage
            );

            return;
        }

        // =========================
        // /manager
        // =========================

        if (text.equals("/manager")) {

            if (authorizedManagers.contains(chatId)) {

                sendManagerMenu(chatId);
                return;
            }

            UserSession session =
                    new UserSession();

            session.setState(
                    UserState.WAITING_FOR_MANAGER_LOGIN
            );

            userSessions.put(
                    chatId,
                    session
            );

            sendMessage(
                    chatId,
                    "🔐 Введите логин менеджера:"
            );

            return;
        }

        // =========================
        // /analytics
        // =========================

        if (text.equals("/analytics")) {

            if (!authorizedManagers.contains(chatId)) {

                sendMessage(
                        chatId,
                        "⛔ Эта команда доступна только менеджеру.\n" +
                                "Сначала выполните вход: /manager"
                );

                return;
            }

            sendManagerMenu(chatId);
            return;
        }

        // =========================
        // /calculate
        // =========================

        if (text.equals("/calculate")) {

            UserSession session =
                    new UserSession();

            session.setState(
                    UserState.WAITING_FOR_AMOUNT
            );

            userSessions.put(
                    chatId,
                    session
            );

            sendMessage(
                    chatId,
                    "💰 Введите сумму кредита в рублях:"
            );

            return;
        }

        // =========================
        // /history
        // =========================

        if (text.equals("/history")) {

            List<LoanRequest> history =
                    loanService.getUserHistory(chatId);

            if (history.isEmpty()) {

                sendMessage(
                        chatId,
                        "📭 История расчётов пока пуста.\n" +
                                "Используйте /calculate, чтобы сделать первый расчёт."
                );

            } else {

                sendLongMessage(
                        chatId,
                        createHistoryMessage(history)
                );
            }

            return;
        }

        // =========================
        // КНОПКИ МЕНЕДЖЕРА
        // =========================

        if (authorizedManagers.contains(chatId)) {

            if (text.equals("📊 Общая аналитика")) {

                sendGeneralAnalytics(chatId);
                return;
            }

            if (text.equals("💰 Фильтр по сумме")) {

                UserSession session =
                        new UserSession();

                session.setState(
                        UserState.WAITING_FOR_FILTER_MIN_AMOUNT
                );

                userSessions.put(
                        chatId,
                        session
                );

                sendMessage(
                        chatId,
                        "💰 Введите минимальную сумму кредита:"
                );

                return;
            }

            if (text.equals("💳 Фильтр по типу")) {

                UserSession session =
                        new UserSession();

                session.setState(
                        UserState.WAITING_FOR_FILTER_PAYMENT_TYPE
                );

                userSessions.put(
                        chatId,
                        session
                );

                sendManagerPaymentTypeKeyboard(chatId);

                return;
            }

            if (text.equals("⭐ Популярные параметры")) {

                sendPopularParameters(chatId);
                return;
            }

            if (text.equals("🚪 Выйти")) {

                authorizedManagers.remove(chatId);
                userSessions.remove(chatId);

                sendMessageWithoutKeyboard(
                        chatId,
                        "🚪 Вы вышли из режима менеджера."
                );

                return;
            }
        }

        UserSession session =
                userSessions.get(chatId);

        // =========================
        // ЛОГИН МЕНЕДЖЕРА
        // =========================

        if (session != null &&
                session.getState() ==
                        UserState.WAITING_FOR_MANAGER_LOGIN) {

            session.setManagerLogin(text);

            session.setState(
                    UserState.WAITING_FOR_MANAGER_PASSWORD
            );

            sendMessage(
                    chatId,
                    "🔑 Введите пароль менеджера:"
            );

            return;
        }

        // =========================
        // ПАРОЛЬ МЕНЕДЖЕРА
        // =========================

        if (session != null &&
                session.getState() ==
                        UserState.WAITING_FOR_MANAGER_PASSWORD) {

            if (managerLogin != null &&
                    managerPassword != null &&
                    session.getManagerLogin()
                            .equals(managerLogin) &&
                    text.equals(managerPassword)) {

                authorizedManagers.add(chatId);

                userSessions.remove(chatId);

                sendManagerMenu(chatId);

            } else {

                userSessions.remove(chatId);

                sendMessage(
                        chatId,
                        "❌ Неверный логин или пароль.\n" +
                                "Попробуйте снова: /manager"
                );
            }

            return;
        }

        // =========================
        // ФИЛЬТР:
        // МИНИМАЛЬНАЯ СУММА
        // =========================

        if (session != null &&
                session.getState() ==
                        UserState.WAITING_FOR_FILTER_MIN_AMOUNT) {

            try {

                double minAmount =
                        Double.parseDouble(text);

                if (minAmount < 0) {

                    sendMessage(
                            chatId,
                            "❌ Сумма не может быть отрицательной.\n" +
                                    "Введите минимальную сумму ещё раз:"
                    );

                    return;
                }

                session.setFilterMinAmount(
                        minAmount
                );

                session.setState(
                        UserState.WAITING_FOR_FILTER_MAX_AMOUNT
                );

                sendMessage(
                        chatId,
                        "💰 Теперь введите максимальную сумму кредита:"
                );

            } catch (NumberFormatException e) {

                sendMessage(
                        chatId,
                        "❌ Нужно ввести число.\n" +
                                "Например: 100000"
                );
            }

            return;
        }

        // =========================
        // ФИЛЬТР:
        // МАКСИМАЛЬНАЯ СУММА
        // =========================

        if (session != null &&
                session.getState() ==
                        UserState.WAITING_FOR_FILTER_MAX_AMOUNT) {

            try {

                double maxAmount =
                        Double.parseDouble(text);

                double minAmount =
                        session.getFilterMinAmount();

                if (maxAmount < minAmount) {

                    sendMessage(
                            chatId,
                            "❌ Максимальная сумма не может быть меньше минимальной.\n" +
                                    "Введите максимальную сумму ещё раз:"
                    );

                    return;
                }

                List<LoanRequest> requests =
                        analyticsService.filterByAmount(
                                minAmount,
                                maxAmount
                        );

                userSessions.remove(chatId);

                if (requests.isEmpty()) {

                    sendMessage(
                            chatId,
                            "📭 Заявок в диапазоне от " +
                                    String.format("%.2f", minAmount) +
                                    " ₽ до " +
                                    String.format("%.2f", maxAmount) +
                                    " ₽ не найдено."
                    );

                } else {

                    sendLongMessage(
                            chatId,
                            createFilteredRequestsMessage(
                                    "💰 Результат фильтра по сумме",
                                    requests
                            )
                    );
                }

                sendManagerMenu(chatId);

            } catch (NumberFormatException e) {

                sendMessage(
                        chatId,
                        "❌ Нужно ввести число.\n" +
                                "Например: 500000"
                );
            }

            return;
        }

        // =========================
        // ФИЛЬТР:
        // ТИП ПЛАТЕЖА
        // =========================

        if (session != null &&
                session.getState() ==
                        UserState.WAITING_FOR_FILTER_PAYMENT_TYPE) {

            PaymentType paymentType;

            if (text.equals("💳 Аннуитетный")) {

                paymentType =
                        PaymentType.ANNUITY;

            } else if (text.equals(
                    "📉 Дифференцированный")) {

                paymentType =
                        PaymentType.DIFFERENTIATED;

            } else {

                sendManagerPaymentTypeKeyboard(chatId);
                return;
            }

            List<LoanRequest> requests =
                    analyticsService.filterByPaymentType(
                            paymentType
                    );

            userSessions.remove(chatId);

            if (requests.isEmpty()) {

                sendMessage(
                        chatId,
                        "📭 Расчётов с таким типом платежа пока нет."
                );

            } else {

                String title;

                if (paymentType ==
                        PaymentType.ANNUITY) {

                    title =
                            "💳 Аннуитетные расчёты";

                } else {

                    title =
                            "📉 Дифференцированные расчёты";
                }

                sendLongMessage(
                        chatId,
                        createFilteredRequestsMessage(
                                title,
                                requests
                        )
                );
            }

            sendManagerMenu(chatId);

            return;
        }

        // =========================
        // СУММА КРЕДИТА
        // =========================

        if (session != null &&
                session.getState() ==
                        UserState.WAITING_FOR_AMOUNT) {

            try {

                double amount =
                        Double.parseDouble(text);

                if (amount <= 0) {

                    sendMessage(
                            chatId,
                            "❌ Сумма должна быть больше 0.\n" +
                                    "Введите сумму кредита ещё раз:"
                    );

                    return;
                }

                session.setAmount(amount);

                session.setState(
                        UserState.WAITING_FOR_MONTHS
                );

                sendMessage(
                        chatId,
                        "📅 Введите срок кредита в месяцах:"
                );

            } catch (NumberFormatException e) {

                sendMessage(
                        chatId,
                        "❌ Нужно ввести число.\n" +
                                "Например: 500000"
                );
            }

            return;
        }

        // =========================
        // СРОК КРЕДИТА
        // =========================

        if (session != null &&
                session.getState() ==
                        UserState.WAITING_FOR_MONTHS) {

            try {

                int months =
                        Integer.parseInt(text);

                if (months <= 0) {

                    sendMessage(
                            chatId,
                            "❌ Срок должен быть больше 0.\n" +
                                    "Введите срок кредита ещё раз:"
                    );

                    return;
                }

                session.setMonths(months);

                session.setState(
                        UserState.WAITING_FOR_INTEREST_RATE
                );

                sendMessage(
                        chatId,
                        "📈 Введите годовую процентную ставку:"
                );

            } catch (NumberFormatException e) {

                sendMessage(
                        chatId,
                        "❌ Срок нужно указать целым числом.\n" +
                                "Например: 12"
                );
            }

            return;
        }

        // =========================
        // ПРОЦЕНТНАЯ СТАВКА
        // =========================

        if (session != null &&
                session.getState() ==
                        UserState.WAITING_FOR_INTEREST_RATE) {

            try {

                double interestRate =
                        Double.parseDouble(text);

                if (interestRate < 0) {

                    sendMessage(
                            chatId,
                            "❌ Процентная ставка не может быть отрицательной.\n" +
                                    "Введите ставку ещё раз:"
                    );

                    return;
                }

                session.setAnnualInterestRate(
                        interestRate
                );

                session.setState(
                        UserState.WAITING_FOR_PAYMENT_TYPE
                );

                sendPaymentTypeKeyboard(chatId);

            } catch (NumberFormatException e) {

                sendMessage(
                        chatId,
                        "❌ Процентную ставку нужно указать числом.\n" +
                                "Например: 15"
                );
            }

            return;
        }

        // =========================
        // ТИП ПЛАТЕЖА КРЕДИТА
        // =========================

        if (session != null &&
                session.getState() ==
                        UserState.WAITING_FOR_PAYMENT_TYPE) {

            PaymentType paymentType;

            if (text.equals("💳 Аннуитетный")) {

                paymentType =
                        PaymentType.ANNUITY;

            } else if (text.equals(
                    "📉 Дифференцированный")) {

                paymentType =
                        PaymentType.DIFFERENTIATED;

            } else {

                sendPaymentTypeKeyboard(chatId);
                return;
            }

            session.setPaymentType(
                    paymentType
            );

            LoanRequest request =
                    new LoanRequest(
                            chatId,
                            session.getAmount(),
                            session.getMonths(),
                            session.getAnnualInterestRate(),
                            session.getPaymentType()
                    );

            try {

                List<Payment> payments =
                        loanService.calculateLoan(
                                request
                        );

                String result =
                        createPaymentSchedule(
                                request,
                                payments
                        );

                sendLongMessageWithoutKeyboard(
                        chatId,
                        result
                );

                userSessions.remove(chatId);

            } catch (IllegalArgumentException e) {

                sendMessage(
                        chatId,
                        "❌ Ошибка расчёта: " +
                                e.getMessage()
                );
            }

            return;
        }

        sendMessage(
                chatId,
                "Я пока не знаю такую команду.\n" +
                        "Напиши /start, чтобы посмотреть доступные команды."
        );
    }

    // =========================================================
    // МЕНЮ МЕНЕДЖЕРА
    // =========================================================

    private void sendManagerMenu(long chatId) {

        List<KeyboardRow> keyboard =
                new ArrayList<>();

        KeyboardRow firstRow =
                new KeyboardRow();

        firstRow.add("📊 Общая аналитика");
        firstRow.add("⭐ Популярные параметры");

        KeyboardRow secondRow =
                new KeyboardRow();

        secondRow.add("💰 Фильтр по сумме");
        secondRow.add("💳 Фильтр по типу");

        KeyboardRow thirdRow =
                new KeyboardRow();

        thirdRow.add("🚪 Выйти");

        keyboard.add(firstRow);
        keyboard.add(secondRow);
        keyboard.add(thirdRow);

        ReplyKeyboardMarkup keyboardMarkup =
                ReplyKeyboardMarkup.builder()
                        .keyboard(keyboard)
                        .resizeKeyboard(true)
                        .selective(false)
                        .build();

        SendMessage message =
                SendMessage.builder()
                        .chatId(chatId)
                        .text(
                                "✅ Режим менеджера\n\n" +
                                        "Выберите действие:"
                        )
                        .replyMarkup(keyboardMarkup)
                        .build();

        executeMessage(message);
    }

    // =========================================================
    // ОБЩАЯ АНАЛИТИКА
    // =========================================================

    private void sendGeneralAnalytics(long chatId) {

        int totalRequests =
                analyticsService.getTotalRequests();

        long annuityCount =
                analyticsService.getAnnuityRequestsCount();

        long differentiatedCount =
                analyticsService.getDifferentiatedRequestsCount();

        PaymentType mostPopular =
                analyticsService.getMostPopularPaymentType();

        String popularType;

        if (mostPopular == null) {

            popularType =
                    "Нет данных";

        } else if (mostPopular ==
                PaymentType.ANNUITY) {

            popularType =
                    "Аннуитетный";

        } else {

            popularType =
                    "Дифференцированный";
        }

        String result =
                "📊 Общая аналитика\n\n" +
                        "📋 Всего запросов: " +
                        totalRequests + "\n" +
                        "💳 Аннуитетных: " +
                        annuityCount + "\n" +
                        "📉 Дифференцированных: " +
                        differentiatedCount + "\n" +
                        "⭐ Популярный тип платежа: " +
                        popularType;

        sendMessage(
                chatId,
                result
        );
    }

    // =========================================================
    // ПОПУЛЯРНЫЕ ПАРАМЕТРЫ
    // =========================================================

    private void sendPopularParameters(long chatId) {

        if (analyticsService.getTotalRequests() == 0) {

            sendMessage(
                    chatId,
                    "📭 Пока недостаточно данных для аналитики."
            );

            return;
        }

        int popularMonths =
                analyticsService.getMostPopularMonths();

        double popularRate =
                analyticsService.getMostPopularInterestRate();

        PaymentType popularPaymentType =
                analyticsService.getMostPopularPaymentType();

        String paymentTypeText;

        if (popularPaymentType ==
                PaymentType.ANNUITY) {

            paymentTypeText =
                    "Аннуитетный";

        } else {

            paymentTypeText =
                    "Дифференцированный";
        }

        String result =
                "⭐ Популярные параметры\n\n" +
                        "📅 Популярный срок: " +
                        popularMonths +
                        " мес.\n" +
                        "📈 Популярная ставка: " +
                        String.format("%.2f", popularRate) +
                        "%\n" +
                        "💳 Популярный тип платежа: " +
                        paymentTypeText;

        sendMessage(
                chatId,
                result
        );
    }

    // =========================================================
    // КЛАВИАТУРА ТИПА ПЛАТЕЖА
    // =========================================================

    private void sendPaymentTypeKeyboard(long chatId) {

        List<KeyboardRow> keyboard =
                new ArrayList<>();

        KeyboardRow row =
                new KeyboardRow();

        row.add("💳 Аннуитетный");
        row.add("📉 Дифференцированный");

        keyboard.add(row);

        ReplyKeyboardMarkup keyboardMarkup =
                ReplyKeyboardMarkup.builder()
                        .keyboard(keyboard)
                        .resizeKeyboard(true)
                        .oneTimeKeyboard(true)
                        .selective(false)
                        .build();

        SendMessage message =
                SendMessage.builder()
                        .chatId(chatId)
                        .text("💳 Выберите тип платежа:")
                        .replyMarkup(keyboardMarkup)
                        .build();

        executeMessage(message);
    }

    // =========================================================
    // КЛАВИАТУРА ФИЛЬТРА ТИПА
    // =========================================================

    private void sendManagerPaymentTypeKeyboard(
            long chatId) {

        List<KeyboardRow> keyboard =
                new ArrayList<>();

        KeyboardRow row =
                new KeyboardRow();

        row.add("💳 Аннуитетный");
        row.add("📉 Дифференцированный");

        keyboard.add(row);

        ReplyKeyboardMarkup keyboardMarkup =
                ReplyKeyboardMarkup.builder()
                        .keyboard(keyboard)
                        .resizeKeyboard(true)
                        .oneTimeKeyboard(true)
                        .selective(false)
                        .build();

        SendMessage message =
                SendMessage.builder()
                        .chatId(chatId)
                        .text(
                                "💳 Выберите тип платежа для фильтра:"
                        )
                        .replyMarkup(keyboardMarkup)
                        .build();

        executeMessage(message);
    }

    // =========================================================
    // ГРАФИК ПЛАТЕЖЕЙ
    // =========================================================

    private String createPaymentSchedule(
            LoanRequest request,
            List<Payment> payments) {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "✅ Расчёт готов!\n\n"
        );

        result.append("💰 Сумма: ")
                .append(
                        String.format(
                                "%.2f",
                                request.getAmount()
                        )
                )
                .append(" ₽\n");

        result.append("📅 Срок: ")
                .append(request.getMonths())
                .append(" мес.\n");

        result.append("📈 Ставка: ")
                .append(
                        String.format(
                                "%.2f",
                                request.getAnnualInterestRate()
                        )
                )
                .append("%\n");

        result.append("💳 Тип платежа: ");

        if (request.getPaymentType() ==
                PaymentType.ANNUITY) {

            result.append("Аннуитетный");

        } else {

            result.append(
                    "Дифференцированный"
            );
        }

        result.append(
                "\n\n📋 График платежей:\n\n"
        );

        for (Payment payment : payments) {

            result.append("Месяц ")
                    .append(payment.getMonth())
                    .append("\n");

            result.append("Платёж: ")
                    .append(
                            String.format(
                                    "%.2f",
                                    payment.getPaymentAmount()
                            )
                    )
                    .append(" ₽\n");

            result.append("Основной долг: ")
                    .append(
                            String.format(
                                    "%.2f",
                                    payment.getPrincipal()
                            )
                    )
                    .append(" ₽\n");

            result.append("Проценты: ")
                    .append(
                            String.format(
                                    "%.2f",
                                    payment.getInterest()
                            )
                    )
                    .append(" ₽\n");

            result.append("Остаток: ")
                    .append(
                            String.format(
                                    "%.2f",
                                    payment.getRemainingDebt()
                            )
                    )
                    .append(" ₽\n\n");
        }

        return result.toString();
    }

    // =========================================================
    // ИСТОРИЯ
    // =========================================================

    private String createHistoryMessage(
            List<LoanRequest> history) {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "📚 История ваших расчётов:\n\n"
        );

        int number = 1;

        for (LoanRequest request : history) {

            result.append(number)
                    .append(". Расчёт\n");

            result.append("💰 Сумма: ")
                    .append(
                            String.format(
                                    "%.2f",
                                    request.getAmount()
                            )
                    )
                    .append(" ₽\n");

            result.append("📅 Срок: ")
                    .append(request.getMonths())
                    .append(" мес.\n");

            result.append("📈 Ставка: ")
                    .append(
                            String.format(
                                    "%.2f",
                                    request.getAnnualInterestRate()
                            )
                    )
                    .append("%\n");

            result.append("💳 Тип: ");

            if (request.getPaymentType() ==
                    PaymentType.ANNUITY) {

                result.append("Аннуитетный");

            } else {

                result.append(
                        "Дифференцированный"
                );
            }

            result.append("\n\n");

            number++;
        }

        return result.toString();
    }

    // =========================================================
    // РЕЗУЛЬТАТ ФИЛЬТРА
    // =========================================================

    private String createFilteredRequestsMessage(
            String title,
            List<LoanRequest> requests) {

        StringBuilder result =
                new StringBuilder();

        result.append(title)
                .append("\n\n");

        result.append("Найдено запросов: ")
                .append(requests.size())
                .append("\n\n");

        int number = 1;

        for (LoanRequest request : requests) {

            result.append(number)
                    .append(". ");

            result.append(
                    String.format(
                            "%.2f",
                            request.getAmount()
                    )
            );

            result.append(" ₽ | ");

            result.append(
                    request.getMonths()
            );

            result.append(" мес. | ");

            result.append(
                    String.format(
                            "%.2f",
                            request.getAnnualInterestRate()
                    )
            );

            result.append("% | ");

            if (request.getPaymentType() ==
                    PaymentType.ANNUITY) {

                result.append("Аннуитетный");

            } else {

                result.append(
                        "Дифференцированный"
                );
            }

            result.append("\n");

            number++;
        }

        return result.toString();
    }

    // =========================================================
    // ОБЫЧНОЕ СООБЩЕНИЕ
    // =========================================================

    private void sendMessage(
            long chatId,
            String text) {

        SendMessage message =
                SendMessage.builder()
                        .chatId(chatId)
                        .text(text)
                        .build();

        executeMessage(message);
    }

    // =========================================================
    // ДЛИННОЕ СООБЩЕНИЕ
    // =========================================================

    private void sendLongMessage(
            long chatId,
            String text) {

        int maxLength = 4000;

        for (int start = 0;
             start < text.length();
             start += maxLength) {

            int end =
                    Math.min(
                            start + maxLength,
                            text.length()
                    );

            String part =
                    text.substring(
                            start,
                            end
                    );

            sendMessage(
                    chatId,
                    part
            );
        }
    }

    // =========================================================
    // СООБЩЕНИЕ + УБРАТЬ КЛАВИАТУРУ
    // =========================================================

    private void sendMessageWithoutKeyboard(
            long chatId,
            String text) {

        ReplyKeyboardRemove keyboardRemove =
                ReplyKeyboardRemove.builder()
                        .removeKeyboard(true)
                        .build();

        SendMessage message =
                SendMessage.builder()
                        .chatId(chatId)
                        .text(text)
                        .replyMarkup(keyboardRemove)
                        .build();

        executeMessage(message);
    }

    // =========================================================
    // ДЛИННОЕ СООБЩЕНИЕ + УБРАТЬ КЛАВИАТУРУ
    // =========================================================

    private void sendLongMessageWithoutKeyboard(
            long chatId,
            String text) {

        ReplyKeyboardRemove keyboardRemove =
                ReplyKeyboardRemove.builder()
                        .removeKeyboard(true)
                        .build();

        int maxLength = 4000;

        boolean firstMessage = true;

        for (int start = 0;
             start < text.length();
             start += maxLength) {

            int end =
                    Math.min(
                            start + maxLength,
                            text.length()
                    );

            String part =
                    text.substring(
                            start,
                            end
                    );

            SendMessage.SendMessageBuilder builder =
                    SendMessage.builder()
                            .chatId(chatId)
                            .text(part);

            if (firstMessage) {

                builder.replyMarkup(
                        keyboardRemove
                );

                firstMessage = false;
            }

            executeMessage(
                    builder.build()
            );
        }
    }

    // =========================================================
    // ОТПРАВКА TELEGRAM
    // =========================================================

    private void executeMessage(
            SendMessage message) {

        try {

            telegramClient.execute(message);

        } catch (TelegramApiException e) {

            e.printStackTrace();
        }
    }
}