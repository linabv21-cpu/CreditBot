package ru.creditbot.calculator;

import ru.creditbot.model.LoanRequest;
import ru.creditbot.model.Payment;

import java.util.ArrayList;
import java.util.List;

public class DifferentiatedPaymentCalculator implements PaymentCalculator {

    @Override
    public List<Payment> calculate(LoanRequest request) {

        List<Payment> payments = new ArrayList<>();

        double amount = request.getAmount();
        int months = request.getMonths();

        double monthlyRate =
                request.getAnnualInterestRate() / 100 / 12;

        double principalPart = amount / months;

        double remainingDebt = amount;

        for (int month = 1; month <= months; month++) {

            double interest = remainingDebt * monthlyRate;

            double paymentAmount = principalPart + interest;

            remainingDebt -= principalPart;

            if (remainingDebt < 0) {
                remainingDebt = 0;
            }

            Payment payment = new Payment(
                    month,
                    paymentAmount,
                    principalPart,
                    interest,
                    remainingDebt
            );

            payments.add(payment);
        }

        return payments;
    }
}