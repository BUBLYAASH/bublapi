package org.bublapi.dent.clinic.service;

import org.bublapi.dent.clinic.dto.ClinicOriginResponseDto;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.entity.ClinicAllowedOrigin;
import org.bublapi.dent.clinic.repository.ClinicAllowedOriginRepository;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.logging.audit.AdministrativeAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClinicOriginService {
   private final ClinicAllowedOriginRepository origins;
   private final ClinicRepository clinics;
   private final AdministrativeAuditService audit;

   public ClinicOriginService(ClinicAllowedOriginRepository origins, ClinicRepository clinics,
                              AdministrativeAuditService audit) {
      this.origins = origins;
      this.clinics = clinics;
      this.audit = audit;
   }

   @Transactional(readOnly = true)
   public List<ClinicOriginResponseDto> list(UUID clinicId) {
      clinic(clinicId);
      return origins.findAllByClinic_IdOrderByOrigin(clinicId)
                    .stream()
                    .map(o -> new ClinicOriginResponseDto(o.getId(), o.getOrigin()))
                    .toList();
   }

   @Transactional
   public ClinicOriginResponseDto add(UUID clinicId, String value) {
      Clinic clinic = clinic(clinicId);
      String origin = WebsiteOrigin.requireOrigin(value);

      Optional<ClinicAllowedOrigin> existing = origins.findByClinic_IdAndOrigin(clinicId, origin);

      if (existing.isPresent()) {
         ClinicAllowedOrigin entry = existing.get();
         return new ClinicOriginResponseDto(entry.getId(), entry.getOrigin());
      }

      ClinicAllowedOrigin entry = new ClinicAllowedOrigin();
      entry.setClinic(clinic);
      entry.setOrigin(origin);

      entry = origins.saveAndFlush(entry);

      audit.clinicUpdated(clinicId, List.of("allowedOrigins"));

      return new ClinicOriginResponseDto(entry.getId(), entry.getOrigin());
   }

   @Transactional
   public void remove(UUID clinicId, UUID originId) {
      ClinicAllowedOrigin entry = origins.findByIdAndClinic_Id(originId, clinicId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Clinic origin not found"));
      origins.delete(entry);
      audit.clinicUpdated(clinicId, List.of("allowedOrigins"));
   }

   private Clinic clinic(UUID id) {
      return clinics.findById(id).orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));
   }
}
