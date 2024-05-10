package com.turingSecApp.turingSec.response.report;

import com.turingSecApp.turingSec.dao.entities.ReportEntity;
import com.turingSecApp.turingSec.response.user.UserDTO;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReportsByUserDTO {
   // private Long id;
    private Long userId;
    private UserDTO user;
    private boolean has_hacker_profile_pic;
    //private String userImgUrl; // Add image URL field
    private List<ReportEntity> reports;
}
