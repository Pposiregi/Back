package com.fitpet.server.ranking.domain.entity;

import com.fitpet.server.user.domain.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ranking", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "date_key"})
})
public class Ranking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private double score;

    @Column(name = "date_key", nullable = false)
    private String dateKey;

    @Builder
    public Ranking(User user, double score, String dateKey) {
        this.user = user;
        this.score = score;
        this.dateKey = dateKey;
    }

    public void updateScore(double score) {
        this.score = score;
    }
}