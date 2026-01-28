package com.fitpet.server.ranking.presentation;

import com.fitpet.server.ranking.application.service.RankingService;
import com.fitpet.server.ranking.presentation.dto.RankingResponse;
import com.fitpet.server.ranking.presentation.dto.RankingSummaryResponse;
import com.fitpet.server.shared.annotation.AuthUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<RankingSummaryResponse> getRankingSummary(@AuthUser Long userId) {
        List<RankingResponse> top10 = rankingService.getTop10();

        RankingResponse myRank = rankingService.getMyRank(userId);

        return ResponseEntity.ok(
                RankingSummaryResponse.of(top10, myRank)
        );
    }

    @PostMapping("/score")
    public ResponseEntity<String> updateScore(@AuthUser Long userId, @RequestParam int steps) {
        rankingService.updateScore(userId, steps);
        return ResponseEntity.ok("success");
    }
}