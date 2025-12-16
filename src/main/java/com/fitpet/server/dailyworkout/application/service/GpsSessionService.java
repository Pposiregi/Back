package com.fitpet.server.dailyworkout.application.service;

import com.fitpet.server.dailyworkout.presentation.dto.request.GpsLogRequest;
import com.fitpet.server.dailyworkout.presentation.dto.request.SessionEndRequest;
import com.fitpet.server.dailyworkout.presentation.dto.request.SessionStartRequest;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsLogResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsSessionStartResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsSessionSummaryResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.SessionEndResponse;
import java.util.List;

public interface GpsSessionService {

    GpsSessionStartResponse startSession(SessionStartRequest request);

    GpsLogResponse logGps(GpsLogRequest request);

    SessionEndResponse endSession(SessionEndRequest request);

    List<GpsSessionSummaryResponse> getMonthlySessions(Long userId, int year, int month);
}