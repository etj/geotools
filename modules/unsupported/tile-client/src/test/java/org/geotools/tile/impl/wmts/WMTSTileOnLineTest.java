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

import java.net.URL;

import org.geotools.test.OnlineTestCase;
import org.geotools.tile.Tile;
import org.geotools.tile.TileService;
import org.geotools.tile.impl.WebMercatorZoomLevel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ian
 *
 */
public class WMTSTileOnLineTest extends OnlineTestCase {

    private Tile restTile;
    private Tile kvpTile;
    @Override
    protected void setUpInternal() throws Exception {
        URL serverURL = new URL(fixture.getProperty("kvp_server"));
        TileService kvpService = new WMTSService("states", serverURL.toExternalForm(),"topp:states","epsg:900913", WMTSServiceType.KVP);
        URL restWMTS = new URL(fixture.getProperty("rest_server"));
        TileService restService = new WMTSService("states", restWMTS.toExternalForm(),"topp:states","epsg4326", WMTSServiceType.REST);
        WMTSTileIdentifier tileIdentifier = new WMTSTileIdentifier(10, 12,
                new WebMercatorZoomLevel(5), kvpService.getName());

        this.restTile = new WMTSTile(tileIdentifier, restService);
        this.kvpTile = new WMTSTile(tileIdentifier, kvpService);
    }

    @Test
    public void testConstructor() {

        Assert.assertNotNull(this.restTile);
        Assert.assertNotNull(this.kvpTile);

    }

    @Test
    public void testGetURL() {
        Assert.assertEquals("http://raspberrypi:8080/geoserver/gwc/service/wmts?request=getTile&tilematrixset=epsg%3A900913&TileRow=12&service=WMTS&format=image%2Fpng&style=&TileCol=10&version=1.0.0&layer=topp%3Astates&TileMatrix=epsg%3A900913%3A5&",
                this.kvpTile.getUrl().toString());

        
        
        Assert.assertEquals("http://raspberrypi:9000/wmts/topp:states/epsg4326/5/10/12.png",
                this.restTile.getUrl().toString());
    }

    @Override
    protected String getFixtureId() {
    
        return "wmts";
    }

}
