package ru.creditbot.calculator;

import ru.creditbot.model.PaymentType;

public class PaymentCalculatorFactory {

    public PaymentCalculator createCalculator(PaymentType paymentType) {

        if (paymentType == PaymentType.ANNUITY) {
            return new AnnuityPaymentCalculator();
        }

        if (paymentType == PaymentType.DIFFERENTIATED) {
            return new DifferentiatedPaymentCalculator();
        }

        throw new IllegalArgumentException(
                "Неизвестный тип платежа: " + paymentType
        );
    }
}