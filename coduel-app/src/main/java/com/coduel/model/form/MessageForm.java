package com.coduel.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageForm {

    @NotNull(message = "recipientUserId is required")
    private Long recipientUserId;

    @NotBlank(message = "must not be blank")
    @Size(max = 2000, message = "exceeds the maximum allowed size")
    private String body;
}
