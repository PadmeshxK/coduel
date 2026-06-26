package com.coduel.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageEditForm {

    @NotBlank(message = "must not be blank")
    @Size(max = 2000, message = "exceeds the maximum allowed size")
    private String body;
}
