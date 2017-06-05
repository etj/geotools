/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2017, Open Source Geospatial Foundation (OSGeo)
 *    
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.tile.impl.wmts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.tile.Tile;
import org.geotools.tile.TileFactory;
import org.geotools.tile.TileService;
import org.geotools.tile.impl.ScaleZoomLevelMatcher;
import org.geotools.tile.impl.ZoomLevel;
import org.opengis.geometry.BoundingBox;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.TransformException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A tile service for WMTS servers. This is tied to a single layer and style.
 * 
 * @author ian
 *
 */
public class WMTSService extends TileService {
    private static final TileFactory tileFactory = new WMTSTileFactory();

    private static final String OWS = "http://www.opengis.net/ows/1.1";

    private static final String xlink = "http://www.w3.org/1999/xlink";

    private String tileMatrixSetName = "";

    private double[] scaleList;

    private TileMatrixSet matrixSet;

    private String layerName;

    private String styleName = ""; // Default style is ""

    private ReferencedEnvelope envelope;

    private String templateURL = "";

    private WMTSServiceType type = WMTSServiceType.REST;

    private String format = "image/png";

    private List<TileMatrixLimits> limits;

    private File cachedGetCapabilities = null;

    /**
     * This is only used for test purposes!
     * 
     * @param name - a name to refer to the layer by.
     * @param baseURL - the templated ResourceURL from the getcapabilities document.
     * 
     * 
     */
    @Deprecated
    public WMTSService(String name, String baseURL, String layerName, String tileMatrixSetName,
            WMTSServiceType type, File getcapa) {
        super(name, baseURL);
        setLayerName(layerName);
        setType(type);
        setTemplateURL(baseURL);
        this.cachedGetCapabilities = getcapa;

        setTileMatrixSetName(tileMatrixSetName); // also load matrixSet
    }

    /**
     * create a service directly with out parsing the capabilties again.
     * 
     * @param requestURL - where to ask for tiles
     * @param type - KVP or REST
     * @param layerName - name of the layer to request
     * @param styleName - name of the style to use?
     * @param tileMatrixSetName - matrixset name
     */
    public WMTSService(String templateURL, WMTSServiceType type, String layerName, String styleName,
            TileMatrixSet tileMatrixSet, List<TileMatrixLimits> limits) {
        super("wmts", templateURL);
        this.limits = limits;
        setTemplateURL(templateURL);
        setLayerName(layerName);
        setStyleName(styleName);
        setType(type);
        setMatrixSet(tileMatrixSet);
        setTileMatrixSetName(tileMatrixSet.getIdentifier());
    }

