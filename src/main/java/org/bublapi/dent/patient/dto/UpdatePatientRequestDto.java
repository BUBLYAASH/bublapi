package org.bublapi.dent.patient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdatePatientRequestDto(@Size(max = 50) String firstName,

                                      @Size(max = 50) String lastName,

                                      @Size(max = 50) String middleName,

                                      @Size(max = 15) @Pattern(regexp = "^\\d{10,15}$") String phone,

                                      @Email String email,

                                      LocalDate birthDate,

                                      String notes,

                                      String allergies,

                                      String chronicDiseases) {

}
