package com.proyecto.servicios.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "gestopago.productos")
public class ProductoProperties {

    private String url;

    private String token;

    private Integer connectTimeoutMs = 5000;

    private Integer readTimeoutMs = 10000;
}