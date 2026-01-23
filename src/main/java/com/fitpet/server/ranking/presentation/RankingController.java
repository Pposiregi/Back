package com.fitpet.server.ranking.presentation;

import com.fitpet.server.ranking.application.service.RankingService;
import com.fitpet.server.ranking.presentation.dto.RankingResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/summary")
    public Map<String, Object> getRankingSummary(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();

        List<RankingResponse> top10 = rankingService.getTop10();
        response.put("topRankings", top10);

        RankingResponse myRank = rankingService.getMyRank(userId);
        response.put("myRanking", myRank);

        return response;
    }

    // 테스트용
    @PostMapping("/score")
    public String testAddScore(@RequestParam Long userId, @RequestParam int steps) {
        rankingService.updateScore(userId, steps);
        return "업데이트 완료: User " + userId + ", 점수 " + steps;
    }
}