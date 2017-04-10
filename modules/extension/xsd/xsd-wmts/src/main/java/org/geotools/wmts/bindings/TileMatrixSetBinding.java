package org.geotools.wmts.bindings;

import org.geotools.wmts.WMTS;
import org.geotools.xml.*;
import org.geotools.xml.AbstractSimpleBinding;

import net.opengis.ows11.BoundingBoxType;
import net.opengis.ows11.CodeType;
import net.opengis.wmts.v_11.TileMatrixSetType;
import net.opengis.wmts.v_11.wmts11Factory;

import javax.xml.namespace.QName;

/**
 * Binding object for the element http://www.opengis.net/wmts/1.0:TileMatrixSet.
 *
 * <p>
 * 
 * <pre>
 *	 <code>
 *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;element name="TileMatrixSet" xmlns="http://www.w3.org/2001/XMLSchema"&gt;
 *  		&lt;annotation&gt;
 *  			&lt;documentation&gt;Describes a particular set of tile matrices.&lt;/documentation&gt;
 *  		&lt;/annotation&gt;
 *  		&lt;complexType&gt;
 *  			&lt;complexContent&gt;
 *  				&lt;extension base="ows:DescriptionType"&gt;
 *  					&lt;sequence&gt;
 *  						&lt;element ref="ows:Identifier"&gt;
 *  							&lt;annotation&gt;
 *  								&lt;documentation&gt;Tile matrix set identifier&lt;/documentation&gt;
 *  							&lt;/annotation&gt;
 *  						&lt;/element&gt;
 *  						&lt;element minOccurs="0" ref="ows:BoundingBox"&gt;
 *  							&lt;annotation&gt;
 *  								&lt;documentation&gt;
 *  									Minimum bounding rectangle surrounding 
 *  									the visible layer presented by this tile matrix 
 *  									set, in the supported CRS &lt;/documentation&gt;
 *  							&lt;/annotation&gt;
 *  						&lt;/element&gt;
 *  						&lt;element ref="ows:SupportedCRS"&gt;
 *  							&lt;annotation&gt;
 *  								&lt;documentation&gt;Reference to one coordinate reference 
 *  								system (CRS).&lt;/documentation&gt;
 *  							&lt;/annotation&gt;
 *  						&lt;/element&gt;
 *  						&lt;element minOccurs="0" name="WellKnownScaleSet" type="anyURI"&gt;
 *  							&lt;annotation&gt;
 *  								&lt;documentation&gt;Reference to a well known scale set.
 *  									urn:ogc:def:wkss:OGC:1.0:GlobalCRS84Scale, 
 *  									urn:ogc:def:wkss:OGC:1.0:GlobalCRS84Pixel, 
 *  									urn:ogc:def:wkss:OGC:1.0:GoogleCRS84Quad and 
 *  									urn:ogc:def:wkss:OGC:1.0:GoogleMapsCompatible are 
 *  								possible values that are defined in Annex E. It has to be consistent with the 
 *  								SupportedCRS and with the ScaleDenominators of the TileMatrix elements.
 *  								&lt;/documentation&gt;
 *  							&lt;/annotation&gt;
 *  						&lt;/element&gt;
 *  						&lt;element maxOccurs="unbounded" ref="wmts:TileMatrix"&gt;
 *  							&lt;annotation&gt;
 *  								&lt;documentation&gt;Describes a scale level and its tile matrix.&lt;/documentation&gt;
 *  							&lt;/annotation&gt;
 *  						&lt;/element&gt;
 *  					&lt;/sequence&gt;
 *  				&lt;/extension&gt;
 *  			&lt;/complexContent&gt;
 *  		&lt;/complexType&gt;
 *  	&lt;/element&gt; 
 *		
 *	  </code>
 * </pre>
 * </p>
 *
 * @generated
 */
public class TileMatrixSetBinding extends AbstractSimpleBinding {

    wmts11Factory factory;

    public TileMatrixSetBinding(wmts11Factory factory) {
        super();
        this.factory = factory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WMTS.TileMatrixSet;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Class getType() {
        return TileMatrixSetType.class;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        TileMatrixSetType matrixSet = factory.createTileMatrixSetType();
        matrixSet.setBoundingBox((BoundingBoxType) node.getChild("BoundingBox"));
        matrixSet.setIdentifier((CodeType) node.getChild("Identifier"));
        matrixSet.setSupportedCRS((String)node.getChildValue("SupportedCRS"));
        matrixSet.setWellKnownScaleSet((String)node.getChildValue("WellKnownScaleSet"));
        matrixSet.getAbstract().addAll(node.getChildren("abstract"));
        matrixSet.getTileMatrix().addAll(node.getChildren("TileMatrix"));
        
        return matrixSet;
    }

}