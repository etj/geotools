package org.geotools.wmts.bindings;


import javax.xml.namespace.QName;

import org.geotools.wmts.WMTS;
import org.geotools.xml.AbstractSimpleBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import net.opengis.ows11.OperationsMetadataType;
import net.opengis.ows11.ServiceIdentificationType;
import net.opengis.ows11.ServiceProviderType;
import net.opengis.wmts.v_11.CapabilitiesType;
import net.opengis.wmts.v_11.ContentsType;
import net.opengis.wmts.v_11.ThemeType;
import net.opengis.wmts.v_11.wmts11Factory;

/**
 * Binding object for the element http://www.opengis.net/wmts/1.0:Capabilities.
 *
 * <p>
 *	<pre>
 *	 <code>
 *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;element name="Capabilities" xmlns="http://www.w3.org/2001/XMLSchema"&gt;
 *  		&lt;annotation&gt;
 *  			&lt;documentation&gt;XML defines the WMTS GetCapabilities operation response. 
 *  			ServiceMetadata document provides clients with service metadata about a specific service 
 *  			instance, including metadata about the tightly-coupled data served. If the server 
 *  			does not implement the updateSequence parameter, the server SHALL always 
 *  			return the complete Capabilities document, without the updateSequence parameter. 
 *  			When the server implements the updateSequence parameter and the 
 *  			GetCapabilities operation request included the updateSequence parameter 
 *  			with the current value, the server SHALL return this element with only the 
 *  			"version" and "updateSequence" attributes. Otherwise, all optional elements 
 *  			SHALL be included or not depending on the actual value of the Contents 
 *  			parameter in the GetCapabilities operation request.
 *  			&lt;/documentation&gt;
 *  		&lt;/annotation&gt;
 *  		&lt;complexType&gt;
 *  			&lt;complexContent&gt;
 *  				&lt;extension base="ows:CapabilitiesBaseType"&gt;
 *  					&lt;sequence&gt;
 *  						&lt;element minOccurs="0" name="Contents" type="wmts:ContentsType"&gt;
 *  							&lt;annotation&gt;
 *  								&lt;documentation&gt;Metadata about the data served by this server. 
 *  								For WMTS, this section SHALL contain data about layers and 
 *  								TileMatrixSets&lt;/documentation&gt;
 *  							&lt;/annotation&gt;
 *  						&lt;/element&gt;
 *  						&lt;element maxOccurs="unbounded" minOccurs="0" ref="wmts:Themes"&gt;
 *  							&lt;annotation&gt;
 *  								&lt;documentation&gt;
 *  								Metadata describing a theme hierarchy for the layers
 *  								&lt;/documentation&gt;
 *  							&lt;/annotation&gt;
 *  						&lt;/element&gt;
 *  						&lt;element maxOccurs="unbounded" minOccurs="0" name="WSDL" type="ows:OnlineResourceType"&gt;
 *  							&lt;annotation&gt;
 *  								&lt;documentation&gt;Reference to a WSDL resource&lt;/documentation&gt;
 *  							&lt;/annotation&gt;
 *  						&lt;/element&gt;
 *  						&lt;element maxOccurs="unbounded" minOccurs="0" name="ServiceMetadataURL" type="ows:OnlineResourceType"&gt;
 *  							&lt;annotation&gt;
 *  								&lt;documentation&gt;
 *  								Reference to a ServiceMetadata resource on resource 
 *  								oriented architectural style
 *  								&lt;/documentation&gt;
 *  							&lt;/annotation&gt;
 *  						&lt;/element&gt;
 *  					&lt;/sequence&gt;
 *  				&lt;/extension&gt;
 *  			&lt;/complexContent&gt;
 *  		&lt;/complexType&gt;
 *  	&lt;/element&gt; 
 *		
 *	  </code>
 *	 </pre>
 * </p>
 *
 * @generated
 */
public class CapabilitiesBinding extends AbstractSimpleBinding {

	wmts11Factory factory;		
	public CapabilitiesBinding( wmts11Factory factory ) {
		super();
		this.factory = factory;
	}

	/**
	 * @generated
	 */
	public QName getTarget() {
		return WMTS.Capabilities;
	}
	
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *	
	 * @generated modifiable
	 */	
	public Class getType() {
		return net.opengis.wmts.v_11.CapabilitiesType.class;
	}
	
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *	
	 * @generated modifiable
	 */	
	public Object parse(ElementInstance instance, Node node, Object value) 
		throws Exception {
		CapabilitiesType capabilities = factory.createCapabilitiesType();
		capabilities.setContents((ContentsType) node.getChild(ContentsType.class));
		capabilities.getThemes().addAll(node.getChildren(ThemeType.class));
		capabilities.setOperationsMetadata((OperationsMetadataType) node.getChild(OperationsMetadataType.class));
		capabilities.setServiceIdentification((ServiceIdentificationType) node.getChildValue(ServiceIdentificationType.class));
		capabilities.setServiceProvider((ServiceProviderType) node.getChildValue(ServiceProviderType.class));
		capabilities.setUpdateSequence((String) node.getChildValue("UpdateSequence"));
		capabilities.setVersion((String)node.getChildValue("version"));
		capabilities.getServiceMetadataURL().addAll(node.getChildren("ServiceMetadataURL"));
		capabilities.getWSDL().addAll(node.getChildren("WSDL"));
		return capabilities;
	}

}