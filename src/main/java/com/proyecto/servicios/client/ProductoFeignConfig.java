package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoAuthProperties;
import com.proyecto.servicios.config.ProductoProperties;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.IntegracionException;
import com.proyecto.servicios.exception.TipoErrorIntegracion;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.Request;
import feign.RequestInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;

public class ProductoFeignConfig {

    public static final String HEADER_AUTH = "Authorization";
    public static final String HEADER_X_API_KEY = "X-API-Key";
    public static final String PREFIX_BEARER = "Bearer ";

    @Bean
    public RequestInterceptor productoBearerTokenInterceptor(ProductoProperties propiedades,
                                                             GestoPagoTokenService gestoPagoTokenService,
                                                             GestoPagoAuthProperties authProperties) {
        return requestTemplate -> {
            String token = resolverToken(propiedades, gestoPagoTokenService, authProperties);
            requestTemplate.header(HEADER_AUTH, PREFIX_BEARER + token);
            requestTemplate.header(HEADER_X_API_KEY, authProperties.getApiKey());
        };
    }

    @Bean
    @SuppressWarnings("deprecation")
    public Request.Options productoRequestOptions(ProductoProperties propiedades) {
        return new Request.Options(propiedades.getConnectTimeoutMs(), propiedades.getReadTimeoutMs());
    }

    private String resolverToken(ProductoProperties propiedades,
                                 GestoPagoTokenService gestoPagoTokenService,
                                 GestoPagoAuthProperties authProperties) {
        if (StringUtils.isNotBlank(propiedades.getToken())) {
            return propiedades.getToken();
        }
        return gestoPagoTokenService
                .obtenerTokenActivo(authProperties.getIdDistribuidor(), authProperties.getCodigoDispositivo())
                .map(GestoPagoToken::getToken)
                .orElseThrow(() -> new IntegracionException(TipoErrorIntegracion.ERROR_AUTENTICACION,
                        "No hay un token GestoPago disponible para consumir el servicio de productos"));
    }
}