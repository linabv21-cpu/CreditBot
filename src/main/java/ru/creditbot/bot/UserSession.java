package ru.creditbot.bot;

import ru.creditbot.model.PaymentType;

public class UserSession {

    private UserState state = UserState.NONE;

    private double amount;
    private int months;
    private double annualInterestRate;
    private PaymentType paymentType;

    private String managerLogin;

    private double filterMinAmount;

    public UserState getState() {
        return state;
    }

    public void setState(UserState state) {
        this.state = state;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getMonths() {
        return months;
    }

    public void setMonths(int months) {
        this.months = months;
    }

    public double getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(double annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public String getManagerLogin() {
        return managerLogin;
    }

    public void setManagerLogin(String managerLogin) {
        this.managerLogin = managerLogin;
    }

    public double getFilterMinAmount() {
        return filterMinAmount;
    }

    public void setFilterMinAmount(double filterMinAmount) {
        this.filterMinAmount = filterMinAmount;
    }
}