package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.exception.IntegracionException;
import com.proyecto.servicios.exception.TipoErrorIntegracion;
import com.proyecto.servicios.model.xml.RespuestaTransaccionXml;
import com.proyecto.servicios.service.GetTransaccionXmlService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.StringReader;

@Service
@Slf4j
public class GetTransaccionXmlServiceImpl implements GetTransaccionXmlService {

    @Override
    public RespuestaTransaccionXml parsear(String xml) {
        if (StringUtils.isBlank(xml)) {
            throw new IntegracionException(TipoErrorIntegracion.RESPUESTA_NO_EXITOSA,
                    "El servicio externo GestoPago no devolvió respuesta XML para la transacción");
        }
        try {
            JAXBContext context = JAXBContext.newInstance(RespuestaTransaccionXml.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            RespuestaTransaccionXml respuesta = (RespuestaTransaccionXml) unmarshaller.unmarshal(new StringReader(xml));
            log.info("XML de transacción procesado correctamente");
            return respuesta;
        } catch (JAXBException exception) {
            log.error("Error al procesar XML de respuesta de transacción: {}", exception.getMessage());
            throw new IntegracionException(TipoErrorIntegracion.RESPUESTA_NO_EXITOSA,
                    "No fue posible procesar la respuesta XML de la transacción de GestoPago", exception);
        }
    }
}
