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

import org.geotools.tile.Tile;
import org.geotools.tile.TileFactory;
import org.geotools.tile.TileService;
import org.geotools.tile.impl.ZoomLevel;

/**
 * Implementation of TileFactory for WMTS
 * 
 * @author ian
 *
 */
public class WMTSTileFactory extends TileFactory {

    /**
     * 
     */
    public WMTSTileFactory() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public Tile findTileAtCoordinate(double lon, double lat, ZoomLevel zoomLevel,
            TileService service) {

        lat = TileFactory.normalizeDegreeValue(lat, 90);
        lon = TileFactory.normalizeDegreeValue(lon, 180);
        System.out.println(lat + "," + lon);
        int xTile = (int) Math.floor((lon + 180) / 360 * (1 << zoomLevel.getZoomLevel()));
        int yTile = (int) Math.floor(
                (1 - Math.log(Math.tan(lat * Math.PI / 180) + 1 / Math.cos(lat * Math.PI / 180))
                        / Math.PI) / 2 * (1 << zoomLevel.getZoomLevel()));

        return new WMTSTile(xTile, yTile, zoomLevel, service);
    }

    @Override
    public ZoomLevel getZoomLevel(int zoomLevel, TileService service) {
        return new WMTSZoomLevel(zoomLevel, (WMTSService) service);
    }

    @Override
    public Tile findRightNeighbour(Tile tile, TileService service) {

        return new WMTSTile((WMTSTileIdentifier) tile.getTileIdentifier().getRightNeighbour(),
                service);
    }

    @Override
    public Tile findLowerNeighbour(Tile tile, TileService service) {

        return new WMTSTile((WMTSTileIdentifier) tile.getTileIdentifier().getLowerNeighbour(),
                service);
    }

}
