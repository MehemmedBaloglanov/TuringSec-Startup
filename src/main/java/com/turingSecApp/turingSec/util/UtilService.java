package com.turingSecApp.turingSec.util;

import com.turingSecApp.turingSec.config.websocket.security.CustomWebsocketSecurityContext;
import com.turingSecApp.turingSec.model.entities.MockData;
import com.turingSecApp.turingSec.model.entities.program.Program;
import com.turingSecApp.turingSec.model.entities.report.Report;
import com.turingSecApp.turingSec.model.entities.role.Role;
import com.turingSecApp.turingSec.model.entities.user.CompanyEntity;
import com.turingSecApp.turingSec.model.entities.user.HackerEntity;
import com.turingSecApp.turingSec.model.entities.user.UserEntity;
import com.turingSecApp.turingSec.model.repository.*;
import com.turingSecApp.turingSec.exception.custom.*;
import com.turingSecApp.turingSec.model.repository.program.ProgramRepository;
import com.turingSecApp.turingSec.model.repository.report.ReportRepository;
import com.turingSecApp.turingSec.response.program.BugBountyProgramWithAssetTypeDTO;
import com.turingSecApp.turingSec.response.user.AuthResponse;
import com.turingSecApp.turingSec.service.MockDataService;
import com.turingSecApp.turingSec.service.interfaces.IReportService;
import com.turingSecApp.turingSec.util.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UtilService {
    private final ReportRepository reportRepository;
    private final CompanyRepository companyRepository;
    private final AdminRepository adminRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomWebsocketSecurityContext websocketSecurityContext;


    //todo: separate websocket and http util service
    public Object getAuthenticatedBaseUser() {
        try {
            return getAuthenticatedHackerWithHTTP();
        } catch (UserNotFoundException e) {
            // if exception occurs it is not Hacker
            log.warn("It is not Hacker entity!");
            return getAuthenticatedCompanyWithHTTP();
        }
    }
    //
    public Object getAuthenticatedBaseUserForWebsocket() {
        try {
            return getAuthenticatedHackerWithWEBSOCKET();
        } catch (UserNotFoundException e) {
            // if exception occurs it is not Hacker
            log.warn("It is not Hacker entity!");
            return getAuthenticatedCompanyWithWEBSOCKET();
        }
    }

    // Method to retrieve authenticated user(Hacker)
    // refactorThis
    public UserEntity getAuthenticatedHackerWithHTTP(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return getAuthenticatedHacker(authentication);
    }
    public UserEntity getAuthenticatedHackerWithWEBSOCKET(){
        Authentication authentication =  websocketSecurityContext.getAuthentication();
        return getAuthenticatedHacker(authentication);
    }
    public UserEntity getAuthenticatedHacker(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException("User with username " + username + " not found"));
        } else {
            throw new UnauthorizedException();
        }
    }

    // Method to retrieve authenticated company
    public CompanyEntity getAuthenticatedCompanyWithHTTP() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return getAuthenticatedCompany(authentication);
    }
    public CompanyEntity getAuthenticatedCompanyWithWEBSOCKET() {
        Authentication authentication =  websocketSecurityContext.getAuthentication();
        return getAuthenticatedCompany(authentication);
    }
    public CompanyEntity getAuthenticatedCompany(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            CompanyEntity company = companyRepository.findByEmail(email);
            if(company==null){
                throw  new CompanyNotFoundException("Company with email " + email + " not found , in getAuthenticatedCompany()");
            }
            return company;
        } else {
            throw new UnauthorizedException();
        }
    }

    // User service
    // Method to get hacker roles
    public Set<Role> getHackerRoles() {
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName("HACKER"));
        return roles;
    }
    public Set<Role> getAdminRoles() {
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName("ADMIN"));
        return roles;
    }

    public String generateActivationToken() {
        // You can implement your own token generation logic here
        // This could involve creating a unique token, saving it in the database,
        // and associating it with the user for verification during activation.
        // For simplicity, you can use a library like java.util.UUID.randomUUID().
        return UUID.randomUUID().toString();
    }
    // Method to build authentication response
    public AuthResponse buildAuthResponse(String token, UserEntity user, HackerEntity hacker) {
        return AuthResponse.builder()
                .accessToken(token)
                .userInfo(UserMapper.INSTANCE.toDto(user, hacker))
                .build();
    }
    public void isUserExistWithEmail(String email) {
//        System.out.println(email);
        if (userRepository.findByEmail(email) != null) {
            throw new EmailAlreadyExistsException("Email is already taken.");
        }
    }

    public void isUserExistWithUsername(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UserAlreadyExistsException("Username is already taken.");
        }
    }
    // Method to check if the user is activated
    public void checkUserIsActivated(UserEntity userEntity) {
        if (!userEntity.isActivated()) {
            throw new UserNotActivatedException("User is not activated yet.");
        }
    }

    public BugBountyProgramWithAssetTypeDTO mapToDTO(Program programEntity) {
        BugBountyProgramWithAssetTypeDTO dto = new BugBountyProgramWithAssetTypeDTO();
//        dto.setId(programEntity.getId());
        dto.setFromDate(programEntity.getFromDate());
        dto.setToDate(programEntity.getToDate());
        dto.setNotes(programEntity.getNotes());
        dto.setPolicy(programEntity.getPolicy());

//        dto.setAssets(programEntity.getAsset());

        // You can map other fields as needed

        return dto;
    }

    // Media services
    public Long validateHacker(UserDetails userDetails) {
        validateUserDetails(userDetails);
        UserEntity userEntity = getUserEntity(userDetails);
        return getHackerId(userEntity);
    }
    private void validateUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException();
        }
    }
    private UserEntity getUserEntity(UserDetails userDetails) {
        String username = userDetails.getUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User with username " + username + " not found"));
    }
    private Long getHackerId(UserEntity userEntity) {
        HackerEntity hackerEntity = userEntity.getHacker();
        if (hackerEntity == null) {
            throw new UserNotFoundException("Hacker ID not found for the authenticated user!");
        }
        return hackerEntity.getId();
    }
    //

    public Program findProgramById(Long id) {
        return programRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bug Bounty Program not found with id:" + id));
    }
    public Report findReportById(Long id) {
        return reportRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bug Bounty Report not found with id:" + id));
    }

    public UserEntity findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(()-> new UserNotFoundException("User not found with this id: " + userId));
    }

    @Transactional
    public void checkUserReport(Object authenticatedUser, Long reportId) throws UserMustBeSameWithReportUserException {
        Report reportOfMessage = findReportById(reportId);
        UserEntity userOfReportMessage = reportOfMessage.getUser();
        if (!authenticatedUser.equals(userOfReportMessage)) {
            throw new UserMustBeSameWithReportUserException("Message of Hacker must be same with report's Hacker");
        }
    }

    @Transactional
    public void checkCompanyReport(Object authenticatedUser,  Long reportId) throws UserMustBeSameWithReportUserException {
        Report reportOfMessage = findReportById(reportId);

        CompanyEntity companyOfReportMessage = reportOfMessage.getBugBountyProgram().getCompany();
        if (!authenticatedUser.equals(companyOfReportMessage)) {
            throw new UserMustBeSameWithReportUserException("Message of Company must be same with report's Company");
        }
    }
}
