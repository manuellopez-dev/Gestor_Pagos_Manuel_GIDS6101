package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.config.GestoPagoAuthProperties;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;
    private final GestoPagoAuthProperties authProperties;

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoTokenRepository tokenRepository,
                                     GestoPagoTokenMapper tokenMapper,
                                     GestoPagoAuthProperties authProperties) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
        this.authProperties = authProperties;
    }

    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:3600000}", initialDelay = 0)
    public void renovarToken() {
        log.info("Renovando token GestoPago para distribuidor={}", authProperties.getIdDistribuidor());
        try {
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    authProperties.getIdDistribuidor(), authProperties.getCodigoDispositivo(),
                    authProperties.getPassword(), authProperties.getApiKey());

            if (response == null || response.getToken() == null) {
                log.error("La respuesta de GestoPago no contiene token");
                return;
            }

            GestoPagoToken tokenEntity = tokenRepository
                    .findByIdDistribuidorAndCodigoDispositivo(authProperties.getIdDistribuidor(),
                            authProperties.getCodigoDispositivo())
                    .map(existing -> {
                        tokenMapper.updateEntity(response, existing);
                        return existing;
                    })
                    .orElseGet(() -> {
                        GestoPagoToken nuevo = tokenMapper.toEntity(response);
                        nuevo.setIdDistribuidor(authProperties.getIdDistribuidor());
                        nuevo.setCodigoDispositivo(authProperties.getCodigoDispositivo());
                        nuevo.setActivo(true);
                        return nuevo;
                    });

            tokenRepository.save(tokenEntity);
            log.info("Token GestoPago renovado correctamente");

        } catch (Exception e) {
            log.error("Error al renovar token GestoPago: {}", e.getMessage(), e);
        }
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        try {
            Optional<GestoPagoToken> existente = tokenRepository
                    .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
            if (existente.isPresent()) {
                return existente;
            }
        } catch (Exception exception) {
            log.warn("No fue posible consultar el token persistido (la BD puede requerir migracion): {}",
                    exception.getMessage());
        }
        return renovarSoloToken(idDistribuidor, codigoDispositivo);
    }

    private Optional<GestoPagoToken> renovarSoloToken(Integer idDistribuidor, String codigoDispositivo) {
        try {
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    idDistribuidor, codigoDispositivo, authProperties.getPassword(), authProperties.getApiKey());
            if (response == null || response.getToken() == null) {
                log.error("La respuesta de GestoPago no contiene token");
                return Optional.empty();
            }
            GestoPagoToken token = tokenMapper.toEntity(response);
            token.setIdDistribuidor(idDistribuidor);
            token.setCodigoDispositivo(codigoDispositivo);
            token.setActivo(true);
            try {
                tokenRepository.save(token);
            } catch (Exception exception) {
                log.warn("No fue posible persistir el token (la BD puede requerir migracion), se devuelve en memoria: {}",
                        exception.getMessage());
            }
            return Optional.of(token);
        } catch (Exception exception) {
            log.error("Error renovando token GestoPago: {}", exception.getMessage());
            return Optional.empty();
        }
    }
}