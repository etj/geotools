/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2004-2016, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.wmts.request;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.ows.HTTPResponse;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.Response;
import org.geotools.data.ows.StyleImpl;
import org.geotools.data.wms.request.AbstractGetMapRequest;
import org.geotools.data.wmts.WMTSCapabilities;
import org.geotools.data.wmts.WMTSLayer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.ServiceException;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.tile.Tile;
import org.geotools.tile.TileService;
import org.geotools.tile.impl.wmts.TileMatrixLimits;
import org.geotools.tile.impl.wmts.TileMatrixSet;
import org.geotools.tile.impl.wmts.TileMatrixSetLink;
import org.geotools.tile.impl.wmts.WMTSService;
import org.geotools.tile.impl.wmts.WMTSServiceType;
import org.geotools.tile.impl.wmts.WMTSTileFactory;
import org.geotools.util.logging.Logging;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.TransformException;

/**
 * 
 * @author Richard Gould
 *
 *
 * @source $URL$
 */
public abstract class AbstractGetTileRequest extends AbstractGetMapRequest
        implements GetTileRequest {
    /** MAXTILES */
    private static final int MAXTILES = 256;

    /** DPI */
    private static final double DPI = 96.0;

    static WMTSTileFactory factory = new WMTSTileFactory();

    public static final String LAYER = "Layer";

    public static final String STYLE = "Style";

    public static final String TILECOL = "TileCol";

    public static final String TILEROW = "TileRow";

    public static final String TILEMATRIX = "TileMatrix";

    public static final String TILEMATRIXSET = "TileMatrixSet";

    private WMTSLayer layer = null;

    private String styleName = "";

    private String srs;

    static final Logger LOGGER = Logging.getLogger(AbstractGetTileRequest.class);

    protected WMTSServiceType type;

    protected WMTSCapabilities capabilities;

    private ReferencedEnvelope bbox;

    private CoordinateReferenceSystem crs;

    /**
     * Constructs a GetMapRequest. The data passed in represents valid values that can be used.
     * 
     * @param onlineResource the location that the request should be applied to
     * @param properties pre-set properties to be used. Can be null.
     */
    public AbstractGetTileRequest(URL onlineResource, Properties properties) {
        super(onlineResource, properties);
    }

    @Override
    public Response createResponse(HTTPResponse response) throws ServiceException, IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addLayer(Layer layer, String style) {
        this.layer = (WMTSLayer) layer;
        super.addLayer(layer, style);
    }

    @Override
    public void addLayer(Layer layer) {
        this.layer = (WMTSLayer) layer;
        super.addLayer(layer);
    }

    @Override
    public void addLayer(Layer layer, StyleImpl style) {
        this.layer = (WMTSLayer) layer;
        super.addLayer(layer, style);
    }

    @Override
    public void setSRS(String srs) {
        this.srs = srs;
        super.setSRS(srs);
    }

    /**
     * @return the crs
     */
    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    @Override
    public void setCRS(CoordinateReferenceSystem coordinateReferenceSystem) {
        crs = coordinateReferenceSystem;

    }

    /**
     * fetch the tiles we need to generate the image
     * 
     * @throws ServiceException
     */
    public Set<Tile> getTiles() throws ServiceException {
        Set<Tile> tiles = new HashSet<>();
        if (layer == null) {
            throw new ServiceException("GetTiles called with no layer set");
        }

        LOGGER.warning("===== getTiles    layer:" + layer );

        String layerString = "";
        String styleString = "";

        try {
            // spaces are converted to plus signs, but must be %20 for url calls [GEOT-4317]
            layerString = URLEncoder.encode(layer.getName(), "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException | NullPointerException e) {
            layerString = layerString + layer.getName();
        }
        styleName = styleName == null ? "" : styleName;
        try {
            styleString = URLEncoder.encode(styleName, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException | NullPointerException e1) {
            styleString = styleString + styleName;
        }

        setProperty(LAYER, layerString);
        setProperty(STYLE, styleString);
        String width = properties.getProperty(WIDTH);
        String height = properties.getProperty(HEIGHT);
        if (width == null || width.isEmpty() || height == null || height.isEmpty()) {
            throw new ServiceException("Can't request TILES without width and height being set");
        }

        LOGGER.warning("===== getTiles    layer:" + layer + " w:" + width + " x h:"+height);

        int w = Integer.parseInt(width);
        int h = Integer.parseInt(height);
        TileMatrixSet matrixSet = null;
        Map<String, TileMatrixSetLink> links = layer.getTileMatrixLinks();
        CoordinateReferenceSystem requestCRS = getCrs();
        LOGGER.fine("request CRS " + requestCRS);
        if (requestCRS == null) {
            try {
                LOGGER.fine("request CRS decoding" + srs);
                requestCRS = CRS.decode(srs);

            } catch (FactoryException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
        /* System.out.println(requestCRS); */
        for (TileMatrixSet matrix : capabilities.getMatrixes()) {

            CoordinateReferenceSystem coordinateReferenceSystem = null;
            try {
                coordinateReferenceSystem = matrix.getCoordinateReferenceSystem();
            } catch (FactoryException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
            /* System.out.println("comparing "+coordinateReferenceSystem); */
            // TODO: possible issues here if axis order is not the same
            if (CRS.equalsIgnoreMetadata(requestCRS, coordinateReferenceSystem)) {// matching SRS
                if (links.containsKey((matrix.getIdentifier()))) { // and available for this layer
                    LOGGER.fine("selected matrix set:" + matrix.getIdentifier());
                    setProperty(TILEMATRIXSET, matrix.getIdentifier());
                    matrixSet = matrix;

                    break;
                }
            }
        }

        if (matrixSet == null) {
            // Just pick one!
            LOGGER.warning("Failed to match the requested CRS (" + requestCRS.getName()
                    + ") with any of the tile matrices!");
            for (TileMatrixSet matrix : capabilities.getMatrixes()) {
                if (links.containsKey((matrix.getIdentifier()))) { // and available for this layer
                    LOGGER.fine("selected matrix set:" + matrix.getIdentifier());
                    setProperty(TILEMATRIXSET, matrix.getIdentifier());
                    matrixSet = matrix;

                    break;
                }
            }
            if (matrixSet == null) {
                throw new ServiceException("Unable to find a matching TileMatrixSet for layer "
                        + layer.getName() + " and SRS: " + requestCRS.getName());
            }
        }
        // System.out.println("selected "+matrixSet.getCrs());
        String requestUrl = onlineResource.toString();
        if (WMTSServiceType.REST.equals(type)) {
            String format = (String) getProperties().get("Format");
            if (format == null || format.isEmpty()) {
                format = "image/png";
            }
            requestUrl = layer.getTemplate(format);
        }
        TileService wmtsService = new WMTSService(requestUrl, type, layerString, styleString,
                matrixSet, layer.getTileMatrixLinks().get(matrixSet.getIdentifier()).getLimits());

        // zoomLevel = factory.getZoomLevel(zoom, wmtsService);
        int scale = 0;

        try {
            scale = (int) Math.round(RendererUtilities.calculateScale(bbox, w, h, DPI));
        } catch (FactoryException | TransformException ex) {
            throw new RuntimeException("Failed to calculate scale", ex);
        }
        tiles = ((WMTSService) wmtsService).findTilesInExtent(bbox, scale, false, MAXTILES);
        LOGGER.fine("found " + tiles.size() + " tiles in " + bbox);
        if (tiles.isEmpty()) {
            return tiles;
        }
        Tile first = tiles.iterator().next();
        int z = first.getTileIdentifier().getZ();
        List<TileMatrixLimits> limits = layer.getTileMatrixLinks().get(matrixSet.getIdentifier())
                .getLimits();
        TileMatrixLimits limit;
        if (!limits.isEmpty()) {

            limit = limits.get(z);
        } else {
            // seems that MapProxy (and all REST APIs?) don't create limits
            limit = new TileMatrixLimits();
            limit.setMaxCol(matrixSet.getMatrices().get(z).getMatrixWidth() - 1);
            limit.setMaxRow(matrixSet.getMatrices().get(z).getMatrixHeight() - 1);
            limit.setMinCol(0);
            limit.setMinRow(0);
            limit.setTileMatix(matrixSet.getIdentifier());
        }
        ArrayList<Tile> remove = new ArrayList<>();
        for (Tile tile : tiles) {

            int x = tile.getTileIdentifier().getX();
            int y = tile.getTileIdentifier().getY();
            if (x < limit.getMincol() || x > limit.getMaxcol()) {
                LOGGER.fine(
                        x + " exceeds col limits " + limit.getMincol() + " " + limit.getMaxcol());
                remove.add(tile);
                continue;
            }

            if (y < limit.getMinrow() || y > limit.getMaxrow()) {
                LOGGER.fine(
                        y + " exceeds row limits " + limit.getMinrow() + " " + limit.getMaxrow());
                remove.add(tile);
            }
        }
        tiles.removeAll(remove);

        return tiles;

    }

    protected abstract void initVersion();

    protected void initRequest() {
        setProperty(REQUEST, "GetTile");
    }

    @Override
    public void setBBox(Envelope envelope) {
        if (srs != null && !srs.isEmpty()) {
            try {
                this.bbox = new ReferencedEnvelope(CRS.decode(srs));
            } catch (MismatchedDimensionException | FactoryException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
                this.bbox = new ReferencedEnvelope();
            }
            this.bbox.expandToInclude(envelope.getUpperCorner());
            this.bbox.expandToInclude(envelope.getLowerCorner());
        } else {
            this.bbox = new ReferencedEnvelope(envelope);
        }
    }

    /**
     * From the Web Map Service Implementation Specification: "The required BBOX parameter allows a Client to request a particular Bounding Box. The
     * value of the BBOX parameter in a GetMap request is a list of comma-separated numbers of the form "minx,miny,maxx,maxy". If the WMS server has
     * declared that a Layer is not subsettable, then the Client shall specify exactly the declared Bounding Box values in the GetMap request and the
     * Server may issue a Service Exception otherwise."
     * <p>
     * You must also call setSRS to provide the spatial reference system information (or CRS:84 will be assumed)
     * 
     * @param bbox A string representing a bounding box in the format "minx,miny,maxx,maxy"
     */
    public void setBBox(String bbox) {
        String[] c = bbox.split(",");
        double x1 = Double.parseDouble(c[0]);
        double x2 = Double.parseDouble(c[2]);
        double y1 = Double.parseDouble(c[1]);
        double y2 = Double.parseDouble(c[3]);

        CoordinateReferenceSystem crs = toServerCRS(srs, false);
        if (isGeotoolsLongitudeFirstAxisOrderForced()
                || crs.getCoordinateSystem().getAxis(0).getDirection().equals(AxisDirection.EAST)) {
            this.bbox = new ReferencedEnvelope(x1, x2, y1, y2, crs);
        } else {
            this.bbox = new ReferencedEnvelope(y1, y2, x1, x2, crs);
        }

    }

}