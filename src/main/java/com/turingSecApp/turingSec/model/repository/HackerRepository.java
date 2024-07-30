package com.turingSecApp.turingSec.model.repository;


import com.turingSecApp.turingSec.model.entities.user.HackerEntity;
import com.turingSecApp.turingSec.model.entities.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HackerRepository extends JpaRepository<HackerEntity, Long> {
    HackerEntity findByUser(UserEntity user);

}
