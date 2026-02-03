package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.application.dto.RankingSyncContext;
import java.util.List;

public interface RankingSyncService {

    void syncRedisToDatabase();

    void flushChunkToDatabase(List<String> userIds, RankingSyncContext context);
}