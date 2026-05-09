package com.coworking.user.dto;

import com.coworking.user.model.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "الاسم مطلوب")
    @Size(min = 3, message = "الاسم لازم يكون 3 حروف على الأقل")
    private String username;

    @NotBlank(message = "الباسورد مطلوب")
    @Size(min = 6, message = "الباسورد ضعيف، لازم يكون 6 حروف على الأقل")
    private String password;

    @NotBlank(message = "الإيميل مطلوب")
    @Email(message = "صيغة الإيميل غير صحيحة")
    private String email;

    @NotNull(message = "الرول مطلوب")
    private Role role;
}