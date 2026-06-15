package org.bublapi.dent.user.service;

import java.util.UUID;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.bublapi.dent.user.dto.CreateUserRequestDto;
import org.bublapi.dent.user.dto.UpdateUserRequestDto;
import org.bublapi.dent.user.dto.UserResponseDto;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.mapper.UserMapper;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final ClinicRepository clinicRepository;
  private final UserMapper userMapper;

  public UserService(UserRepository userRepository, ClinicRepository clinicRepository,
      UserMapper userMapper) {
    this.userRepository = userRepository;
    this.clinicRepository = clinicRepository;
    this.userMapper = userMapper;
  }

  public UserResponseDto create(CreateUserRequestDto request) {
    Clinic clinic = clinicRepository.findById(request.clinicId())
        .orElseThrow(() -> new RuntimeException("Clinic with provided ID not found"));

    String hash = BCrypt.hashpw(request.password(), BCrypt.gensalt(12));

    User user = userMapper.toEntity(request);
    user.setClinic(clinic);
    user.setPasswordHash(hash);

    User saved = userRepository.save(user);

    return userMapper.toResponse(saved);
  }

  public UserResponseDto update(UUID id, UpdateUserRequestDto request) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("User not found"));

    userMapper.updateEntity(request, user);

    if (request.password() != null && !request.password().isBlank()) {
      String hash = BCrypt.hashpw(request.password(), BCrypt.gensalt(12));
      user.setPasswordHash(hash);
    }

    User saved = userRepository.save(user);

    return userMapper.toResponse(saved);
  }
}
