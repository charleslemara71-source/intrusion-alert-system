package com.ias.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String landing() {
        return "landing";      // opens templates/landing.html
    }

    @GetMapping("/login")
    public String login() {
        return "login";        // opens templates/login.html
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";    // opens templates/dashboard.html
    }
}