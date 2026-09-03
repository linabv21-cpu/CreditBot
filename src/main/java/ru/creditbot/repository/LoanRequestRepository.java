package ru.creditbot.repository;

import ru.creditbot.model.LoanRequest;

import java.util.List;

public interface LoanRequestRepository {

    void save(LoanRequest request);

    List<LoanRequest> findByUserId(long userId);

    List<LoanRequest> findAll();
}