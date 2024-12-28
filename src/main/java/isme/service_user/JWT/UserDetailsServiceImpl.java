package isme.service_user.JWT;


import isme.service_user.Models.User;
import isme.service_user.Repositories.UserRepo;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl  {


    private final UserRepo userRepository;
    public UserDetailsServiceImpl(UserRepo userRepository) {
        this.userRepository = userRepository;
    }
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);


        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of()


        );
    }
}