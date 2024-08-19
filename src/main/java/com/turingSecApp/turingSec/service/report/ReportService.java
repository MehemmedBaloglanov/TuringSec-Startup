package com.turingSecApp.turingSec.service.report;

import com.turingSecApp.turingSec.exception.custom.ReportNotFoundException;
import com.turingSecApp.turingSec.model.entities.program.Program;
import com.turingSecApp.turingSec.model.entities.report.enums.REPORTSTATUSFORCOMPANY;
import com.turingSecApp.turingSec.model.entities.report.enums.REPORTSTATUSFORUSER;
import com.turingSecApp.turingSec.model.entities.user.CompanyEntity;
import com.turingSecApp.turingSec.model.entities.report.ReportCVSS;
import com.turingSecApp.turingSec.model.entities.report.Report;
import com.turingSecApp.turingSec.model.entities.report.ReportManual;
import com.turingSecApp.turingSec.model.entities.user.UserEntity;
import com.turingSecApp.turingSec.model.repository.*;
import com.turingSecApp.turingSec.model.repository.program.ProgramRepository;
import com.turingSecApp.turingSec.model.repository.report.ReportCVSSRepository;
import com.turingSecApp.turingSec.model.repository.report.ReportManualRepository;
import com.turingSecApp.turingSec.model.repository.report.ReportRepository;
import com.turingSecApp.turingSec.exception.custom.ResourceNotFoundException;
import com.turingSecApp.turingSec.exception.custom.UserNotFoundException;
import com.turingSecApp.turingSec.file_upload.service.ReportMediaService;
import com.turingSecApp.turingSec.helper.entityHelper.report.IReportEntityHelper;
import com.turingSecApp.turingSec.payload.report.ReportCVSSPayload;
import com.turingSecApp.turingSec.payload.report.ReportManualPayload;
import com.turingSecApp.turingSec.response.report.ReportsByUserDTO;
import com.turingSecApp.turingSec.response.report.ReportsByUserWithCompDTO;
import com.turingSecApp.turingSec.response.user.UserDTO;
import com.turingSecApp.turingSec.service.interfaces.IReportService;
import com.turingSecApp.turingSec.service.user.CustomUserDetails;
import com.turingSecApp.turingSec.util.GlobalConstants;
import com.turingSecApp.turingSec.util.ReportUtilService;
import com.turingSecApp.turingSec.util.UtilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.turingSecApp.turingSec.model.entities.report.enums.REPORTSTATUSFORCOMPANY.ASSESSED;
import static com.turingSecApp.turingSec.model.entities.report.enums.REPORTSTATUSFORCOMPANY.REVIEWED;
import static com.turingSecApp.turingSec.model.entities.report.enums.REPORTSTATUSFORUSER.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService implements IReportService {
    private final ReportRepository bugBountyReportRepository;
    private final ReportManualRepository reportManualRepository;
    private final ReportCVSSRepository reportCVSSRepository;
    private final UtilService utilService;
    private final IReportEntityHelper reportEntityHelper;
    private final ReportMediaService reportMediaService;
    private final GlobalConstants globalConstants;
    private  final ReportUtilService reportUtilService;

    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Override
    public Report getBugBountyReportById(Long id) {
       return bugBountyReportRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Report not found with id:" + id));
    }
    @Override
    public ReportManual submitManualReportForTest(List<MultipartFile> files, ReportManualPayload reportPayload, Long bugBountyProgramId) throws IOException {
        // Fetch the BugBountyProgramEntity from the repository
        Program program = programRepository.findById(bugBountyProgramId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id:" + bugBountyProgramId));

        // Create a new report entity -> for basic type fields or embeddable
        ReportManual report = reportEntityHelper.createReportManualFromPayload(reportPayload);

        // Set the user for the bug bounty report
        setAuthenticatedUserToReport(1L, report);

        // Set room explicitly
        report.setRoom("191ded5d-148b-446d-8069-e8a8bd8c7ec6");

        // Set the bug bounty program for the bug bounty report
        report.setBugBountyProgram(program);
        Report savedReport1 = reportManualRepository.save(report);

        ///
        // Set child reference type fields with the relation
        ReportManual savedReport = (ReportManual) reportEntityHelper.setChildReferenceFieldsFromPayload(reportPayload,savedReport1);

        return savedReport;

    }
    @Override
    public ReportManual submitManualReport(List<MultipartFile> files, UserDetails userDetails, ReportManualPayload reportPayload, Long bugBountyProgramId) throws IOException {
        // Check the authenticated hacker
        UserEntity authenticatedUser = utilService.getAuthenticatedHackerWithHTTP();

        // Fetch the BugBountyProgramEntity from the repository
        Program program = programRepository.findById(bugBountyProgramId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id:" + bugBountyProgramId));

        // Create a new report entity -> for basic type fields or embeddable
        ReportManual report = reportEntityHelper.createReportManualFromPayload(reportPayload);

        // refactorThis: Set related entities (user and program)
        // Set the user for the bug bounty report
        setAuthenticatedUserToReport(authenticatedUser.getId(), report);

        // Set the bug bounty program for the bug bounty report
        report.setBugBountyProgram(program);
        Report savedReport1 = reportManualRepository.save(report);

        // Set Attachments
        setAttachmentsToReportIfFilesExist(files, userDetails, savedReport1);
        ///
        // Set child reference type fields with the relation
        ReportManual savedReport = (ReportManual) reportEntityHelper.setChildReferenceFieldsFromPayload(reportPayload,savedReport1);

        return savedReport;

    }

    private void setAttachmentsToReportIfFilesExist(List<MultipartFile> files, UserDetails userDetails, Report report) throws IOException {
        Long hackerId = utilService.validateHacker(userDetails);
        //todo: check report belongs to this user?

        // Call the service method to save the video
        if (files!=null){
            if(!files.isEmpty())
                reportMediaService.saveFiles(files, report.getId());
        }
    }

    private void setAuthenticatedUserToReport(Long authenticatedUserID, Report report) {
        UserEntity userFromDB = userRepository.findById(authenticatedUserID)
                .orElseThrow(() -> new UserNotFoundException("User with id " + authenticatedUserID + " not found"));
        report.setUser(userFromDB);
    }

    @Override
    public ReportCVSS submitCVSSReport(List<MultipartFile> files, UserDetails userDetails,ReportCVSSPayload reportPayload, Long bugBountyProgramId) throws IOException {
        // Check the authenticated hacker
        UserEntity authenticatedUser = utilService.getAuthenticatedHackerWithHTTP();

        // Fetch the BugBountyProgramEntity from the repository
        Program program = programRepository.findById(bugBountyProgramId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id:" + bugBountyProgramId));

        // Create a new report entity -> for basic type fields or embeddable
        ReportCVSS report = reportEntityHelper.createReportCVSSFromPayload(reportPayload);

        // refactorThis: Set related entities (user and program)

        // Set the user for the bug bounty report
        setAuthenticatedUserToReport(authenticatedUser.getId(), report);

        // Set the bug bounty program for the bug bounty report
        report.setBugBountyProgram(program);
        Report savedReport1 = reportCVSSRepository.save(report);

        // Set Attachments
        setAttachmentsToReportIfFilesExist(files, userDetails, savedReport1);
        ///
        // Set child reference type fields with the relation
        ReportCVSS savedReport = (ReportCVSS) reportEntityHelper.setChildReferenceFieldsFromPayload(reportPayload,savedReport1);

        return /*ReportMapper.INSTANCE.toDTO(savedReport)*/savedReport;
    }

    @Override
    public List<Report> getReportsByCompanyId(Long companyId) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id:" + companyId));

        List<Report> reports = bugBountyReportRepository.findByBugBountyProgramCompany(company);

        // Throw exception if it is empty
        checkEmpty(companyId, reports);

        return reports;
    }
    @Override
    public List<Report> getReportsByUserId(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id:" + userId));

        List<Report> reports = bugBountyReportRepository.findByUser(user);

        // Throw exception if it is empty
        checkEmpty(userId, reports);

        return reports;
    }
    public List<Report> getAllReports() {
        List<Report> all = bugBountyReportRepository.findAll();

        // Throw exception if it is empty
        checkEmpty(all);

        return all;
    }
    @Override
    public Report reviewReportByCompany(Long id) {
        return updateReportStatus(id, REVIEWED, UNDER_REVIEW);
    }

    @Override
    public Report acceptReportByCompany(Long id) {
        return updateReportStatus(id, ASSESSED, ACCEPTED);
    }

    @Override
    public Report rejectReportByCompany(Long id) {
        return updateReportStatus(id, ASSESSED, REJECTED);
    }

    @Override
    public List<ReportsByUserWithCompDTO> getReportsByUserWithStatus(REPORTSTATUSFORUSER status) {
        // Retrieve authenticated user
        UserEntity user = utilService.getAuthenticatedHackerWithHTTP();

        log.info("The status parameter provided: " + status);
        List<Report> userReports = null;

        if (status == null) {
            // Handle the case where the status parameter is not provided
            // It returns all without filtering the status if param is not provided
            userReports = bugBountyReportRepository.findByUser(user);
        }else{
            // Validate the status
            if (!(status.name().equalsIgnoreCase(REPORTSTATUSFORUSER.SUBMITTED.name()) ||
                    status.name().equalsIgnoreCase(REPORTSTATUSFORUSER.UNDER_REVIEW.name()) ||
                    status.name().equalsIgnoreCase(REPORTSTATUSFORUSER.ACCEPTED.name()) ||
                    status.name().equalsIgnoreCase(REPORTSTATUSFORUSER.REJECTED.name()))) {
                throw new IllegalArgumentException("Report status for user must be SUBMITTED or UNDER_REVIEW or ACCEPTED or REJECTED");
            }

            // Get all reports associated with the user and the status of report for user
            userReports = bugBountyReportRepository.findByUserAndStatusForUser(user,status);
        }

        // Group reports by user
        Map<UserDTO, List<Report>> reportsByUser = groupReportsByUser(userReports);

        // Create ReportsByUserDTO objects for each user and add them to the list
        return createReportsByUserWithCompDTO(reportsByUser);
    }

    @Override
    public List<ReportsByUserWithCompDTO> getReportsByCompanyProgramWithStatus(REPORTSTATUSFORCOMPANY status) {

        //todo:
        return  null;
    }


    private Report updateReportStatus(Long id, REPORTSTATUSFORCOMPANY companyStatus, REPORTSTATUSFORUSER userStatus) {
        Report report = getBugBountyReportById(id);

        // Check report belongs to this company
        CompanyEntity authenticatedCompany = utilService.getAuthenticatedCompanyWithHTTP();
        reportUtilService.checkUserOrCompanyReport(authenticatedCompany, report.getId());

        report.setStatusForCompany(companyStatus);
        report.setStatusForUser(userStatus);

        return bugBountyReportRepository.save(report);
    }


    private void checkEmpty(Long userId, List<Report> reports) {
        if (reports.isEmpty()) {
            throw new ReportNotFoundException("There is no report with user/company id: " + userId);
        }
    }
    private void checkEmpty(List<Report> reports) {
        if (reports.isEmpty()) {
            throw new ReportNotFoundException("There is no report");
        }
    }
    @Override
    @Transactional
    public void deleteBugBountyReport(Long id) {
        // Ensure that the authenticated hacker can only update their own report
        Report report = bugBountyReportRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Report not found with id:" + id));

        //  Check report ownership
        Object authenticatedBaseUser = utilService.getAuthenticatedBaseUser();
        reportUtilService.checkUserOrCompanyReport(authenticatedBaseUser, report.getId());

        // Find related program then remove from list<ReportEntity>, then you can delete
        Program program = programRepository.findByReportsContains(report).orElseThrow(() -> new ResourceNotFoundException("Program not found with this report:" + report));
        program.removeReport(report.getId());

        Report updatedReport = reportEntityHelper.deleteReportChildEntities(report);

        bugBountyReportRepository.delete(updatedReport);
    }
    private Map<UserDTO, List<Report>> groupReportsByUser(List<Report> userReports) {
        return userReports.stream()
                .collect(Collectors.groupingBy(report ->
                        new UserDTO(report.getUser().getId(), report.getUser().getUsername(),
                                report.getUser().getEmail(), report.getUser().getFirst_name(), report.getUser().getLast_name(), report.getUser().getHacker().getId())));
    }

    private List<ReportsByUserWithCompDTO> createReportsByUserWithCompDTO(Map<UserDTO, List<Report>> reportsByUser) {
        return reportsByUser.entrySet().stream()
                .map(entry -> {
                    UserDTO userDTO = entry.getKey();
                    List<Report> reports = entry.getValue();
                    // Extract a single company name from bug bounty programs associated with the reports
                    String companyName = reports.stream()
                            .map(report -> report.getBugBountyProgram().getCompany().getCompany_name())
                            .findFirst()
                            .orElse(null);
                    // Create and return the ReportsByUserDTO object
                    return new ReportsByUserWithCompDTO(companyName, reports);
                })
                .collect(Collectors.toList());
    }

   //
   @Override
   public List<ReportsByUserDTO> getBugBountyReportsForCompanyPrograms() {
       // Retrieve the authenticated user details
       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
       CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

       // Extract the company from the authenticated user details
       CompanyEntity company = extractCompanyFromUserDetails(userDetails);

       // Retrieve bug bounty reports submitted for the company's programs
       return getReportsForCompanyPrograms(company);
   }




    private CompanyEntity extractCompanyFromUserDetails(CustomUserDetails userDetails) {
        Object user = userDetails.getUser();
        if (!(user instanceof CompanyEntity)) {
            throw new IllegalStateException("Authenticated user is not a company");
        }
        return (CompanyEntity) user;
    }

    private List<ReportsByUserDTO> getReportsForCompanyPrograms(CompanyEntity company) {
        // Fetch the company entity along with its bug bounty programs within an active Hibernate session
        company = companyRepository.findById(company.getId()).orElse(null);
        if (company == null) {
            log.info("Company is not found , in getReportsForCompanyPrograms method ");
            return Collections.emptyList();
        }

        // Retrieve bug bounty programs associated with the company
        Set<Program> bugBountyPrograms = company.getBugBountyPrograms();

        // Retrieve bug bounty reports submitted for the company's programs
        List<Report> reports = fetchReportsForPrograms(bugBountyPrograms);

        // Group reports by user
        Map<UserDTO, List<Report>> reportsByUser = groupReportsByUser(reports);

        // Fetch image URL for each user
        Map<Long, String> userImgUrls = fetchUserImgUrls(reportsByUser);

        return createReportsByUserDTOList(reportsByUser, userImgUrls);
    }

    private List<Report> fetchReportsForPrograms(Set<Program> bugBountyPrograms) {
        return bugBountyReportRepository.findByBugBountyProgramIn(bugBountyPrograms);
    }

    private Map<Long, String> fetchUserImgUrls(Map<UserDTO, List<Report>> reportsByUser) {
        return reportsByUser.keySet().stream()
                .collect(Collectors.toMap(UserDTO::getHackerId, this::getUserImgUrl, (url1, url2) -> url1)); // Merge function to handle duplicate keys
    }


    private List<ReportsByUserDTO> createReportsByUserDTOList(Map<UserDTO, List<Report>> reportsByUser, Map<Long, String> userImgUrls) {
        return reportsByUser.entrySet().stream()
                .map(entry -> {
                    UserDTO userDTO = entry.getKey();
                    List<Report> userReports = entry.getValue();

                    ReportsByUserDTO reportsByUserDTO = new ReportsByUserDTO();
                    reportsByUserDTO.setUserId(userDTO.getId());
                    reportsByUserDTO.setUser(userDTO);
                    reportsByUserDTO.setReports(userReports);

                    reportsByUserDTO.setHas_hacker_profile_pic(userImgUrls.containsKey(userDTO.getHackerId()));
                    // reportsByUserDTO.setUserImgUrl(userImgUrls.getOrDefault(userDTO.getHackerId(), ""));
                    return reportsByUserDTO;
                })
                .collect(Collectors.toList());
    }


    private String getUserImgUrl(UserDTO userDTO) {
        return globalConstants.ROOT_LINK + "/api/background-image-for-hacker/download/" + userDTO.getHackerId();
    }

    private String getUsernameFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        throw new RuntimeException("Unable to extract username from JWT token");
    }

}
