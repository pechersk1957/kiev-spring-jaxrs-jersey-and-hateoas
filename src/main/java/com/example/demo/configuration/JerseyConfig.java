package com.example.demo.configuration;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import com.example.demo.controller.CustomOrderHateoasController;

@Component
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(CustomOrderHateoasController.class);
    }

}
