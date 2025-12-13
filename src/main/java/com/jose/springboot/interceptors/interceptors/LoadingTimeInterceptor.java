package com.jose.springboot.interceptors.interceptors;

import java.util.Random;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component("timeInterceptor")
public class LoadingTimeInterceptor implements HandlerInterceptor{

    private static final Logger logger = LoggerFactory.getLogger(LoadingTimeInterceptor.class);
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    throws Exception{
        HandlerMethod method = (HandlerMethod) handler;
        String nombreMethod = method.getMethod().getName();
        long startTime = System.currentTimeMillis();
        request.setAttribute("start", startTime);
        Random random = new Random();
        Thread.sleep(random.nextInt(500));
        logger.info("LoadingTimeInterceptor: preHandle() entrando en el método " + nombreMethod + "...");
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            @Nullable ModelAndView modelAndView) 
    throws Exception{
        HandlerMethod method = (HandlerMethod) handler;
        String nombreMethod = method.getMethod().getName();
        long timeTaken = System.currentTimeMillis() - (long)request.getAttribute("start");
        logger.info("LoadingTimeInterceptor: postHandle() saliendo del método " + nombreMethod + "...");
        logger.info("LoadingTimeInterceptor: El tiempo de carga del método " + nombreMethod + " es de " + timeTaken + " ms");
    }

    
}
