package com.turingSecApp.turingSec.background_file_upload_for_hacker.service;

import com.turingSecApp.turingSec.background_file_upload_for_hacker.entity.BackgroundImageForHacker;
import com.turingSecApp.turingSec.background_file_upload_for_hacker.exception.FileNotFoundException;
import com.turingSecApp.turingSec.background_file_upload_for_hacker.repository.FileRepository;
import com.turingSecApp.turingSec.background_file_upload_for_hacker.response.FileResponse;
import com.turingSecApp.turingSec.dao.entities.HackerEntity;
import com.turingSecApp.turingSec.dao.entities.user.UserEntity;
import com.turingSecApp.turingSec.dao.repository.HackerRepository;
import com.turingSecApp.turingSec.dao.repository.UserRepository;
import com.turingSecApp.turingSec.exception.custom.UnauthorizedException;
import com.turingSecApp.turingSec.exception.custom.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileService implements IFileService {
    private final ModelMapper modelMapper;
    private final FileRepository fileRepository;

    private final HackerRepository hackerRepository;
    private final UserRepository userRepository;

    public FileResponse saveVideoOrImg(MultipartFile multipartFile, Long hackerId) throws IOException {
        BackgroundImageForHacker existingFile = getExistingFileOrEmpty(hackerId);
        HackerEntity hacker = getHackerById(hackerId);

        BackgroundImageForHacker updatedFile = updateFileInfo(existingFile, multipartFile,hackerId);

        setHackerPicturesTrueAndSave(hacker);

        return mapToFileResponse(fileRepository.save(updatedFile));
    }

    private BackgroundImageForHacker getExistingFileOrEmpty(Long hackerId) {
        return fileRepository.findBackgroundImageForHackerByHackerId(hackerId).orElse(new BackgroundImageForHacker());
    }

    private BackgroundImageForHacker updateFileInfo(BackgroundImageForHacker existingFile, MultipartFile multipartFile , Long hackerId) throws IOException {
        existingFile.setName(multipartFile.getOriginalFilename());
        existingFile.setContentType(multipartFile.getContentType());
        existingFile.setFileData(multipartFile.getBytes());
        existingFile.setHackerId(hackerId);

        return existingFile;
    }

    private HackerEntity getHackerById(Long hackerId) {
        return hackerRepository.findById(hackerId)
                .orElseThrow(() -> new UserNotFoundException("Hacker not found with id:" + hackerId));
    }

    private FileResponse mapToFileResponse(BackgroundImageForHacker file) {
        return modelMapper.map(file, FileResponse.class);
    }
    @Override
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

    @Override
    public BackgroundImageForHacker getVideoById(Long hackerId) throws FileNotFoundException {
        return fileRepository.findBackgroundImageForHackerByHackerId(hackerId).orElseThrow(
                () -> new FileNotFoundException("File cannot found by hackerId:" + hackerId));
    }
    // Util
    private void setHackerPicturesTrueAndSave(HackerEntity hacker) {
        hacker.setHas_profile_pic(true);
//        hacker.setHas_background_pic(true); // for background there is other method
        hackerRepository.save(hacker);
    }
}