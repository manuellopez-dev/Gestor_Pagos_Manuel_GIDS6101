CREATE TABLE IF NOT EXISTS usuarios (
    id                  BIGSERIAL       PRIMARY KEY,
    username            VARCHAR(50)     NOT NULL,
    nombre              VARCHAR(100)    NOT NULL,
    apellidos           VARCHAR(100)    NOT NULL,
    email               VARCHAR(150)    NOT NULL,
    telefono            VARCHAR(20),
    estado              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVO',
    roles               VARCHAR(100)    NOT NULL DEFAULT 'ROLE_USER',
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usuarios_email UNIQUE (email),
    CONSTRAINT uq_usuarios_username UNIQUE (username)
);

CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);
CREATE INDEX IF NOT EXISTS idx_usuarios_username ON usuarios(username);
CREATE INDEX IF NOT EXISTS idx_usuarios_estado ON usuarios(estado);
