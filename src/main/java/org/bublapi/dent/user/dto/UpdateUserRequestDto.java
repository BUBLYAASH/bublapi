package org.bublapi.dent.user.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequestDto(@Email String email,

                                   @Pattern(regexp = "^\\d{10,15}$") @Size(max = 15) String phone,

                                   @Size(max = 50) String firstName,

                                   @Size(max = 50) String lastName,

                                   @Size(max = 50) String middleName,

                                   @Size(min = 8, max = 72) String password) {

}
