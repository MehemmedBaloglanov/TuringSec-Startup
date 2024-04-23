package com.turingSecApp.turingSec.dao.repository;

import com.turingSecApp.turingSec.dao.entities.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);

    UserEntity findByActivationToken(String token);

    UserEntity findByEmail(String email);

}
