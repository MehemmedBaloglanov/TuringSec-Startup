package com.turingSecApp.turingSec.model.repository.program;

import com.turingSecApp.turingSec.model.entities.program.Program;
import com.turingSecApp.turingSec.model.entities.user.CompanyEntity;
import com.turingSecApp.turingSec.model.entities.report.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramRepository extends JpaRepository<Program,Long> {
    List<Program> findByCompany(CompanyEntity company);
    Optional<Program> findByReportsContains(Report reports);

    Program findByFromDateAndToDateAndNotesAndPolicyAndCompany(
            LocalDate fromDate, LocalDate toDate, String notes, String policy, CompanyEntity company);
}
