package com.fitpet.server.pet.infra.adapter;

import com.fitpet.server.pet.domain.entity.Pet;
import com.fitpet.server.pet.domain.repository.PetRepository;
import com.fitpet.server.pet.infra.jpa.PetJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PetRepositoryAdapter implements PetRepository {
    private final PetJpaRepository jpa;

    @Override
    public Pet save(Pet pet) {
        return jpa.save(pet);
    }

    @Override
    public Optional<Pet> findById(Long petId) {
        return jpa.findById(petId);
    }

    @Override
    public Optional<Pet> findByOwnerId(Long ownerId) {
        return jpa.findByOwnerId(ownerId);
    }

    @Override
    public void delete(Pet pet) {
        jpa.delete(pet);
    }

}
