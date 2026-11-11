package com.proyecto.servicios.service;

public interface CryptoService {

    String firmar(String contenido);

    String cifrarAes(String contenido);

    String descifrarAes(String cifrado);
}
