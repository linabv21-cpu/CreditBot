package ru.creditbot.model;

public class Payment {

    private final int month;
    private final double paymentAmount;
    private final double principal;
    private final double interest;
    private final double remainingDebt;

    public Payment(int month,
                   double paymentAmount,
                   double principal,
                   double interest,
                   double remainingDebt) {

        this.month = month;
        this.paymentAmount = paymentAmount;
        this.principal = principal;
        this.interest = interest;
        this.remainingDebt = remainingDebt;
    }

    public int getMonth() {
        return month;
    }

    public double getPaymentAmount() {
        return paymentAmount;
    }

    public double getPrincipal() {
        return principal;
    }

    public double getInterest() {
        return interest;
    }

    public double getRemainingDebt() {
        return remainingDebt;
    }
}