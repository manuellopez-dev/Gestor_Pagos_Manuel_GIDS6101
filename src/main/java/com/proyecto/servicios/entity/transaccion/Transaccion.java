package com.proyecto.servicios.entity.transaccion;

import com.proyecto.servicios.entity.cuenta.Cuenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotencia_key", nullable = false, unique = true, length = 100)
    private String idempotenciaKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(name = "id_servicio", nullable = false, length = 50)
    private String idServicio;

    @Column(name = "id_producto", nullable = false, length = 50)
    private String idProducto;

    @Column(name = "nombre_producto", length = 150)
    private String nombreProducto;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "referencia", nullable = false, length = 100)
    private String referencia;

    @Column(name = "digito_verificador", length = 10)
    private String digitoVerificador;

    @Column(name = "distribuidor", length = 50)
    private String distribuidor;

    @Column(name = "id_permiso", length = 50)
    private String idPermiso;

    @Column(name = "bandera", length = 20)
    private String bandera;

    @Column(name = "upe", length = 50)
    private String upe;

    @Column(name = "unidad", length = 20)
    private String unidad;

    @Column(name = "firma_digital", columnDefinition = "TEXT")
    private String firmaDigital;

    @Column(name = "payload_cifrado", columnDefinition = "TEXT")
    private String payloadCifrado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoTransaccion estado;

    @Column(name = "folio_proveedor", length = 100)
    private String folioProveedor;

    @Column(name = "autorizacion_proveedor", length = 100)
    private String autorizacionProveedor;

    @Column(name = "codigo_respuesta_proveedor", length = 50)
    private String codigoRespuestaProveedor;

    @Column(name = "mensaje_proveedor", length = 255)
    private String mensajeProveedor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoTransaccion.EN_PROCESO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
