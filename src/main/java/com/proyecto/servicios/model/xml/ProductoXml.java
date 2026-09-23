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
public class ProductoXml {

    @XmlAttribute(name = "servicio")
    private String servicio;

    @XmlAttribute(name = "producto")
    private String producto;

    @XmlAttribute(name = "idServicio")
    private String idServicio;

    @XmlAttribute(name = "idProducto")
    private String idProducto;

    @XmlAttribute(name = "idCatTipoServicio")
    private String idCatTipoServicio;

    @XmlAttribute(name = "tipoFront")
    private String tipoFront;

    @XmlAttribute(name = "hasDigitoVerificador")
    private String hasDigitoVerificador;

    @XmlAttribute(name = "precio")
    private String precio;

    @XmlAttribute(name = "showAyuda")
    private String showAyuda;

    @XmlAttribute(name = "tipoReferencia")
    private String tipoReferencia;

    @XmlElement(name = "legend")
    private String legend;
}