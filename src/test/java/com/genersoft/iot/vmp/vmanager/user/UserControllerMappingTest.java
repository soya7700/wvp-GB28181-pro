package com.genersoft.iot.vmp.vmanager.user;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserControllerMappingTest {

    @Test
    void loginSupportsGetAndPost() throws Exception {
        Method login = UserController.class.getMethod(
                "login",
                javax.servlet.http.HttpServletRequest.class,
                javax.servlet.http.HttpServletResponse.class,
                String.class,
                String.class
        );

        RequestMapping mapping = login.getAnnotation(RequestMapping.class);
        assertEquals(new HashSet<>(Arrays.asList(RequestMethod.GET, RequestMethod.POST)),
                new HashSet<>(Arrays.asList(mapping.method())));
        assertEquals(Collections.singleton("/login"),
                new HashSet<>(Arrays.asList(mapping.value())));
    }
}
