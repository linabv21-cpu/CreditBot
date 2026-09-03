package ru.creditbot.repository;

import ru.creditbot.model.LoanRequest;

import java.util.ArrayList;
import java.util.List;

public class InMemoryLoanRequestRepository implements LoanRequestRepository {

    private final List<LoanRequest> requests = new ArrayList<>();

    @Override
    public void save(LoanRequest request) {
        requests.add(request);
    }

    @Override
    public List<LoanRequest> findByUserId(long userId) {

        List<LoanRequest> result = new ArrayList<>();

        for (LoanRequest request : requests) {

            if (request.getUserId() == userId) {
                result.add(request);
            }
        }

        return result;
    }

    @Override
    public List<LoanRequest> findAll() {
        return new ArrayList<>(requests);
    }
}