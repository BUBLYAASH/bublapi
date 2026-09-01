package org.bublapi.dent.doctor.service;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.doctor.dto.CreateDoctorRequestDto;
import org.bublapi.dent.doctor.dto.DoctorResponseDto;
import org.bublapi.dent.doctor.dto.LinkUserToDoctorRequestDto;
import org.bublapi.dent.doctor.dto.UpdateDoctorRequestDto;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor.mapper.DoctorMapper;
import org.bublapi.dent.doctor.repository.DoctorRepository;
import org.bublapi.dent.logging.AdministrativeAuditService;
import org.bublapi.dent.logging.UserAuditService;
import org.bublapi.dent.role.entity.Role;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.role.repository.RoleRepository;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class DoctorService {

   private final DoctorRepository doctorRepository;
   private final UserRepository userRepository;
   private final RoleRepository roleRepository;
   private final DoctorMapper doctorMapper;
   private final UserAuditService userAuditService;
   private final AdministrativeAuditService administrativeAuditService;

   public DoctorService(DoctorRepository doctorRepository, UserRepository userRepository, RoleRepository roleRepository,
                        DoctorMapper doctorMapper, UserAuditService userAuditService,
                        AdministrativeAuditService administrativeAuditService) {
      this.doctorRepository = doctorRepository;
      this.userRepository = userRepository;
      this.roleRepository = roleRepository;
      this.doctorMapper = doctorMapper;
      this.userAuditService = userAuditService;
      this.administrativeAuditService = administrativeAuditService;
   }

   private static List<String> getChangedFields(Doctor doctor, UpdateDoctorRequestDto request) {
      List<String> changedFields = new ArrayList<>();

      if (request.firstName() != null && !Objects.equals(doctor.getFirstName(), request.firstName())) {
         changedFields.add("firstName");
      }

      if (request.lastName() != null && !Objects.equals(doctor.getLastName(), request.lastName())) {
         changedFields.add("lastName");
      }

      if (request.middleName() != null && !Objects.equals(doctor.getMiddleName(), request.middleName())) {
         changedFields.add("middleName");
      }

      if (request.specialty() != null && !Objects.equals(doctor.getSpecialty(), request.specialty())) {
         changedFields.add("specialty");
      }

      if (request.avatarUrl() != null && !Objects.equals(doctor.getAvatarUrl(), request.avatarUrl())) {
         changedFields.add("avatarUrl");
      }

      if (request.description() != null && !Objects.equals(doctor.getDescription(), request.description())) {
         changedFields.add("description");
      }

      return changedFields;
   }

   @Transactional
   public DoctorResponseDto create(CreateDoctorRequestDto request) {
      Clinic clinic = ClinicContext.get();

      Doctor entity = doctorMapper.toEntity(request);

      entity.setClinic(clinic);

      Doctor saved = doctorRepository.save(entity);

      userAuditService.doctorCreated(saved.getId());

      return doctorMapper.toResponse(saved);
   }

   @Transactional
   public DoctorResponseDto update(UUID doctorId, UpdateDoctorRequestDto request) {
      UUID clinicId = ClinicContext.getClinicId();

      Doctor doctor = doctorRepository.findByClinic_IdAndId(clinicId, doctorId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Doctor in clinic not found"));

      List<String> changedFields = getChangedFields(doctor, request);

      doctorMapper.updateEntity(request, doctor);

      userAuditService.doctorUpdated(doctorId, changedFields);

      return doctorMapper.toResponse(doctor);
   }

   @Transactional
   public DoctorResponseDto deactivate(UUID doctorId) {
      UUID clinicId = ClinicContext.getClinicId();

      Doctor doctor = doctorRepository.findByClinic_IdAndId(clinicId, doctorId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found in this clinic"));

      doctor.setActive(false);

      userAuditService.doctorDeactivated(doctorId);

      return doctorMapper.toResponse(doctor);
   }

   @Transactional
   public DoctorResponseDto activate(UUID doctorId) {
      UUID clinicId = ClinicContext.getClinicId();

      Doctor doctor = doctorRepository.findByClinic_IdAndId(clinicId, doctorId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found in this clinic"));

      doctor.setActive(true);

      userAuditService.doctorActivated(doctorId);

      return doctorMapper.toResponse(doctor);
   }

   public List<DoctorResponseDto> findAllForStaff() {
      UUID clinicId = ClinicContext.getClinicId();

      return doctorRepository.findAllByClinic_Id(clinicId).stream().map(doctorMapper::toResponse).toList();
   }

   public List<DoctorResponseDto> findAllActiveForPublic() {
      UUID clinicId = ClinicContext.getClinicId();

      return doctorRepository.findAllByClinic_IdAndActiveTrue(clinicId).stream().map(doctorMapper::toResponse).toList();
   }

   public DoctorResponseDto findById(UUID doctorId) {
      UUID clinicId = ClinicContext.getClinicId();

      Doctor doctor = doctorRepository.findByClinic_IdAndId(clinicId, doctorId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found in this clinic"));

      return doctorMapper.toResponse(doctor);
   }

   public DoctorResponseDto findActiveById(UUID doctorId) {
      UUID clinicId = ClinicContext.getClinicId();

      Doctor doctor = doctorRepository.findByClinic_IdAndIdAndActiveTrue(clinicId, doctorId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found or unavailable"));

      return doctorMapper.toResponse(doctor);
   }

   @Transactional
   public DoctorResponseDto linkUser(UUID doctorId, LinkUserToDoctorRequestDto request) {
      UUID clinicId = ClinicContext.getClinicId();

      String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
      String phone = request.phone() == null ? "" : request.phone().trim();

      if (email.isBlank() && phone.isBlank()) {
         throw new BadRequestException("Email and phone are empty");
      }

      User user = userRepository.findByEmailOrPhoneInClinic(email, phone, clinicId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

      Doctor doctor = doctorRepository.findByClinic_IdAndIdAndActiveTrue(clinicId, doctorId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found or unavailable"));

      Role doctorRole = roleRepository.findByName(RoleName.DOCTOR)
                                      .orElseThrow(() -> new ResourceNotFoundException("Role DOCTOR not found"));

      if (doctor.getUser() != null) {
         throw new BadRequestException("Doctor profile is already linked to a user");
      }

      doctorRepository.findByClinic_IdAndUser_Id(clinicId, user.getId()).ifPresent(e -> {
         throw new BadRequestException("User is already linked to another doctor profile");
      });

      doctor.setUser(user);
      user.getRoles().add(doctorRole);

      userAuditService.doctorUpdated(doctor.getId(), List.of("userId"));

      administrativeAuditService.roleGranted(user.getId(), clinicId, RoleName.DOCTOR.name());

      return doctorMapper.toResponse(doctor);
   }

   @Transactional
   public DoctorResponseDto unlinkUser(UUID doctorId) {
      UUID clinicId = ClinicContext.getClinicId();

      Doctor doctor = doctorRepository.findByClinic_IdAndId(clinicId, doctorId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

      User user = doctor.getUser();

      if (user == null) {
         throw new BadRequestException("Doctor profile is not linked to a user");
      }

      doctor.setUser(null);

      user.getRoles().removeIf(role -> role.getName() == RoleName.DOCTOR);

      userAuditService.doctorUpdated(doctor.getId(), List.of("userId"));

      administrativeAuditService.roleRevoked(user.getId(), clinicId, RoleName.DOCTOR.name());

      return doctorMapper.toResponse(doctor);
   }
}
