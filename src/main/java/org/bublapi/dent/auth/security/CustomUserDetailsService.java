package org.bublapi.dent.auth.security;

import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {
   private final UserRepository userRepository;

   public CustomUserDetailsService(UserRepository userRepository) {
      this.userRepository = userRepository;
   }

   @Override
   public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
      UUID clinicId = ClinicContext.getClinicId();

      User user = userRepository.findByEmailWithRolesInClinic(username, clinicId)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

      return new CustomUserDetails(user);
   }

   public UserDetails loadUserByUserId(UUID id) throws UsernameNotFoundException {
      User user = userRepository.findByIdWithRoles(id)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

      return new CustomUserDetails(user);
   }
}
