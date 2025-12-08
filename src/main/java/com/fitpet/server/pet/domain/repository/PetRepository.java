package com.fitpet.server.pet.domain.repository;

import com.fitpet.server.pet.domain.entity.Pet;
import java.util.Optional;

public interface PetRepository {
    Pet save(Pet pet);

    Optional<Pet> findById(Long petId);

    Optional<Pet> findByOwnerId(Long ownerId);

    void delete(Pet pet);
}
