package com.fitpet.server.pet.presentation.controller;

import com.fitpet.server.pet.application.service.PetService;
import com.fitpet.server.pet.presentation.dto.PetCreateRequest;
import com.fitpet.server.pet.presentation.dto.PetDto;
import com.fitpet.server.pet.presentation.dto.PetUpdateRequest;
import com.fitpet.server.shared.annotation.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets")
@Slf4j
public class PetController {

    private final PetService petService;

    @PostMapping
    public ResponseEntity<PetDto> create(
            @AuthUser Long userId,
            @Valid @RequestBody PetCreateRequest request
    ) {
        log.info("[PetController] 펫 생성 요청 : petName = {} , petType = {},  ownerId = {}",
                request.name(), request.petType(), userId);

        PetDto createdPet = petService.createPet(userId, request);

        log.info("[PetController] 펫 생성 완료 : petName = {} , petType = {},  ownerId = {}",
                request.name(), request.petType(), userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdPet);
    }

    @DeleteMapping("/{petId}")
    public ResponseEntity<Void> delete(
            @AuthUser Long userId,
            @PathVariable Long petId
    ) {
        log.info("[PetController] 펫 삭제 요청 : userId = {}, petId = {}", userId, petId);

        petService.deletePet(userId, petId);

        log.info("[PetController] 펫 삭제 완료 : userId = {}, petId = {}", userId, petId);

        return ResponseEntity
                .noContent()
                .build();
    }

    @GetMapping("/{petId}")
    public ResponseEntity<PetDto> read(
            @AuthUser Long userId,
            @PathVariable Long petId
    ) {
        log.info("[PetController] 펫 조회 요청 : userId = {}, petId = {}", userId, petId);

        PetDto pet = petService.findPet(userId, petId);

        log.info("[PetController] 펫 조회 완료 : userId = {}, petId = {}", userId, petId);

        return ResponseEntity.ok().body(pet);
    }

    @PatchMapping("/{petId}")
    public ResponseEntity<PetDto> update(
            @AuthUser Long userId,
            @PathVariable Long petId,
            @Valid @RequestBody PetUpdateRequest request
    ) {
        log.info("[PetController] 펫 수정 요청 : userId = {}, petId = {}", userId, petId);

        PetDto pet = petService.updatePet(userId, petId, request);

        log.info("[PetController] 펫 수정 완료 : userId = {}, petId = {}", userId, petId);

        return ResponseEntity.ok().body(pet);
    }
}