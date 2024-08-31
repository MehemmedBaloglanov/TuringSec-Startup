package com.turingSecApp.turingSec.controller;

import com.turingSecApp.turingSec.model.entities.report.ReportCVSS;
import com.turingSecApp.turingSec.model.entities.report.Report;
import com.turingSecApp.turingSec.model.entities.report.ReportManual;
import com.turingSecApp.turingSec.model.entities.report.enums.REPORTSTATUSFORCOMPANY;
import com.turingSecApp.turingSec.model.entities.report.enums.REPORTSTATUSFORUSER;
import com.turingSecApp.turingSec.payload.report.ReportCVSSPayload;
import com.turingSecApp.turingSec.payload.report.ReportDateRangeRequest;
import com.turingSecApp.turingSec.payload.report.ReportManualPayload;
import com.turingSecApp.turingSec.response.report.ReportsByUserDTO;
import com.turingSecApp.turingSec.response.report.ReportsByUserWithCompDTO;
import com.turingSecApp.turingSec.response.base.BaseResponse;
import com.turingSecApp.turingSec.service.interfaces.IReportService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bug-bounty-reports")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    // todo
    //  DTO cevirmek
    //  token istemeye ehtiyac yoxdu, payload hecne, dto -> BaseResponse<List<ReportDTO>> -> company id , program id , userId(hansi hackerdi) , username of user +

    //1. user report atannan sonra  ---> submitted - unreviewed +
    // POST submitManualReport-da statusu submitted - unreviewed set et +
    // POST submitCVSS-da statusu submitted - unreviewed set et +

    // hacker hissesinde all( submitted underreview (accepted | rejected) -> assessed )
    // sirket hissesinde all(unreviewed,reviewed,assessed)

    // get all reports for hacker -> var -> getAllBugBountyReportsByUser +
    // get submitted reports for hacker
    // get underreview reports for hacker
    // get accepted reports for hacker
    // get rejected reports for hacker
    // get assessed reports for hacker -> if accepted | rejected return

    // get all reports for company -> var -> getAllBugBountyReportsByCompany +
    // get submitted reports for company
    // get unreviewed reports for company
    // get reviewed reports for company
    // get assessed reports for company

    // QUERY ILE ET -> EGER QUERY PARAM SEHVDISE ILLAGEAL ARGUMENT


    //  User - Company  +
    // user report atannan sonra  ---> submitted - unreviewed +
    // company reporta tiklasa ----> underreview - reviewed +
    // company reportu deyerlendirir --->
    // accepted - assessed +
    // rejected - assessed +


    private final IReportService bugBountyReportService;

    @PatchMapping("/{id}/company/review")
    public BaseResponse<Report> reviewReportByCompany(@PathVariable Long id){
        return BaseResponse.success(bugBountyReportService.reviewReportByCompany(id));
    }

    @PatchMapping("/{id}/company/accept")
    public BaseResponse<Report> acceptReportByCompany(@PathVariable Long id){
        return BaseResponse.success(bugBountyReportService.acceptReportByCompany(id));
    }
    @PatchMapping("/{id}/company/reject")
    public BaseResponse<Report> rejectReportByCompany(@PathVariable Long id){
        return BaseResponse.success(bugBountyReportService.rejectReportByCompany(id));
    }

    @GetMapping("/{id}")
    public BaseResponse<Report> getBugBountyReportById(@PathVariable Long id) {
        Report bugBountyReport = bugBountyReportService.getBugBountyReportById(id);
        return BaseResponse.success(bugBountyReport);
    }

    // Learn:
    //  @RequestPart
    //  Purpose: Used to bind a part of a "multipart/form-data" request to a method parameter.
    //  Typical Usage: When you need to handle file uploads along with other form fields or JSON data in a multipart request.

//    https://github.com/swagger-api/swagger-core/issues/3050
//ResponseEntity<Void> doSomething(MyMultipartRequest request) {...}

    // Handle big videos -> https://stackoverflow.com/questions/2689989/how-to-handle-maxuploadsizeexceededexception
    @PostMapping(value = "/manualReport",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<ReportManual> submitManualReport(
            @RequestPart(value = "files",required = false) @Parameter(description = "File to upload") @Schema(type = "string", format = "binary") List<MultipartFile> files,
            @RequestPart("reportPayload") @Valid ReportManualPayload reportPayload,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long bugBountyProgramId
    ) {

        ReportManual submittedBugBountyReport = null;
        try {
            submittedBugBountyReport = bugBountyReportService.submitManualReport(files,userDetails,reportPayload, bugBountyProgramId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return BaseResponse.success(
                submittedBugBountyReport,
                "Bug bounty report submitted successfully");
    }
    @PostMapping(value = "/CVSSReport",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<ReportCVSS> submitCVSSReport(
            @RequestPart(value = "files",required = false) @Parameter(description = "File to upload") @Schema(type = "string", format = "binary") List<MultipartFile> files,
            @RequestPart("reportPayload") @Valid ReportCVSSPayload reportPayload,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long bugBountyProgramId) {
        ReportCVSS submittedBugBountyReport = null;
        try {
            submittedBugBountyReport = bugBountyReportService.submitCVSSReport(files,userDetails,reportPayload, bugBountyProgramId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return BaseResponse.success(submittedBugBountyReport,"Bug bounty report submitted successfully");
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> deleteBugBountyReport(@PathVariable Long id) {
        bugBountyReportService.deleteBugBountyReport(id);
        return BaseResponse.noContent();
    }

    @GetMapping("/user")
    public BaseResponse<List<ReportsByUserWithCompDTO>> getReportsByUserWithStatus(@RequestParam(required = false,value = "status") REPORTSTATUSFORUSER status) {
        List<ReportsByUserWithCompDTO> userReports = bugBountyReportService.getReportsByUserWithStatus(status);
        return BaseResponse.success(userReports);
    }
    @GetMapping("/company")
    public BaseResponse<List<ReportsByUserDTO>> getReportsByCompanyProgramWithStatus(@RequestParam(required = false,value = "status") REPORTSTATUSFORCOMPANY status) {
        List<ReportsByUserDTO> userReports = bugBountyReportService.getReportsByCompanyProgramWithStatus(status);
        return BaseResponse.success(userReports);
    }

    @GetMapping("/company/{id}")
    public BaseResponse<List<Report>> getAllReportsByCompanyId(@PathVariable Long id) {
        return BaseResponse.success(bugBountyReportService.getReportsByCompanyId(id));
    }

    @GetMapping("/user/{id}")
    public BaseResponse<List<Report>> getAllReportsByUserId(@PathVariable Long id) {
        return BaseResponse.success(bugBountyReportService.getReportsByUserId(id));
    }

    @GetMapping
    public BaseResponse<List<Report>> getAllReport(){
        return BaseResponse.success(bugBountyReportService.getAllReports());
    }

    @GetMapping(path = "/date-range")
    public BaseResponse<List<Report>> getReportDateRange(@RequestParam("startDate") LocalDate createdAt,
                                                         @RequestParam("endDate")LocalDate endDate){
        return BaseResponse.success(bugBountyReportService.getReportByDateRange( .createdAt,endDate));
    }



}
