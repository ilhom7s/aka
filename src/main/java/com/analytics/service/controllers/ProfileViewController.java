package com.analytics.service.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileViewController {
    // /profile тоже открывает статическую страницу (данные тянет через REST /profile/*)
    @GetMapping("/profile")
    public String profilePage() {
        return "forward:/profile.html";
    }
}