    @Override
    public Set<Tile> findTilesInExtent(ReferencedEnvelope _mapExtent, int scaleFactor,
            boolean recommendedZoomLevel, int maxNumberOfTiles) {
        Set<Tile> ret = Collections.emptySet();
        //System.out.println("request bbox :"+_mapExtent+" "+_mapExtent.getCoordinateReferenceSystem().getCoordinateSystem().getAxis(0).getDirection());
        ReferencedEnvelope extent = createSafeEnvelopeInTileCRS( _mapExtent ) ;
        //System.out.println("fixed bbox :"+extent+" "+extent.getCoordinateReferenceSystem().getCoordinateSystem().getAxis(0).getDirection());
        
        ReferencedEnvelope coverageEnvelope = getBounds();
        //System.out.println("coverage bbox :"+coverageEnvelope+" "+coverageEnvelope.getCoordinateReferenceSystem().getCoordinateSystem().getAxis(0).getDirection());
        boolean sameCRS = CRS.equalsIgnoreMetadata(coverageEnvelope.getCoordinateReferenceSystem(),
                extent.getCoordinateReferenceSystem());
        if (sameCRS) {
            if (!coverageEnvelope.intersects((BoundingBox) extent)) {
                
                return ret;
            }
        } else {
            ReferencedEnvelope dataEnvelopeWGS84;
            ReferencedEnvelope requestEnvelopeWGS84;
            try {
                dataEnvelopeWGS84 = coverageEnvelope.transform(DefaultGeographicCRS.WGS84, true);

                requestEnvelopeWGS84 = extent.transform(DefaultGeographicCRS.WGS84, true);

                if (!dataEnvelopeWGS84.intersects((BoundingBox) requestEnvelopeWGS84)) {
                    return ret;
                }
            } catch (TransformException | FactoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
      
        TileFactory tileFactory = getTileFactory();

        ScaleZoomLevelMatcher zoomLevelMatcher = null;
        try {

            zoomLevelMatcher = new ScaleZoomLevelMatcher(getTileCrs(), getProjectedTileCrs(),
                    CRS.findMathTransform(getTileCrs(), getProjectedTileCrs()),
                    CRS.findMathTransform(getProjectedTileCrs(), getTileCrs()), _mapExtent,
                    _mapExtent, scaleFactor);

        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }

        int zl = getZoomLevelFromMapScale(zoomLevelMatcher, scaleFactor);
        ZoomLevel zoomLevel = tileFactory.getZoomLevel(zl, this);
        long maxNumberOfTilesForZoomLevel = zoomLevel.getMaxTileNumber();

        // Map<String, Tile> tileList = new HashMap<String, Tile>();
        Set<Tile> tileList = new HashSet<Tile>(
                (int) Math.min(maxNumberOfTiles, maxNumberOfTilesForZoomLevel));
        Tile firstTile;
        // Let's get the first tile which covers the upper-left corner
        if (/*TileMatrix.isGeotoolsLongitudeFirstAxisOrderForced()
                || */extent.getCoordinateReferenceSystem().getCoordinateSystem().getAxis(0)
                        .getDirection().equals(AxisDirection.EAST)) {
            firstTile = tileFactory.findTileAtCoordinate(extent.getMinX(), extent.getMaxY(),
                    zoomLevel, this);
        } else {
            firstTile = tileFactory.findTileAtCoordinate(extent.getMinY(), extent.getMaxX(),
                    zoomLevel, this);
        }

        addTileToCache(firstTile);
        tileList.add(firstTile);

        Tile firstTileOfRow = firstTile;
        Tile movingTile = firstTile;

        // Loop column
        do {
            // Loop row
            do {

                // get the next tile right of this one
                // Tile rightNeighbour = movingTile.getRightNeighbour();
                Tile rightNeighbour = tileFactory.findRightNeighbour(movingTile, this);// movingTile.getRightNeighbour();

                // Check if the new tile is still part of the extent and
                // that we don't have the first tile again
                boolean intersects = extent.intersects((Envelope) rightNeighbour.getExtent());
                if (intersects && !firstTileOfRow.equals(rightNeighbour)) {

                    addTileToCache(rightNeighbour);
                    tileList.add(rightNeighbour);

                    movingTile = rightNeighbour;
                } else {

                    break;
                }
                if (tileList.size() > maxNumberOfTiles) {
                    LOGGER.warning("Reached tile limit of " + maxNumberOfTiles
                            + ". Returning an empty collection.");
                    return Collections.emptySet();
                }
            } while (tileList.size() < maxNumberOfTilesForZoomLevel);

            // get the next tile under the first one of the row
            // Tile lowerNeighbour = firstTileOfRow.getLowerNeighbour();
            Tile lowerNeighbour = tileFactory.findLowerNeighbour(firstTileOfRow, this);

            // Check if the new tile is still part of the extent
            boolean intersects = extent.intersects((Envelope) lowerNeighbour.getExtent());
            if (intersects && !firstTile.equals(lowerNeighbour)) {

                // System.out.printf("N: %s %s", lowerNeighbour.getId(),
                // addTileToList(lowerNeighbour));

                addTileToCache(lowerNeighbour);
                tileList.add(lowerNeighbour);

                firstTileOfRow = movingTile = lowerNeighbour;
            } else {
                break;
            }
        } while (tileList.size() < maxNumberOfTilesForZoomLevel);

        return tileList;
    }

    /**
     * @param _mapExtent
     * @return
     */
    private ReferencedEnvelope createSafeEnvelopeInTileCRS(ReferencedEnvelope _mapExtent) {
        CoordinateReferenceSystem crs = getProjectedTileCrs();
        try {
            //clip _mapExtent to the domain of availability!
            ReferencedEnvelope extent = getAcceptableExtent(crs);
            crs=extent.getCoordinateReferenceSystem();
            if(!CRS.equalsIgnoreMetadata(crs, _mapExtent.getCoordinateReferenceSystem())) {
                extent = extent.transform(_mapExtent.getCoordinateReferenceSystem(), true);
            } /*else {
              return _mapExtent;  
            }*/
            if(_mapExtent.intersects((Envelope)extent)) {
                _mapExtent = _mapExtent.intersection(extent);
            }
            return _mapExtent.transform(crs, true);

        } catch (TransformException e) {
            throw new RuntimeException(e);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the type
     */
    public WMTSServiceType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(WMTSServiceType type) {
        this.type = type;
    }

    /**
     * @param layerName
     */
    private void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    /**
     * @return the layerName
     */
    public String getLayerName() {
        return layerName;
    }

    /**
     * @return the styleName
     */
    public String getStyleName() {
        return styleName;
    }

    /**
     * @param styleName the styleName to set
     */
    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    @Override
    public double[] getScaleList() {
        return scaleList;
    }

    @Override
    public ReferencedEnvelope getBounds() {

        if (envelope != null) {
            return envelope;
        }
        // Look this up from the CRS
        CoordinateReferenceSystem projectedTileCrs = getProjectedTileCrs();
        return envelope = getAcceptableExtent(projectedTileCrs);
    }

    /**
     * @param projectedTileCrs
     * @return
     */
   public static ReferencedEnvelope getAcceptableExtent(CoordinateReferenceSystem projectedTileCrs) {
        Extent extent = projectedTileCrs.getDomainOfValidity();
        Iterator<? extends GeographicExtent> itr = extent.getGeographicElements().iterator();
        while (itr.hasNext()) {
            //GeographicExtent is always long/lat!
            GeographicExtent ex = itr.next();
            ReferencedEnvelope envelope;
            if (ex instanceof GeographicBoundingBox) {
                DefaultGeographicCRS wgs84 = DefaultGeographicCRS.WGS84;
                envelope = new ReferencedEnvelope(wgs84);
                if (wgs84.getCoordinateSystem().getAxis(0).getDirection().equals(AxisDirection.EAST)) {
                    envelope.expandToInclude(
                            new Coordinate(((GeographicBoundingBox) ex).getEastBoundLongitude(),
                                    ((GeographicBoundingBox) ex).getNorthBoundLatitude()));
                    envelope.expandToInclude(
                            new Coordinate(((GeographicBoundingBox) ex).getWestBoundLongitude(),
                                    ((GeographicBoundingBox) ex).getSouthBoundLatitude()));
                    
                }else {
                    envelope.expandToInclude(
                            new Coordinate(((GeographicBoundingBox) ex).getNorthBoundLatitude(),
                                    ((GeographicBoundingBox) ex).getEastBoundLongitude()));
                    envelope.expandToInclude(
                            new Coordinate(((GeographicBoundingBox) ex).getSouthBoundLatitude(),
                                    ((GeographicBoundingBox) ex).getWestBoundLongitude()));
                    
                }
                return envelope;
            }

        }
        LOGGER.severe(
                "Unable to determine Geographic Extent of CRS:" + projectedTileCrs.toString());
        return null;
    }

    @Override
    public CoordinateReferenceSystem getProjectedTileCrs() {
        try {
            return matrixSet.getCoordinateReferenceSystem();
        } catch (FactoryException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    @Override
    public TileFactory getTileFactory() {
        return tileFactory;
    }

    /**
     * @return the tileMatrixSetName
     */
    public String getTileMatrixSetName() {
        return tileMatrixSetName;
    }

    /**
     * @param tileMatrixSetName the tileMatrixSetName to set
     */
    public void setTileMatrixSetName(String tileMatrixSetName) {
        if (tileMatrixSetName == null || tileMatrixSetName.isEmpty()) {
            throw new IllegalArgumentException("Tile matrix set name cannot be null");
        }

        this.tileMatrixSetName = tileMatrixSetName;
        if (WMTSServiceType.REST.equals(type)) {
            if (matrixSet == null) {
                extractRestTileMatrixSet();
            }
        }
        if (WMTSServiceType.KVP.equals(type)) {
            if (matrixSet == null) {
                extractKVPTileMatrixSet();
            }
        }
    }

    /**
     * @return the limits
     */
    public List<TileMatrixLimits> getLimits() {

        return limits;
    }

    /**
     * @param limits the limits to set
     */
    public void setLimits(List<TileMatrixLimits> limits) {
        this.limits = limits;
    }

    private org.w3c.dom.Document parseCachedCapabilities() throws IOException {

        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            org.w3c.dom.Document doc = db.parse(cachedGetCapabilities);
            return doc;
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Error handling getCapabilities document", e);
        }
    }

    private org.w3c.dom.Document getKVPCapabilitiesDoc() throws IOException {

        if(cachedGetCapabilities != null) {
            return parseCachedCapabilities();
        }

        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(getBaseUrl());
        method.setQueryString("REQUEST=GetCapabilities");
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));
        try {
            // Execute the method.

            int statusCode = client.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK) {
                LOGGER.severe("Method failed: " + method.getStatusLine());
            }

            // Read the response body.
            byte[] responseBody = method.getResponseBody();

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            // System.out.println(new String(responseBody,"UTF-8"));

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            org.w3c.dom.Document doc = db.parse(new ByteArrayInputStream(responseBody));
            return doc;
        } catch (HttpException e) {
            throw new IOException("Error handling getCapabilities document", e);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new IOException("Error handling getCapabilities document", e);
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
    }

    /**
     * This is replaced by gt-xsd-wmts
     */
    @Deprecated
    private void extractKVPTileMatrixSet() {
        try{
            org.w3c.dom.Document doc = getKVPCapabilitiesDoc();

            NodeList ops = doc.getElementsByTagNameNS(OWS, "Operation");
            for (int i = 0; i < ops.getLength(); i++) {
                Element element = (Element) ops.item(i);
                if (element.getAttribute("name").equalsIgnoreCase("GetTile")) {
                    NodeList values = element.getElementsByTagNameNS(OWS, "Get");
                    Element url = (Element) values.item(0);

                    templateURL = url.getAttributeNS(xlink, "href");
                }
            }

            // read layer info

            NodeList layers = doc.getElementsByTagName("Layer");
            for (int i = 0; i < layers.getLength(); i++) {
                Element element = (Element) layers.item(i);
                NodeList names = element.getElementsByTagNameNS(OWS, "Identifier");
                if (layerName.equalsIgnoreCase(names.item(0).getTextContent())) {
                    envelope = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);

                    Element layer = (Element) layers.item(i);
                    getWGS84Bounds(layer);

                }
            }

            NodeList tms = doc.getElementsByTagName("TileMatrixSet");
            for (int i = 0; i < tms.getLength(); i++) {
                Element e = (Element) tms.item(i);
                NodeList names = e.getElementsByTagNameNS("http://www.opengis.net/ows/1.1",
                        "Identifier");
                if (names.getLength() == 0) { // annoyingly TileMatrixSet tag is used in layers too!
                    continue;
                }

                String setName = names.item(0).getTextContent();

                if (tileMatrixSetName.equalsIgnoreCase(setName)) {
                    matrixSet = TileMatrixSet.parseTileMatrixSet(e);
                    scaleList = new double[matrixSet.size()];
                    int j = 0;
                    for (TileMatrix tm : matrixSet.getMatrices()) {
                        scaleList[j++] = tm.getDenominator();
                    }
                }
            }
            // TODO: Tidy these exceptions up and throw something
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error parsing Capabilities document", e);
        }
    }

    /**
     * @param layer
     */
    @Deprecated
    private void getWGS84Bounds(Element layer) {
        Node wgsbbox = layer.getElementsByTagNameNS(OWS, "WGS84BoundingBox").item(0);
        if(wgsbbox != null) {
            NodeList bbox = wgsbbox.getChildNodes();
            for (int j = 0; j < bbox.getLength(); j++) {
                if (!(bbox.item(j) instanceof Element))
                    continue;
                Element e = (Element) bbox.item(j);

                if (e.getLocalName().equals("LowerCorner") || e.getLocalName().equals("UpperCorner")) {
                    String coords[] = e.getTextContent().split(" ");

                    envelope.expandToInclude(Double.parseDouble(coords[0]),
                            Double.parseDouble(coords[1]));
                }
            }
        }
    }


    private org.w3c.dom.Document getRESTCapabilitiesDoc() throws IOException {

        if(cachedGetCapabilities != null) {
            return parseCachedCapabilities();
        }

        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(getBaseUrl());
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));
        try {
            // Execute the method.
            int statusCode = client.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK) {
                LOGGER.severe("Method failed: " + method.getStatusLine());

            }

            // Read the response body.
            byte[] responseBody = method.getResponseBody();

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            // System.out.println(new String(responseBody,"UTF-8"));

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            org.w3c.dom.Document doc = db.parse(new ByteArrayInputStream(responseBody));
            return doc;
        } catch (HttpException e) {
            throw new IOException("Error handling REST getCapabilities document", e);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new IOException("Error handling REST getCapabilities document", e);
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
    }

