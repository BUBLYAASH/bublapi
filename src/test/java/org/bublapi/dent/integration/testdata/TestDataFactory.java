package org.bublapi.dent.integration.testdata;

import lombok.RequiredArgsConstructor;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.clinic_service.repository.ClinicServiceRepository;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.dental_service.entity.DentalService;
import org.bublapi.dent.dental_service.entity.ServiceCategory;
import org.bublapi.dent.dental_service.repository.DentalServiceRepository;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor.repository.DoctorRepository;
import org.bublapi.dent.patient.entity.Patient;
import org.bublapi.dent.patient.repository.PatientRepository;
import org.bublapi.dent.role.entity.Role;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.role.repository.RoleRepository;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TestDataFactory {

   public static final String DEFAULT_PASSWORD = "password123";

   private final UserRepository userRepository;
   private final PasswordEncoder passwordEncoder;
   private final ClinicRepository clinicRepository;
   private final RoleRepository roleRepository;
   private final DoctorRepository doctorRepository;
   private final PatientRepository patientRepository;
   private final DentalServiceRepository dentalServiceRepository;
   private final ClinicServiceRepository clinicServiceRepository;

   public User createUser(Clinic clinic, String email) {
      return createUserWithRoles(clinic, email, RoleName.PATIENT);
   }

   public User createUserWithPhone(Clinic clinic, String email, String phone) {
      Role role = roleRepository.findByName(RoleName.PATIENT)
                                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

      User user = new User();
      user.setEmail(email);
      user.setPhone(phone);
      user.setFirstName("Test");
      user.setLastName("User");
      user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
      user.setClinic(clinic);
      user.setRoles(Set.of(role));
      user.setEnabled(true);

      return userRepository.save(user);
   }

   public User createUserWithRoles(Clinic clinic, String email, RoleName... roleNames) {
      Set<Role> roles = Arrays.stream(roleNames)
                              .map(name -> roleRepository.findByName(name).orElseThrow())
                              .collect(Collectors.toCollection(HashSet::new));

      User user = new User();
      user.setEmail(email);
      user.setPhone(UUID.randomUUID().toString().replace("-", "").substring(0, 15));
      user.setFirstName("Test");
      user.setLastName("User");
      user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
      user.setClinic(clinic);
      user.setRoles(roles);
      user.setEnabled(true);

      return userRepository.save(user);
   }

   public User createDisabledUser(Clinic clinic, String email) {
      User user = createUser(clinic, email);
      user.setEnabled(false);

      return userRepository.save(user);
   }

   public Clinic createClinic() {
      Clinic clinic = new Clinic();
      clinic.setTitle("Test clinic " + UUID.randomUUID());
      clinic.setAddress("Test address");
      clinic.setActive(true);
      return clinicRepository.save(clinic);
   }

   public Doctor createDoctor(Clinic clinic) {
      Doctor doctor = new Doctor();
      doctor.setClinic(clinic);
      doctor.setFirstName("Dr");
      doctor.setLastName("House");
      doctor.setSpecialty("Dentistry");
      doctor.setActive(true);
      return doctorRepository.save(doctor);
   }

   public Doctor createInactiveDoctor(Clinic clinic) {
      Doctor doctor = new Doctor();
      doctor.setClinic(clinic);
      doctor.setFirstName("Dr");
      doctor.setLastName("Inactive");
      doctor.setSpecialty("Dentistry");
      doctor.setActive(false);
      return doctorRepository.save(doctor);
   }

   public Patient createPatient(Clinic clinic) {
      Patient patient = new Patient();
      patient.setClinic(clinic);
      patient.setFirstName("Patient");
      patient.setLastName("Test");
      patient.setPhone(UUID.randomUUID().toString().replace("-", "").substring(0, 15));
      patient.setActive(true);
      return patientRepository.save(patient);
   }

   public Patient createPatientForUser(Clinic clinic, User user) {
      Patient patient = new Patient();
      patient.setClinic(clinic);
      patient.setUser(user);
      patient.setFirstName(user.getFirstName());
      patient.setLastName(user.getLastName());
      patient.setPhone(UUID.randomUUID().toString().replace("-", "").substring(0, 15));
      patient.setActive(true);
      return patientRepository.save(patient);
   }

   public DentalService createDentalService() {
      DentalService ds = new DentalService();
      ds.setTitle("Test Service " + UUID.randomUUID());
      ds.setCategory(ServiceCategory.THERAPY);
      ds.setDefaultDurationMinutes(30);
      ds.setActive(true);
      return dentalServiceRepository.save(ds);
   }

   public ClinicService createClinicService(Clinic clinic) {
      DentalService ds = createDentalService();
      ClinicService cs = new ClinicService();
      cs.setClinic(clinic);
      cs.setDentalService(ds);
      cs.setPrice(1000);
      cs.setDurationMinutes(30);
      cs.setActive(true);
      return clinicServiceRepository.save(cs);
   }

   public ClinicService createInactiveClinicService(Clinic clinic) {
      DentalService ds = createDentalService();
      ClinicService cs = new ClinicService();
      cs.setClinic(clinic);
      cs.setDentalService(ds);
      cs.setPrice(500);
      cs.setDurationMinutes(30);
      cs.setActive(false);
      return clinicServiceRepository.save(cs);
   }

   public Role getRole(RoleName roleName) {
      return roleRepository.findByName(roleName).orElseThrow();
   }
}
