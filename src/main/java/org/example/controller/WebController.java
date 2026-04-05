package org.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String root() {
        return "redirect:/nercon/home.html";
    }

    @RequestMapping("/")
    public String nerconRoot() {
        return "redirect:/nercon/home.html";
    }
}

