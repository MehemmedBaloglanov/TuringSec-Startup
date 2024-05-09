package com.turingSecApp.turingSec.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class BugBountyProgramWithAssetTypePayload {
    // private Long id; // New field for company ID

    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    private LocalDate toDate;
    @NotBlank(message = "Policy is required")
    private String policy;
    private String notes;
    private List<@Valid  AssetTypePayload> assetTypes = new ArrayList<>();

    private List<@Valid StrictPayload> prohibits = new ArrayList<>();

    private List<String> inScope = new ArrayList<>();

    private List<String> outOfScope = new ArrayList<>();

//    @NotNull(message = "Company Id is required")
//    private Long companyId;
//    @NotNull(message = "Program Id is required")
//    private Long programId;

    // Getters and setters
}
