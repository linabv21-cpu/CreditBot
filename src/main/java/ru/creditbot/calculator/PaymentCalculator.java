package ru.creditbot.calculator;

import ru.creditbot.model.LoanRequest;
import ru.creditbot.model.Payment;

import java.util.List;

public interface PaymentCalculator {

    List<Payment> calculate(LoanRequest request);
}