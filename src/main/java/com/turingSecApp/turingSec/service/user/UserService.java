package com.turingSecApp.turingSec.service.user;


import com.turingSecApp.turingSec.Request.*;
import com.turingSecApp.turingSec.dao.entities.AssetTypeEntity;
import com.turingSecApp.turingSec.dao.entities.BugBountyProgramEntity;
import com.turingSecApp.turingSec.dao.entities.CompanyEntity;
import com.turingSecApp.turingSec.dao.entities.HackerEntity;
import com.turingSecApp.turingSec.dao.entities.role.Role;
import com.turingSecApp.turingSec.dao.entities.user.UserEntity;
import com.turingSecApp.turingSec.dao.repository.*;
import com.turingSecApp.turingSec.exception.custom.*;
import com.turingSecApp.turingSec.filter.JwtUtil;
import com.turingSecApp.turingSec.payload.*;
import com.turingSecApp.turingSec.response.AuthResponse;
import com.turingSecApp.turingSec.response.BugBountyProgramDTO;
import com.turingSecApp.turingSec.response.UserHackerDTO;
import com.turingSecApp.turingSec.service.EmailNotificationService;
import com.turingSecApp.turingSec.service.ProgramsService;
import com.turingSecApp.turingSec.service.interfaces.IUserService;
import com.turingSecApp.turingSec.util.ProgramMapper;
import com.turingSecApp.turingSec.util.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.turingSecApp.turingSec.util.GlobalConstants.ROOT_LINK;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final EmailNotificationService emailNotificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final ProgramsService programsService;
    private final UserRepository userRepository;

    private final HackerRepository hackerRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final ProgramsRepository programsRepository;
    private final ReportsRepository bugBountyReportRepository;
    @Override
    public AuthResponse registerHacker(RegisterPayload registerPayload) {
        // Ensure the user doesn't exist
        checkUserDoesNotExist(registerPayload);

        // Create and save the user entity
        UserEntity user = createUserEntity(registerPayload);

        // Create and save the hacker entity
        HackerEntity hackerEntity = createAndSaveHackerEntity(user);

        // Send activation email
        sendActivationEmail(user);

        // Generate token for the registered user
        String token = generateTokenForUser(user);

        // Retrieve the user and hacker details from the database
        UserEntity userById = findUserById(user.getId());
        HackerEntity hackerFromDB = findHackerByUser(userById);

        // Build and return the authentication response
        return buildAuthResponse(token, userById, hackerFromDB);
    }

    // Method to check if user already exists with the provided username or email
    private void checkUserDoesNotExist(RegisterPayload registerPayload) {
        isUserExistWithUsername(registerPayload.getUsername());
        isUserExistWithEmail(registerPayload.getEmail());
    }

    // Method to create and save the user entity
    private UserEntity createUserEntity(RegisterPayload registerPayload) {
        UserEntity user = UserEntity.builder()
                .first_name(registerPayload.getFirstName())
                .last_name(registerPayload.getLastName())
                .country(registerPayload.getCountry())
                .username(registerPayload.getUsername())
                .email(registerPayload.getEmail())
                .password(passwordEncoder.encode(registerPayload.getPassword()))
                .activated(false)
                .build();

        // Set user roles
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName("HACKER"));
        user.setRoles(roles);

        // Save the user
        return userRepository.save(user);
    }

    // Method to create and save the hacker entity
    private HackerEntity createAndSaveHackerEntity(UserEntity user) {
        //Note: To fetch user explicitly to avoid save process instead it updates because there is user entity with actual id not null
        UserEntity fetchedUser = userRepository.findByUsername(user.getUsername()).orElseThrow(()-> new UserNotFoundException("User with username " + user.getUsername() + " not found"));


        HackerEntity hackerEntity = new HackerEntity();
        hackerEntity.setUser(fetchedUser);
        hackerEntity.setFirst_name(fetchedUser.getFirst_name());
        hackerEntity.setLast_name(fetchedUser.getLast_name());
        hackerEntity.setCountry(fetchedUser.getCountry());
        hackerRepository.save(hackerEntity);

        // Accomplish associations between user and hacker
        fetchedUser.setHacker(hackerEntity);
        userRepository.save(fetchedUser);

        return hackerEntity;
    }

    // Method to generate authentication token for the user
    private String generateTokenForUser(UserEntity user) {
        UserDetails userDetails = new CustomUserDetails(user);
        return jwtTokenProvider.generateToken(userDetails);
    }


    // Method to retrieve hacker details by associated user
    private HackerEntity findHackerByUser(UserEntity user) {
        return hackerRepository.findByUser(user);
    }

    // Method to build authentication response
    private AuthResponse buildAuthResponse(String token, UserEntity user, HackerEntity hacker) {
        return AuthResponse.builder()
                .accessToken(token)
                .userInfo(UserMapper.INSTANCE.toDto(user, hacker))
                .build();
    }
    ///////////\\\\\\\\\\\

    @Override
    public void insertActiveHacker(RegisterPayload registerPayload) {
        // Ensure the user doesn't exist
        checkUserDoesNotExist(registerPayload);

        // Create and Save the user entity
        UserEntity user = createUserEntity(registerPayload, true);

        // Create and save the hacker entity
        HackerEntity hackerEntity = createAndSaveHackerEntity(user);

        // Accomplish associations between user and hacker
        associateUserWithHacker(user, hackerEntity);
    }

    // Method to create and save the user entity
    private UserEntity createUserEntity(RegisterPayload registerPayload, boolean activated) {
        UserEntity user = UserEntity.builder()
                .first_name(registerPayload.getFirstName())
                .last_name(registerPayload.getLastName())
                .country(registerPayload.getCountry())
                .username(registerPayload.getUsername())
                .email(registerPayload.getEmail())
                .password(passwordEncoder.encode(registerPayload.getPassword()))
                .activated(activated)
                .roles(getHackerRoles())
                .build();

        return userRepository.save(user);
    }

    // Method to accomplish associations between user and hacker
    private void associateUserWithHacker(UserEntity user, HackerEntity hackerEntity) {
        user.setHacker(hackerEntity);
        userRepository.save(user);
    }

    // Method to get hacker roles
    private Set<Role> getHackerRoles() {
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName("HACKER"));
        return roles;
    }


    /////////////////////\\\\\\\\\\\\\\\\

    private void isUserExistWithEmail(String email) {
//        System.out.println(email);
        if (userRepository.findByEmail(email) != null) {
            throw new EmailAlreadyExistsException("Email is already taken.");
        }
    }

    private void isUserExistWithUsername(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UserAlreadyExistsException("Username is already taken.");
        }
    }

    @Override
    public boolean activateAccount(String token) {
        // Retrieve user by activation token
        UserEntity user = userRepository.findByActivationToken(token);

        if (user != null /*&& !user.isActivated()*/) {
            // Activate the user by updating the account status or perform other necessary actions
            user.setActivated(true);
            userRepository.save(user);
            return true;
        }

        return false;
    }
    @Override
    public AuthResponse loginUser(LoginRequest loginRequest) {
        // Find user by email
        UserEntity userEntity = findUserByEmail(loginRequest.getUsernameOrEmail());

        // If user not found by email, try finding by username
        if (userEntity == null) {
            userEntity = findUserByUsername(loginRequest.getUsernameOrEmail());
        }

        // Authenticate user if found
        if (userEntity != null && passwordEncoder.matches(loginRequest.getPassword(), userEntity.getPassword())) {
            // Check if the user is activated
            checkUserIsActivated(userEntity);

            // Generate token using the user details
            String token = generateTokenForUser(userEntity);

            // Retrieve user and hacker details from the database
            UserEntity userById = findUserById(userEntity.getId());
            HackerEntity hackerFromDB = findHackerByUser(userById);

            // Create and return authentication response
            return buildAuthResponse(token, userById, hackerFromDB);
        } else {
            // Authentication failed
            throw new BadCredentialsException("Invalid username/email or password.");
        }
    }

    // Method to find user by email
    private UserEntity findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Method to check if the user is activated
    private void checkUserIsActivated(UserEntity userEntity) {
        if (!userEntity.isActivated()) {
            throw new UserNotActivatedException("User is not activated yet.");
        }
    }


    @Override
    public void changePassword(ChangePasswordRequest request) {
        // Retrieve authenticated user
        UserEntity user = getAuthenticatedUser();

        // Validate current password
        validateCurrentPassword(request, user);

        // Validate and update new password
        updatePassword(request.getNewPassword(), request.getConfirmNewPassword(), user);
    }

    // Method to retrieve authenticated user
    private UserEntity getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException("User with username " + username + " not found"));
        } else {
            throw new UnauthorizedException();
        }
    }

    // Method to validate current password
    private void validateCurrentPassword(ChangePasswordRequest request, UserEntity user) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Incorrect current password");
        }
    }

    // Method to validate and update new password
    private void updatePassword(String newPassword,String confirmedPassword, UserEntity user) {
        // Validate new password and confirm new password
        if (!newPassword.equals(confirmedPassword)) {
            throw new BadCredentialsException("New password and confirm new password do not match");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }


    @Override
    public void changeEmail(ChangeEmailRequest request) {
        // Retrieve authenticated user
        UserEntity user = getAuthenticatedUser();

        // Validate current password
        validateCurrentPassword(request, user);

        // Check if the new email is already in use
        checkIfEmailExists(request.getNewEmail());

        // Update email
        user.setEmail(request.getNewEmail());
        userRepository.save(user);
    }

   // Method to validate current password
    private void validateCurrentPassword(ChangeEmailRequest request, UserEntity user) {
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Incorrect current password");
        }
    }

    // Method to check if the new email is already in use
    private void checkIfEmailExists(String newEmail) {
        if (userRepository.findByEmail(newEmail) != null) {
            throw new EmailAlreadyExistsException("Email " + newEmail + " is already in use");
        }
    }


    @Override
    public UserHackerDTO updateProfile(UserUpdateRequest userUpdateRequest) {
        // Get the authenticated user details from the security context
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Extract the username from the authenticated user details
        String username = userDetails.getUsername();

        // Current username can be same
        if(!username.equals(userUpdateRequest.getUsername()))
            isUserExistWithUsername(userUpdateRequest.getUsername());

        // Retrieve the user entity from the repository based on the username
        UserEntity userEntity = userRepository.findByUsername(username).orElseThrow(()-> new UserNotFoundException("User with username " + username + " not found"));


        // Update the user's first name and last name with the new values

        userEntity.setUsername(userUpdateRequest.getUsername());

        userEntity.setFirst_name(userUpdateRequest.getFirstName());
        userEntity.setLast_name(userUpdateRequest.getLastName());
        userEntity.setCountry(userUpdateRequest.getCountry());

        // Save the updated user entity
        userRepository.save(userEntity);

        // Update the corresponding HackerEntity if it exists
        HackerEntity hackerEntity = hackerRepository.findByUser(userEntity);
        if (hackerEntity != null) {
            hackerEntity.setFirst_name(userUpdateRequest.getFirstName());
            hackerEntity.setLast_name(userUpdateRequest.getLastName());
            hackerEntity.setCountry(userUpdateRequest.getCountry());
            hackerEntity.setCity(userUpdateRequest.getCity());

            hackerEntity.setWebsite(userUpdateRequest.getWebsite());
//            hackerEntity.setBackground_pic(profileUpdateRequest.getBackground_pic());
//            hackerEntity.setProfile_pic(profileUpdateRequest.getProfile_pic());

            hackerEntity.setBio(userUpdateRequest.getBio());
            hackerEntity.setLinkedin(userUpdateRequest.getLinkedin());
            hackerEntity.setTwitter(userUpdateRequest.getTwitter());
            hackerEntity.setGithub(userUpdateRequest.getGithub());

            hackerEntity.setUser(userEntity);

            hackerRepository.save(hackerEntity);
        }

        userEntity.setHacker(hackerEntity);
        userRepository.save(userEntity);

        return  UserMapper.INSTANCE.toDto(userEntity, hackerEntity);
    }


    public String generateNewToken(UserHackerDTO updatedUser) {
        UserDetails userDetailsFromDB = userDetailsService.loadUserByUsername(updatedUser.getUsername());
        // Assuming you have generated a new token here
        return jwtTokenProvider.generateToken(userDetailsFromDB);
    }

    @Override
    public UserDTO getUserById(Long userId) {
        // Retrieve user information by ID
        Optional<UserEntity> userOptional = userRepository.findById(userId);

        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();
            return UserMapper.INSTANCE.convert(user);
        } else {
            throw new UserNotFoundException("User is not found with id:" + userId);
        }
    }

    @Override
    public UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            // Retrieve user details from the database
            return UserMapper.INSTANCE.convert(userRepository.findByUsername(username).orElseThrow(()-> new UserNotFoundException("User with username " + username + " not found")));

        } else {
            // Handle case where user is not authenticated
            // You might return an error response or throw an exception
            throw new UnauthorizedException();
        }
    }

    @Override
    public List<UserHackerDTO> getAllUsers() {

      return userRepository.findAllByActivated(true)
              .stream()
              .map(userEntity -> UserMapper.INSTANCE.toDto(userEntity, userEntity.getHacker()))
              .collect(Collectors.toList());

    }
    /////////////

    public void sendActivationEmail(UserEntity user) {
        // Generate activation token and save it to the user entity
        String activationToken = generateActivationToken();
        user.setActivationToken(activationToken);
        userRepository.save(user);

        // Send activation email
        String activationLink = ROOT_LINK + "/api/auth/activate?token=" + activationToken;
        String subject = "Activate Your Account";
        String content = "Dear " + user.getFirst_name() + ",\n\n"
                + "Thank you for registering with our application. Please click the link below to activate your account:\n\n"
                + activationLink + "\n\n"
                + "Best regards,\nThe Application Team";

        emailNotificationService.sendEmail(user.getEmail(), subject, content);
    }

    private String generateActivationToken() {
        // You can implement your own token generation logic here
        // This could involve creating a unique token, saving it in the database,
        // and associating it with the user for verification during activation.
        // For simplicity, you can use a library like java.util.UUID.randomUUID().
        return UUID.randomUUID().toString();
    }


    public String findUsernameByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email);
        if (user != null) {
            return user.getUsername();
        } else {
            // Handle the case where the email is not found
            // You may throw an exception or return null based on your application's requirements
            return null;
        }
    }

    public List<CompanyEntity> getAllCompanies() {
        return companyRepository.findAll();
    }

    @Override
    public void deleteUser() {
        // Get the authenticated user's username from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // Find the user by username
        UserEntity user = userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException("User with username " + username + " not found"));

        // Manually delete associated records
        bugBountyReportRepository.deleteAllByUser(user); // Assuming repository method exists

        // Delete the user
        userRepository.delete(user);

        // Clear the authorization header
