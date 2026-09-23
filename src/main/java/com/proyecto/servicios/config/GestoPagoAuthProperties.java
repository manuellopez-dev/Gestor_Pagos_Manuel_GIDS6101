package com.proyecto.servicios.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "gestopago.auth")
public class GestoPagoAuthProperties {

    private String url;

    private Integer idDistribuidor;

    private String codigoDispositivo;

    private String password;

    private String apiKey;

    private Long refreshRateMs = 3600000L;
}