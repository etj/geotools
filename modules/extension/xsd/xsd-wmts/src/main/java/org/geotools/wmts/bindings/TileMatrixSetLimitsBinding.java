package org.geotools.wmts.bindings;

import org.geotools.wmts.WMTS;
import org.geotools.xml.*;
import org.geotools.xml.AbstractSimpleBinding;

import net.opengis.wmts.v_11.TileMatrixLimitsType;
import net.opengis.wmts.v_11.TileMatrixSetLimitsType;
import net.opengis.wmts.v_11.wmts11Factory;

import javax.xml.namespace.QName;

/**
 * Binding object for the element http://www.opengis.net/wmts/1.0:TileMatrixSetLimits.
 *
 * <p>
 * 
 * <pre>
 *	 <code>
 *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;element name="TileMatrixSetLimits" xmlns="http://www.w3.org/2001/XMLSchema"&gt;
 *  		&lt;annotation&gt;
 *  			&lt;documentation&gt;
 *  				Metadata about a the limits of the tile row and tile col indices.
 *  			&lt;/documentation&gt;
 *  		&lt;/annotation&gt;
 *  		&lt;complexType&gt;
 *  			&lt;sequence&gt;
 *  				&lt;element maxOccurs="unbounded" ref="wmts:TileMatrixLimits"&gt;
 *  					&lt;annotation&gt;
 *  						&lt;documentation&gt;
 *  							Metadata describing the limits of the TileMatrixSet indices. 
 *  							Multiplicity must be the multiplicity of TileMatrix in this 
 *  							TileMatrixSet.
 *  						&lt;/documentation&gt;
 *  					&lt;/annotation&gt;
 *  				&lt;/element&gt;
 *  			&lt;/sequence&gt;
 *  		&lt;/complexType&gt;
 *  	&lt;/element&gt; 
 *		
 *	  </code>
 * </pre>
 * </p>
 *
 * @generated
 */
public class TileMatrixSetLimitsBinding extends AbstractSimpleBinding {

    wmts11Factory factory;

    public TileMatrixSetLimitsBinding(wmts11Factory factory) {
        super();
        this.factory = factory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WMTS.TileMatrixSetLimits;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Class getType() {
        return TileMatrixSetLimitsType.class;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {

        TileMatrixSetLimitsType limits = factory.createTileMatrixSetLimitsType();
        
        limits.getTileMatrixLimits().addAll(node.getChildren("TileMatrixSetLimits"));
        
        return limits;
    }

}