    /**
     * Extract the scales, bbox etc from capabilities.
     */
    @Deprecated
    private void extractRestTileMatrixSet() {

        try {
            // read layer info
            org.w3c.dom.Document doc = getRESTCapabilitiesDoc();

            boolean layerFound = false;
            NodeList layers = doc.getElementsByTagName("Layer");
            for (int i = 0; i < layers.getLength(); i++) {
                Element element = (Element) layers.item(i);
                NodeList names = element.getElementsByTagNameNS(OWS, "Identifier");
                if (layerName.equalsIgnoreCase(names.item(0).getTextContent())) {
                    envelope = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);

                    Element layer = (Element) layers.item(i);
                    getWGS84Bounds(layer);
                    Element resource = (Element) layer.getElementsByTagName("ResourceURL").item(0);
                    templateURL = resource.getAttribute("template");
                    layerFound = true;
                }
            }
            if(! layerFound) {
                LOGGER.warning("Layer not found : " + layerName);
            }

            NodeList tms = doc.getElementsByTagName("TileMatrixSet");
            for (int i = 0; i < tms.getLength(); i++) {
                Element e = (Element) tms.item(i);
                NodeList names = e.getElementsByTagNameNS("http://www.opengis.net/ows/1.1",
                        "Identifier");
                if (names.getLength() == 0) { // annoyingly TileMatrixSet tag is used in layers too!
                    continue;
                }

                if (tileMatrixSetName.equalsIgnoreCase(names.item(0).getTextContent())) {
                    matrixSet = TileMatrixSet.parseTileMatrixSet(e);
                    scaleList = new double[matrixSet.size()];
                    int j = 0;
                    for (TileMatrix tm : matrixSet.getMatrices()) {
                        scaleList[j++] = tm.getDenominator();
                    }
                }
            }
            if (matrixSet == null) {
                LOGGER.warning("TileMatrixSet not found: " + tileMatrixSetName);
            }

            // TODO: Tidy these exceptions up and throw something
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error parsing REST Capabilities document", e);
        }
    }

    /**
     * @return the templateURL
     */
    public String getTemplateURL() {
        return templateURL;
    }

    /**
     * @param templateURL the templateURL to set
     */
    public void setTemplateURL(String templateURL) {
        this.templateURL = templateURL;
    }

    /**
     * @param zoomLevel
     * @return
     */
    public TileMatrix getTileMatrix(int zoomLevel) {
        if (matrixSet == null) {
            throw new RuntimeException("TileMatrix is not set in WMTSService");
        }
        return matrixSet.getMatrices().get(zoomLevel);
    }

    /**
     * @return the matrixSet
     */
    public TileMatrixSet getMatrixSet() {
        return matrixSet;
    }

    /**
     * @param matrixSet the matrixSet to set
     */
    public void setMatrixSet(TileMatrixSet matrixSet) {
        this.matrixSet = matrixSet;
        scaleList = new double[matrixSet.size()];
        int j = 0;
        for (TileMatrix tm : matrixSet.getMatrices()) {
            scaleList[j++] = tm.getDenominator();
        }
    }

    /**
     * @return
     */
    public String getFormat() {
        return format;
    }

    /**
     * @param format the format to set
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @param i
     * @return
     */
    public WMTSZoomLevel getZoomLevel(int zoom) {
        //
        return new WMTSZoomLevel(zoom, this);
    }

}
