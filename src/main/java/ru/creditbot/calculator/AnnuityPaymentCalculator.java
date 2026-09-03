package ru.creditbot.calculator;

import ru.creditbot.model.LoanRequest;
import ru.creditbot.model.Payment;

import java.util.ArrayList;
import java.util.List;

public class AnnuityPaymentCalculator implements PaymentCalculator {

    @Override
    public List<Payment> calculate(LoanRequest request) {

        List<Payment> payments = new ArrayList<>();

        double amount = request.getAmount();
        int months = request.getMonths();

        double monthlyRate =
                request.getAnnualInterestRate() / 100 / 12;

        double monthlyPayment;

        if (monthlyRate == 0) {
            monthlyPayment = amount / months;
        } else {
            monthlyPayment =
                    amount
                            * monthlyRate
                            * Math.pow(1 + monthlyRate, months)
                            / (Math.pow(1 + monthlyRate, months) - 1);
        }

        double remainingDebt = amount;

        for (int month = 1; month <= months; month++) {

            double interest = remainingDebt * monthlyRate;
            double principal = monthlyPayment - interest;

            remainingDebt -= principal;

            if (remainingDebt < 0) {
                remainingDebt = 0;
            }

            Payment payment = new Payment(
                    month,
                    monthlyPayment,
                    principal,
                    interest,
                    remainingDebt
            );

            payments.add(payment);
        }

        return payments;
    }
}