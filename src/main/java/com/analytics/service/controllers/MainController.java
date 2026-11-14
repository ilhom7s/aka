package com.analytics.service.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class MainController {
    // Thymeleaf в проекте нет — отдаём статический main.html из resources/static
    @GetMapping
    public String main() {
        return "forward:/main.html";
    }
}
