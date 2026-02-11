package com.fitpet.server.bodyhistory.application.service;

import java.util.List;

import com.fitpet.server.bodyhistory.presentation.dto.request.BodyHistoryCreateRequest;
import com.fitpet.server.bodyhistory.presentation.dto.request.BodyHistoryUpdateRequest;
import com.fitpet.server.bodyhistory.presentation.dto.response.BodyHistoryResponse;

public interface BodyHistoryService {

    BodyHistoryResponse createBodyHistory(Long userId, BodyHistoryCreateRequest request);

    BodyHistoryResponse findBodyHistoryById(Long historyId);

    List<BodyHistoryResponse> findAllBodyHistoriesByUserId(Long userId);

    List<BodyHistoryResponse> findMonthlyBodyHistories(Long userId, int year, int month);

    BodyHistoryResponse updateBodyHistory(Long historyId, BodyHistoryUpdateRequest request);

    void deleteBodyHistory(Long historyId);
}