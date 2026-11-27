package com.homeseek.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The password policy from Increment 1 (8-72 standard characters, at least one letter and one number). */
@NotBlank(message = "Password is required")
@Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
@Pattern(regexp = "^[\\x20-\\x7E]*$", message = "Password may only use standard keyboard characters")
@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "Password must contain at least one letter and one number")
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "Invalid password";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
