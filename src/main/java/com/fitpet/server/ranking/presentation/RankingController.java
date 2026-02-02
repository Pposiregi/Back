package com.fitpet.server.ranking.presentation;

import com.fitpet.server.ranking.application.facade.RankingFacade;
import com.fitpet.server.ranking.presentation.dto.RankingSummaryResponse;
import com.fitpet.server.shared.annotation.AuthUser;
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
    
    private final RankingFacade rankingFacade;

    @GetMapping("/summary")
    public ResponseEntity<RankingSummaryResponse> getRankingSummary(@AuthUser Long userId) {
        return ResponseEntity.ok(rankingFacade.getRankingSummary(userId));
    }

    @PostMapping("/score")
    public ResponseEntity<String> updateScore(@AuthUser Long userId, @RequestParam int steps) {
        rankingFacade.updateScore(userId, steps);
        return ResponseEntity.ok("success");
    }
}