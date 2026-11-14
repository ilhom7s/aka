package com.analytics.service.services;

import com.analytics.service.dto.ChangeEmailRequestDto;
import com.analytics.service.dto.ChangePasswordRequestDto;
import com.analytics.service.dto.ChngePhoneRequestDto;
import com.analytics.service.dto.UserInfoResponseDto;
import com.analytics.service.entity.Order;
import com.analytics.service.entity.User;
import com.analytics.service.repository.OrderRepository;
import com.analytics.service.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@AllArgsConstructor
public class ProfileService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public List<Order> findOrders(String login){
        User user = userService.findUserByEmailOrPhone(login);
        return orderRepository.findUserOrders(user.getId());
    }

    public List<Order> findActiveUserOrders(String login) {
        User user = userService.findUserByEmailOrPhone(login);
        return orderRepository.findActiveUserOrders(user.getId());
    }

    @Transactional
    public void changePassword(ChangePasswordRequestDto changePassword,String login){
        User user = userService.findUserByEmailOrPhone(login);
        if(!passwordEncoder.matches(changePassword.password(), user.getPassword())){throw new RuntimeException("Пароль неверный");}
        if (!changePassword.newPassword().equals(changePassword.repeatNewPassword())) {
            throw new IllegalArgumentException("Новый пароль и повтор не совпадают");
        }
        if (changePassword.newPassword().equals(changePassword.password())) {
            throw new IllegalArgumentException("Новый пароль должен отличаться от текущего");
        }
        user.setPassword(passwordEncoder.encode(changePassword.newPassword()));
    }

    @Transactional
    public void changeEmail(ChangeEmailRequestDto changeEmail, String login) {
        User user = userService.findUserByEmailOrPhone(login);
        if(!passwordEncoder.matches(changeEmail.password(), user.getPassword())){throw new RuntimeException("Пароль неверный");}
        if(changeEmail.newEmail().equals(user.getEmail())){
            throw new RuntimeException("Новая почта должна отличаться от старой");
        }
        user.setEmail(changeEmail.newEmail());
    }
    @Transactional
    public void changePhone(ChngePhoneRequestDto changePhone, String login){
        User user = userService.findUserByEmailOrPhone(login);
        if(!passwordEncoder.matches(changePhone.password(), user.getPassword())){throw new RuntimeException("Пароль неверный");}
        if(changePhone.newPhone().equals(user.getUserPhone())){
            throw new RuntimeException("Новый номер должен отличаться от старой");
        }
        user.setUserPhone(changePhone.newPhone());
    }

    public UserInfoResponseDto getUserInfo(String login) {
        User user = userService.findUserByEmailOrPhone(login);
        return UserInfoResponseDto.builder().email(user.getEmail()).phone(user.getUserPhone()).build();
    }
}
