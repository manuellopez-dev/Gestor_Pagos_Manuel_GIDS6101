package com.proyecto.servicios.repositorys.producto;

import com.proyecto.servicios.entity.producto.Producto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductoRepository extends MongoRepository<Producto, String> {

    Optional<Producto> findByIdProducto(String idProducto);
}