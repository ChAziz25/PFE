package com.PFE.backend.services;

import com.PFE.backend.models.User;
import com.PFE.backend.repositories.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void createUser(String name, String email, String rawPassword){
        User user = new User(name, email, rawPassword);
        userRepository.save(user);
    }
}
