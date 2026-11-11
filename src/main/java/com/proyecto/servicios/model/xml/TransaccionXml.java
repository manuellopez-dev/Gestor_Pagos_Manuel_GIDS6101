package com.proyecto.servicios.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class TransaccionXml {

    @XmlAttribute(name = "autorizacion")
    private String autorizacionAttr;

    @XmlElement(name = "AUTORIZACION")
    private String autorizacionElem;

    @XmlAttribute(name = "folio")
    private String folioAttr;

    @XmlElement(name = "FOLIO")
    private String folioElem;

    @XmlAttribute(name = "idTransaccion")
    private String idTransaccionAttr;

    @XmlElement(name = "IDTRANSACCION")
    private String idTransaccionElem;

    public String getAutorizacion() {
        return autorizacionAttr != null ? autorizacionAttr : autorizacionElem;
    }

    public String getFolio() {
        return folioAttr != null ? folioAttr : folioElem;
    }

    public String getIdTransaccion() {
        return idTransaccionAttr != null ? idTransaccionAttr : idTransaccionElem;
    }
}
