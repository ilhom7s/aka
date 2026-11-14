package com.analytics.service.controllers;

import com.analytics.service.dto.*;
import com.analytics.service.repository.UserMapper;
import com.analytics.service.services.ProfileService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile")
@AllArgsConstructor
public class ProfileController {
   private final ProfileService profileService;
   private final UserMapper mapper;

    @GetMapping("/my-orders")
    public List<OrderDto> myOrders(Authentication authentication){
        String login = authentication.getName();
        return profileService.findOrders(login).stream().map(mapper::toOrderDto).toList();
    }

    @GetMapping("/active")
    public List<OrderDto> myActiveOrders(Authentication authentication){
        String login = authentication.getName();
        return profileService.findActiveUserOrders(login).stream().map(mapper::toOrderDto).toList();
}
    @PostMapping("/change")
    public void changePassword(@RequestBody ChangePasswordRequestDto dto,Authentication authentication){
        String login = authentication.getName();
        profileService.changePassword(dto,login);
}
    @PostMapping("/emailch")
    public void changeEmail(@RequestBody ChangeEmailRequestDto dto,Authentication authentication){
        String login = authentication.getName();
        profileService.changeEmail(dto,login);
}
    @PostMapping("/phonech")
    public void changePhone(@RequestBody ChngePhoneRequestDto dto,Authentication authentication){
        String login = authentication.getName();
        profileService.changePhone(dto,login);
}
    @GetMapping("/info")
    public UserInfoResponseDto info(Authentication authentication){
        String login = authentication.getName();
        return profileService.getUserInfo(login);
    }
}
