package com.fitpet.server.pet.application.service;

import com.fitpet.server.pet.domain.entity.PetExpression;
import com.fitpet.server.pet.domain.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PetExpressionService {

    private final PetRepository petRepository;

    @Transactional
    public void updateExpression(Long ownerId, PetExpression expression) {
        if (expression == null || ownerId == null) {
            return;
        }
        petRepository.findByOwnerId(ownerId)
            .ifPresent(pet -> pet.changeExpression(expression));
    }
}
