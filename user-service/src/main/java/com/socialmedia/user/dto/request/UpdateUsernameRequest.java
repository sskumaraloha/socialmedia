package com.socialmedia.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUsernameRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_.]{3,32}$",
                message = "username must be 3-32 characters: letters, digits, '_' or '.'") String username
) {
}
