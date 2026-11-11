CREATE TABLE IF NOT EXISTS cuentas (
    id                  BIGSERIAL       PRIMARY KEY,
    numero_cuenta       VARCHAR(50)     NOT NULL,
    usuario_id          BIGINT,
    saldo               NUMERIC(15, 2)  NOT NULL DEFAULT 0.00,
    saldo_retenido      NUMERIC(15, 2)  NOT NULL DEFAULT 0.00,
    estado              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVA',
    version             BIGINT          NOT NULL DEFAULT 0,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cuentas_numero UNIQUE (numero_cuenta),
    CONSTRAINT chk_cuentas_saldo CHECK (saldo >= 0),
    CONSTRAINT chk_cuentas_saldo_retenido CHECK (saldo_retenido >= 0)
);

CREATE INDEX IF NOT EXISTS idx_cuentas_usuario ON cuentas(usuario_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_estado ON cuentas(estado);

CREATE TABLE IF NOT EXISTS transacciones (
    id                          BIGSERIAL       PRIMARY KEY,
    idempotencia_key            VARCHAR(100)    NOT NULL,
    cuenta_id                   BIGINT          NOT NULL REFERENCES cuentas(id),
    id_servicio                 VARCHAR(50)     NOT NULL,
    id_producto                 VARCHAR(50)     NOT NULL,
    nombre_producto             VARCHAR(150),
    monto                       NUMERIC(15, 2)  NOT NULL,
    referencia                  VARCHAR(100)    NOT NULL,
    digito_verificador          VARCHAR(10),
    estado                      VARCHAR(30)     NOT NULL,
    folio_proveedor             VARCHAR(100),
    autorizacion_proveedor      VARCHAR(100),
    codigo_respuesta_proveedor  VARCHAR(50),
    mensaje_proveedor           VARCHAR(255),
    fecha_creacion              TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion         TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_transacciones_idempotencia UNIQUE (idempotencia_key),
    CONSTRAINT chk_transacciones_monto CHECK (monto > 0)
);

CREATE INDEX IF NOT EXISTS idx_transacciones_cuenta ON transacciones(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_transacciones_estado ON transacciones(estado);
CREATE INDEX IF NOT EXISTS idx_transacciones_fecha ON transacciones(fecha_creacion);
