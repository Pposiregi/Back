package com.fitpet.server.ranking.application.service;

import java.util.List;

public interface RankingSyncService {

    void syncRedisToDatabase();

    void flushChunkToDatabase(List<String> userIds, String dateStr);
}
