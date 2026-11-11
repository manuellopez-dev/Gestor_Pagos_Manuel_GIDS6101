package com.proyecto.servicios.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesCryptoServiceImplTest {

    private AesCryptoServiceImpl cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new AesCryptoServiceImpl(
                "MiClaveSuperSeguraAES256Test2026!",
                "MiClaveFirmaHmacSha256Test2026!"
        );
    }

    @Test
    void firmar_generaFirmaHmacValida() {
        String payload = "idCuenta=CTA-001|idServicio=56|monto=100.50";
        String firma1 = cryptoService.firmar(payload);
        String firma2 = cryptoService.firmar(payload);

        assertThat(firma1).isNotBlank();
        assertThat(firma1).isEqualTo(firma2); // Determinista con la misma clave
    }

    @Test
    void cifrarYDescifrarAes_retornaTextoOriginalExacto() {
        String textoOriginal = "idCuenta=CTA-001|idServicio=56|telefonoReferencia=5511223344|montoPago=100.50|upe=UPE-01|unidad=1";

        String cifrado = cryptoService.cifrarAes(textoOriginal);

        assertThat(cifrado).isNotBlank();
        assertThat(cifrado).isNotEqualTo(textoOriginal);

        String descifrado = cryptoService.descifrarAes(cifrado);

        assertThat(descifrado).isEqualTo(textoOriginal);
    }

    @Test
    void descifrarAes_cuandoTextoEsInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> cryptoService.descifrarAes("textoNoCifrado"))
                .isInstanceOf(Exception.class);
    }
}
