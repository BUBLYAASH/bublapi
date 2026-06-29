package org.bublapi.dent.auth.security;

import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
   private final UserRepository userRepository;

   public CustomUserDetailsService(UserRepository userRepository) {
      this.userRepository = userRepository;
   }

   @Override
   public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
      //TODO: after adding API Key for clinic, change method findByEmail to findByEmailInClinic
      User user = userRepository.findByEmailWithRoles(username)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

      return new CustomUserDetails(user);
   }
}
