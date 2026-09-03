package ru.creditbot.model;

public class LoanRequest {

    private final long userId;
    private final double amount;
    private final int months;
    private final double annualInterestRate;
    private final PaymentType paymentType;

    public LoanRequest(long userId,
                       double amount,
                       int months,
                       double annualInterestRate,
                       PaymentType paymentType) {

        this.userId = userId;
        this.amount = amount;
        this.months = months;
        this.annualInterestRate = annualInterestRate;
        this.paymentType = paymentType;
    }

    public long getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    public int getMonths() {
        return months;
    }

    public double getAnnualInterestRate() {
        return annualInterestRate;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }
}