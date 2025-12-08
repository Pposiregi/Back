package com.fitpet.server.pet.infra.jpa;

import com.fitpet.server.pet.domain.entity.Pet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetJpaRepository extends JpaRepository<Pet, Long> {

    Optional<Pet> findByOwnerId(Long ownerId);
}
