package com.proyecto.servicios.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class RespuestaTransaccionXml {

    @XmlElement(name = "MENSAJE")
    private MensajeXml mensaje;

    @XmlElement(name = "TRANSACCION")
    private TransaccionXml transaccion;
}
