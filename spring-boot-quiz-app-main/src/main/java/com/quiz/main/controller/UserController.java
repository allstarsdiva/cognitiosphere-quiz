package com.quiz.main.controller;

import com.quiz.main.model.UserDto; // Assuming you have a UserDto class
import com.quiz.main.model.User;
import com.quiz.main.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserDto());
        return "register";
    }

    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("user", new UserDto());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("user") UserDto userDto, Model model) {
        try {
            userService.registerNewUser(userDto);
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("user", userDto);
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
    }


    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    // Removed duplicate /showRegister endpoint

}
