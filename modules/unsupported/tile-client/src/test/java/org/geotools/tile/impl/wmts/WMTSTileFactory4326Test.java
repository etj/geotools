/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2015, Open Source Geospatial Foundation (OSGeo)
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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.tile.Tile;
import org.geotools.tile.TileFactory;
import org.geotools.tile.TileFactoryTest;
import org.geotools.tile.TileService;

import org.junit.Assert;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.opengis.referencing.FactoryException;

public class WMTSTileFactory4326Test extends TileFactoryTest {
    class TestPoint {
        double lat;

        double lon;

        int zoomlevel;

        int expectedRow;

        int expectedCol;

        /**
         * @param lat
         * @param lon
         * @param zoomlevel
         * @param expectedRow
         * @param expectedCol
         */
        public TestPoint(double lat, double lon, int zoomlevel, int expectedRow,
                int expectedCol) {
            super();
            this.lat = lat;
            this.lon = lon;
            this.zoomlevel = zoomlevel;
            this.expectedRow = expectedRow;
            this.expectedCol = expectedCol;
        }

    }

    @Test
    public void testGetTileFromCoordinate() {
        int i=0;
        TileService[] services = new TileService[2];
        for (WMTSServiceType t : WMTSServiceType.values()) {
            
            services[i++] = createKVPService(); // TODO: create a testpoint array for REST too

        }
        TestPoint[] tests = {
            new TestPoint(90, -180, 2, 0, 0),
            new TestPoint(75, -173, 2, 0, 0),
            new TestPoint(90, -180, 0, 0, 0),
            new TestPoint(0, 0, 3, 3, 7),
            new TestPoint(0, 0, 2, 1, 3),
            new TestPoint(0, 0, 1, 0, 1),
            new TestPoint(50, -70, 0, 0, 0),
            new TestPoint(50, 70, 0, 0, 1),
            new TestPoint(-50, -70, 1, 1, 1),
            new TestPoint(50, 70, 1, 0, 2),
            new TestPoint(50, -70, 1, 0, 1),
            new TestPoint(-50, 70, 1, 1, 2)
        };
        
        for (TestPoint tp : tests) {
            for (int i1=0;i1<2;i1++) {
                TileService service = services[i1];
                // For some reason map proxy has an extra level compared to GeoServer!
                int offset = 0;
                if (((WMTSService)service).getType().equals(WMTSServiceType.REST)) {
                    offset = 1;
                }
                WMTSZoomLevel zoomLevel = ((WMTSService) service).getZoomLevel(tp.zoomlevel + offset);// new WMTSZoomLevel(1,(WMTSService) service);
                // top right
                Tile tile = factory.findTileAtCoordinate(tp.lon, tp.lat, zoomLevel, service);

                WMTSTile expectedTile = new WMTSTile(tp.expectedCol, tp.expectedRow, zoomLevel, service);
                System.out.println(tp.lat+","+ tp.lon+" expected:"+expectedTile+" got "+tile);
                Assert.assertEquals(""+tp.lat+","+ tp.lon,expectedTile.getTileIdentifier(), tile.getTileIdentifier());

            }
        }

    }

    @Test
    public void testFindRightNeighbour() {
        for (WMTSServiceType t : WMTSServiceType.values()) {
            WMTSService service = (WMTSService) createService(t);
            WMTSZoomLevel zoomLevel = service.getZoomLevel(5);
            WMTSTile tile = new WMTSTile(20, 15, zoomLevel, service);

            Tile neighbour = factory.findRightNeighbour(tile, service);
            Assert.assertNotNull(neighbour);
            // assertTrue(neighbour.getContextState().equals(ContextState.OKAY));
            WMTSTile expectedNeighbour = new WMTSTile(21, 15, zoomLevel, service);

            Assert.assertEquals(expectedNeighbour.getTileIdentifier(),
                    neighbour.getTileIdentifier());
        }
    }

