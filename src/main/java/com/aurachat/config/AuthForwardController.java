package com.aurachat.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
public class AuthForwardController {

    @RequestMapping("/auth/**")
    public void forwardAuth(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String forwardPath = "/api" + request.getRequestURI();
        request.getRequestDispatcher(forwardPath).forward(request, response);
    }
}

