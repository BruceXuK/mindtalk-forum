package com.mindtalk.forum.modules.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 更新个人资料请求
 */
@Data
public class UpdateProfileDTO {

    @Size(max = 50, message = "昵称最长 50 个字符")
    private String nickname;

    @Size(max = 500, message = "简介最长 500 个字符")
    private String bio;

    private Integer gender;

    private LocalDate birthday;

    @Size(max = 100, message = "所在地最长 100 个字符")
    private String location;
}
