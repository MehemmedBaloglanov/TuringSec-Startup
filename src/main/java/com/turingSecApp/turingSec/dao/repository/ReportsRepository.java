package com.turingSecApp.turingSec.dao.repository;

import com.turingSecApp.turingSec.dao.entities.BugBountyProgramEntity;
import com.turingSecApp.turingSec.dao.entities.ReportEntity;
import com.turingSecApp.turingSec.dao.entities.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository

public interface ReportsRepository extends JpaRepository<ReportEntity, Long> {
    List<ReportEntity> findByUser(UserEntity user);
    List<ReportEntity> findByBugBountyProgram(BugBountyProgramEntity program);
    @Query("SELECT r FROM ReportEntity r WHERE r.bugBountyProgram IN :programs")
    List<ReportEntity> findByBugBountyProgramIn(Collection<BugBountyProgramEntity> programs);
    void deleteAllByUser(UserEntity user);
}
