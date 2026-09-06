package org.bublapi.dent.clinic.controller.publicapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Public Clinic")
@RestController
@RequestMapping("/api/public/clinic")
public class PublicClinicController {

   @Operation(summary = "Show clinic timezone")
   @GetMapping("/timezone")
   public Map<String, String> timezone() {
      Clinic clinic = ClinicContext.get();

      if (clinic == null) {
         throw new ResourceNotFoundException("Clinic not found");
      }

      return Map.of("timezone", clinic.getTimezone());
   }
}
