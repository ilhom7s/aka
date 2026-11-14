package com.analytics.service.services;

import com.analytics.service.Exceptions.NotFoundException;
import com.analytics.service.entity.User;
import com.analytics.service.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findUserByEmailOrPhone(String loginToken){
        return userRepository.findByEmail(loginToken).or(
                ()->userRepository.findByUserPhone(loginToken)).orElseThrow(()->new NotFoundException("Пользователь не найден")
        );
    }
}
