package ru.creditbot.service;

import ru.creditbot.calculator.PaymentCalculator;
import ru.creditbot.calculator.PaymentCalculatorFactory;
import ru.creditbot.model.LoanRequest;
import ru.creditbot.model.Payment;
import ru.creditbot.repository.LoanRequestRepository;

import java.util.List;

public class LoanService {

    private final PaymentCalculatorFactory calculatorFactory;
    private final LoanRequestRepository repository;

    public LoanService(PaymentCalculatorFactory calculatorFactory,
                       LoanRequestRepository repository) {

        this.calculatorFactory = calculatorFactory;
        this.repository = repository;
    }

    public List<Payment> calculateLoan(LoanRequest request) {

        validate(request);

        PaymentCalculator calculator =
                calculatorFactory.createCalculator(
                        request.getPaymentType()
                );

        List<Payment> payments =
                calculator.calculate(request);

        repository.save(request);

        return payments;
    }

    public List<LoanRequest> getUserHistory(long userId) {
        return repository.findByUserId(userId);
    }

    private void validate(LoanRequest request) {

        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException(
                    "Сумма кредита должна быть больше 0"
            );
        }

        if (request.getMonths() <= 0) {
            throw new IllegalArgumentException(
                    "Срок кредита должен быть больше 0"
            );
        }

        if (request.getAnnualInterestRate() < 0) {
            throw new IllegalArgumentException(
                    "Процентная ставка не может быть отрицательной"
            );
        }
    }
}