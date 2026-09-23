package com.proyecto.servicios.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ProductoProperties.class, GestoPagoAuthProperties.class})
public class ProductoConfig {
}