//        request.removeAttribute("Authorization"); //Not Working
    }
    //

    @Override
    public List<BugBountyProgramWithAssetTypeDTO> getAllBugBountyPrograms() {
        List<BugBountyProgramEntity> programs = programsService.getAllBugBountyProgramsAsEntity();

        // Map BugBountyProgramEntities to BugBountyProgramDTOs
    return programs.stream()
                .map(programEntity -> {
                    BugBountyProgramWithAssetTypeDTO dto = mapToDTO(programEntity);
                    dto.setCompanyId(programEntity.getCompany().getId());
//                    dto.getProgramId();
                    return dto;
                })
                .collect(Collectors.toList());

//        return programs.stream()
//                .map(ProgramMapper.INSTANCE::toDTO)
//                .collect(Collectors.toList());

    }

    @Override
    public BugBountyProgramDTO getBugBountyProgramById(Long id) {
      Optional<BugBountyProgramEntity> program = programsRepository.findById(id);

      return ProgramMapper.INSTANCE.toDto(program.orElseThrow(() -> new ResourceNotFoundException("Bug Bounty Program not found")));
    }

    @Override
    public UserEntity findUserByUsername(String username) {
      return   userRepository.findByUsername(username).orElseThrow(()-> new UserNotFoundException("User not found with this username: " + username));
    }


    public CompanyEntity getCompaniesById(Long id) {
        Optional<CompanyEntity> companyEntity = companyRepository.findById(id);
        return companyEntity.orElseThrow(() -> new ResourceNotFoundException("Company not found with id:" + id));
    }

    ///////// Util methods
    private UserEntity findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(()-> new UserNotFoundException("User not found with this id: " + userId));
    }
    private BugBountyProgramWithAssetTypeDTO mapToDTO(BugBountyProgramEntity programEntity) {
        BugBountyProgramWithAssetTypeDTO dto = new BugBountyProgramWithAssetTypeDTO();
//        dto.setId(programEntity.getId());
        dto.setFromDate(programEntity.getFromDate());
        dto.setToDate(programEntity.getToDate());
        dto.setNotes(programEntity.getNotes());
        dto.setPolicy(programEntity.getPolicy());

        // Map associated asset types
        List<AssetTypeDTO> assetTypeDTOs = programEntity.getAssetTypes().stream()
                .map(this::mapAssetTypeToDTO)
                .collect(Collectors.toList());
        dto.setAssetTypes(assetTypeDTOs);

        // You can map other fields as needed

        return dto;
    }
    private AssetTypeDTO mapAssetTypeToDTO(AssetTypeEntity assetTypeEntity) {
        AssetTypeDTO dto = new AssetTypeDTO();
//        dto.setId(assetTypeEntity.getId());
        dto.setLevel(assetTypeEntity.getLevel());
        dto.setAssetType(assetTypeEntity.getAssetType());
        dto.setPrice(assetTypeEntity.getPrice());
        dto.setProgramId(assetTypeEntity.getBugBountyProgram().getId());

        return dto;
    }
}

