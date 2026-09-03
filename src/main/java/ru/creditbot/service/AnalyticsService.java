package ru.creditbot.service;

import ru.creditbot.model.LoanRequest;
import ru.creditbot.model.PaymentType;
import ru.creditbot.repository.LoanRequestRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsService {

    private final LoanRequestRepository repository;

    public AnalyticsService(LoanRequestRepository repository) {
        this.repository = repository;
    }

    public int getTotalRequests() {
        return repository.findAll().size();
    }

    public long getAnnuityRequestsCount() {

        long count = 0;

        for (LoanRequest request : repository.findAll()) {

            if (request.getPaymentType() == PaymentType.ANNUITY) {
                count++;
            }
        }

        return count;
    }

    public long getDifferentiatedRequestsCount() {

        long count = 0;

        for (LoanRequest request : repository.findAll()) {

            if (request.getPaymentType() == PaymentType.DIFFERENTIATED) {
                count++;
            }
        }

        return count;
    }

    public PaymentType getMostPopularPaymentType() {

        List<LoanRequest> requests = repository.findAll();

        if (requests.isEmpty()) {
            return null;
        }

        Map<PaymentType, Integer> counts = new HashMap<>();

        for (LoanRequest request : requests) {

            PaymentType type = request.getPaymentType();

            counts.put(
                    type,
                    counts.getOrDefault(type, 0) + 1
            );
        }

        PaymentType mostPopular = null;
        int maxCount = 0;

        for (Map.Entry<PaymentType, Integer> entry : counts.entrySet()) {

            if (entry.getValue() > maxCount) {

                maxCount = entry.getValue();
                mostPopular = entry.getKey();
            }
        }

        return mostPopular;
    }

    public List<LoanRequest> filterByPaymentType(
            PaymentType paymentType) {

        List<LoanRequest> result = new ArrayList<>();

        for (LoanRequest request : repository.findAll()) {

            if (request.getPaymentType() == paymentType) {
                result.add(request);
            }
        }

        return result;
    }

    public List<LoanRequest> filterByAmount(
            double minAmount,
            double maxAmount) {

        List<LoanRequest> result = new ArrayList<>();

        for (LoanRequest request : repository.findAll()) {

            if (request.getAmount() >= minAmount &&
                    request.getAmount() <= maxAmount) {

                result.add(request);
            }
        }

        return result;
    }

    public int getMostPopularMonths() {

        List<LoanRequest> requests = repository.findAll();

        if (requests.isEmpty()) {
            return 0;
        }

        Map<Integer, Integer> counts = new HashMap<>();

        for (LoanRequest request : requests) {

            int months = request.getMonths();

            counts.put(
                    months,
                    counts.getOrDefault(months, 0) + 1
            );
        }

        int mostPopularMonths = 0;
        int maxCount = 0;

        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {

            if (entry.getValue() > maxCount) {

                maxCount = entry.getValue();
                mostPopularMonths = entry.getKey();
            }
        }

        return mostPopularMonths;
    }

    public double getMostPopularInterestRate() {

        List<LoanRequest> requests = repository.findAll();

        if (requests.isEmpty()) {
            return 0;
        }

        Map<Double, Integer> counts = new HashMap<>();

        for (LoanRequest request : requests) {

            double rate = request.getAnnualInterestRate();

            counts.put(
                    rate,
                    counts.getOrDefault(rate, 0) + 1
            );
        }

        double mostPopularRate = 0;
        int maxCount = 0;

        for (Map.Entry<Double, Integer> entry : counts.entrySet()) {

            if (entry.getValue() > maxCount) {

                maxCount = entry.getValue();
                mostPopularRate = entry.getKey();
            }
        }

        return mostPopularRate;
    }
}