    @Test
    public void testFindLowerNeighbour() {
        for (WMTSServiceType t : WMTSServiceType.values()) {
            TileService service = createService(t);
            WMTSTile tile = new WMTSTile(10, 5, new WMTSZoomLevel(5, (WMTSService) service),
                    service);

            Tile neighbour = factory.findLowerNeighbour(tile, service);

            WMTSTile expectedNeighbour = new WMTSTile(10, 6,
                    new WMTSZoomLevel(5, (WMTSService) service), service);

            Assert.assertEquals(expectedNeighbour.getTileIdentifier(),
                    neighbour.getTileIdentifier());
        }

    }

    @Test
    public void testGetExtentFromTileName() throws FactoryException {

        WMTSService services[] = {
            createRESTService(),
            createKVPService()};

        ReferencedEnvelope expectedEnv[] = {
            new ReferencedEnvelope(1102848.0, 2151424.0, -951424.0, 97152.0, CRS.decode("EPSG:31287")),
            new ReferencedEnvelope(-90, 0.00, -90.0, 0.0, DefaultGeographicCRS.WGS84)};

        for (int i = 0; i < 2; i++) {
            TileService service = services[i];
            // For some reason map proxy has an extra level compared to GeoServer!
            int offset = 0;
            if (((WMTSService)service).getType().equals(WMTSServiceType.REST)) {
                offset = 1;
            }
            WMTSZoomLevel zoomLevel = ((WMTSService) service).getZoomLevel(1 + offset);
            WMTSTileIdentifier tileId = new WMTSTileIdentifier(1, 1,
                    zoomLevel, "SomeName");
            WMTSTile tile = new WMTSTile(tileId, service);

            ReferencedEnvelope env = WMTSTileFactory.getExtentFromTileName(tileId,service);

            Assert.assertEquals(tile.getExtent(), env);

            Assert.assertEquals(service.getName(), expectedEnv[i].getMinX(), env.getMinX(), 0.001);
            Assert.assertEquals(service.getName(), expectedEnv[i].getMinY(), env.getMinY(), 0.001);
            Assert.assertEquals(service.getName(), expectedEnv[i].getMaxX(), env.getMaxX(), 0.001);
            Assert.assertEquals(service.getName(), expectedEnv[i].getMaxY(), env.getMaxY(), 0.001);
        }
    }

    private TileService createService(WMTSServiceType type) {

        if (WMTSServiceType.REST.equals(type)) {
            return createRESTService();
        } else {
            return createKVPService();
        }
    }

    private WMTSService createRESTService() {
        try {
            URL capaResource = getClass().getClassLoader().getResource("WMTSCapabilities.xml");
            assertNotNull("Can't find REST getCapa resource", capaResource);
            File capaFile = new File(capaResource.toURI());
            assertTrue("Can't find REST getCapa file", capaFile.exists());

            String baseURL = "XXXhttp://wmsx.zamg.ac.at/mapcacheStatmap/wmts/1.0.0/WMTSCapabilities.xml";
            return new WMTSService("REST", baseURL, "grey", "statmap", WMTSServiceType.REST, capaFile);

        } catch (URISyntaxException ex) {
            fail(ex.getMessage());
            return null;
        }
    }

    private WMTSService createKVPService() {
        try {
            URL capaKvp = getClass().getClassLoader().getResource("getcapa_kvp.xml");
            assertNotNull(capaKvp);
            File capaKvpFile = new File(capaKvp.toURI());

            String baseURL = "http://demo.geo-solutions.it/geoserver/gwc/service/wmts?REQUEST=getcapabilities";
            return new WMTSService("KVP", baseURL, "unesco:Unesco_point", "EPSG:4326", WMTSServiceType.KVP, capaKvpFile);

        } catch (URISyntaxException ex) {
            fail(ex.getMessage());
            return null;
        }
    }

    protected TileFactory createFactory() {
        return new WMTSTileFactory();
    }
}
