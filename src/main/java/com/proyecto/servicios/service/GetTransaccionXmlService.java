package com.proyecto.servicios.service;

import com.proyecto.servicios.model.xml.RespuestaTransaccionXml;

public interface GetTransaccionXmlService {

    RespuestaTransaccionXml parsear(String xml);
}
