package com.proyecto.servicios.repositorys.transaccion;

import com.proyecto.servicios.entity.transaccion.EstadoTransaccion;
import com.proyecto.servicios.entity.transaccion.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    Optional<Transaccion> findByIdempotenciaKey(String idempotenciaKey);

    boolean existsByIdempotenciaKey(String idempotenciaKey);

    List<Transaccion> findByEstado(EstadoTransaccion estado);

    List<Transaccion> findByCuentaId(Long cuentaId);
}
