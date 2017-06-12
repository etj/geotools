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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;

import org.geotools.tile.Tile;
import org.geotools.tile.TileIdentifier;
import org.geotools.tile.TileService;
import org.geotools.tile.impl.ZoomLevel;
import org.geotools.util.ObjectCache;
import org.geotools.util.ObjectCaches;

/**
 *
 * Handle information about a WMTS tile
 *
 * @author ian
 *
 */
public class WMTSTile extends Tile {

    public static final int DEFAULT_TILE_SIZE = 256;

    /**
     * Many WMTS tiles may be reloaded over and over, especially in a tiles getMap request.
     * The size should be made configurable
     **/
    private static ObjectCache tileImages = ObjectCaches.create("soft", 150);

    WMTSServiceType type = WMTSServiceType.REST;

    private WMTSService service;


    public WMTSTile(int x, int y, ZoomLevel zoomLevel, TileService service) {
        this(new WMTSTileIdentifier(x, y, zoomLevel, service.getName()), service);
        this.service = (WMTSService) service;
        setType(this.service.getType());
    }

    /**
     * @param tileIdentifier
     * @param service
     */
    public WMTSTile(WMTSTileIdentifier tileIdentifier, TileService service) {
        super(tileIdentifier, WMTSTileFactory.getExtentFromTileName(tileIdentifier, service),
                DEFAULT_TILE_SIZE);
        this.service = (WMTSService) service;
        setType(this.service.getType());
    }

    /**
     * @return the type of WMTS KVP or REST
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



    @Override
    public URL getUrl() {
        String baseUrl = new String(service.getTemplateURL());

        TileIdentifier tileIdentifier = getTileIdentifier();
        if (WMTSServiceType.KVP.equals(type)) {
            //in theory it should end with ? but there may be other params that the server needs out of spec
            if(!baseUrl.contains("?")) {

                baseUrl+="?";
            }
            HashMap<String, Object> params = new HashMap<>();
            params.put("service", "WMTS");
            params.put("version", "1.0.0");
            params.put("request", "getTile");
            params.put("layer",service.getLayerName());
            params.put("style",service.getStyleName());
            params.put("format",service.getFormat());
            params.put("tilematrixset", service.getTileMatrixSetName());
            params.put("TileMatrix", service.getTileMatrixSetName()+":"+tileIdentifier.getZ());
            params.put("TileCol", tileIdentifier.getX());
            params.put("TileRow", tileIdentifier.getY());

            StringBuilder arguments = new StringBuilder();
            for(Object p:params.keySet()) {
                try {
                arguments.append(p+"="+URLEncoder.encode(params.get(p).toString(),"UTF-8"));
                arguments.append('&');
                }catch (Exception e) {
                    // TODO: handle exception
                }
            }
            try {
                return new URL(baseUrl+arguments.toString());
            } catch (MalformedURLException e) {
                //I'm pretty sure this never happens!
                throw new RuntimeException(e);
            }
        } else if (WMTSServiceType.REST.equals(type)) {

            baseUrl = baseUrl.replace("{TileMatrixSet}", service.getTileMatrixSetName());
            baseUrl = baseUrl.replace("{TileMatrix}", "" + tileIdentifier.getZ());
            baseUrl = baseUrl.replace("{TileCol}", "" + tileIdentifier.getX());
            baseUrl = baseUrl.replace("{TileRow}", "" + tileIdentifier.getY());


            LOGGER.fine("requesting " + tileIdentifier.getCode());
            try {
                return new URL(baseUrl);
            } catch (MalformedURLException e) {
                //I'm pretty sure this never happens!
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("Unexpected WMTS Service type "+type);
        }
    }

    /**
     * Load and cache locally the WMTS tiles
     */
    @Override
    public BufferedImage loadImageTileImage(Tile tile) throws IOException {
        LOGGER.log(Level.FINE, "Loading tile "+getId()+": " + this.getUrl());

        String tileKey = tile.getUrl().toString();

        if( !(tileImages.peek(tileKey) == null || tileImages.get(tileKey) == null)) {
            LOGGER.log(Level.FINE, "Tile image already loaded for tile "+getId());
            return (BufferedImage) tileImages.get(tileKey);
        } else {
            LOGGER.log(Level.FINE, "Tile image not yet loaded for tile "+getId());
            BufferedImage bi = doLoadImageTileImage(tile);
            tileImages.put(tileKey, bi);
            return bi;
        }
    }

    public BufferedImage doLoadImageTileImage(Tile tile) throws IOException {

        Map<String, String> headers = (Map<String, String>)this.service.getExtrainfo().get("HEADERS");
        InputStream is = null;
        try {
            is = setupInputStream(getUrl(), headers);
            return ImageIO.read(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private InputStream setupInputStream(URL url, Map<String, String> headers) throws IOException
    {
        HttpClient client = new HttpClient();
        String uri = url.toExternalForm();
        uri = uri.replaceAll("\\{Time\\}", "20140101");
        LOGGER.log(Level.FINE, "URL is "+ uri);

        HttpMethod get = new GetMethod(uri);
        if(headers != null) {
            for (String headerName : headers.keySet()) {
                LOGGER.log(Level.FINE, "Adding header " +  headerName +" = " + headers.get(headerName));
                get.addRequestHeader(headerName, headers.get(headerName));
            }
        }

        int code = client.executeMethod(get);
        if(code != 200) {
            throw new IOException("Connection returned code " + code);
        }

        return get.getResponseBodyAsStream();
    }


}