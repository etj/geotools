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

import java.util.List;
import java.util.logging.Logger;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.tile.Tile;
import org.geotools.tile.TileFactory;
import org.geotools.tile.TileService;
import org.geotools.tile.impl.ZoomLevel;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;

/**
 * Implementation of TileFactory for WMTS
 * 
 * @author ian
 *
 */
public class WMTSTileFactory extends TileFactory {
    /** pixelSizeMeters */
    private static final double PixelSizeMeters = 0.28e-3;
    private static final Logger LOGGER = Logging
            .getLogger(WMTSTileFactory.class.getPackage().getName());

    /**
     * 
     */
    public WMTSTileFactory() {
        // TODO Auto-generated constructor stub
    }

    
    @Override
    public Tile findTileAtCoordinate(double lon, double lat, ZoomLevel zoomLevel,
            TileService service) {
        WMTSZoomLevel zl = (WMTSZoomLevel) zoomLevel;
        TileMatrix tileMatrix = ((WMTSService) service).getMatrixSet().getMatrices()
                .get(zl.getZoomLevel());
        List<TileMatrixLimits> limits = ((WMTSService) service).getLimits();
        TileMatrixLimits tileMatrixLimits;
        if (limits != null && zl.getZoomLevel() < limits.size()) {
            tileMatrixLimits = limits.get(zl.getZoomLevel());
        } else { // probably a REST API with no limits
            tileMatrixLimits = new TileMatrixLimits();
            tileMatrixLimits.setMinCol(0L);
            tileMatrixLimits.setMinRow(0L);
            tileMatrixLimits.setMaxCol(tileMatrix.matrixWidth);
            tileMatrixLimits.setMaxRow(tileMatrix.matrixHeight);
        }

        
        double pixelSpan = getPixelSpan(tileMatrix);
        
        double tileSpanY = (tileMatrix.getTileHeight() * pixelSpan);
        double tileSpanX = (tileMatrix.getTileWidth() * pixelSpan);
        double tileMatrixMinX;
        double tileMatrixMaxY;
        if (TileMatrix.isGeotoolsLongitudeFirstAxisOrderForced()
                || tileMatrix.getCrs().getCoordinateSystem().getAxis(0).getDirection().equals(AxisDirection.EAST)) {
            tileMatrixMinX = tileMatrix.getTopLeft().getX();
            tileMatrixMaxY = tileMatrix.getTopLeft().getY();
        } else {
            tileMatrixMaxY = tileMatrix.getTopLeft().getX();
            tileMatrixMinX = tileMatrix.getTopLeft().getY();
        }
        // to compensate for floating point computation inaccuracies
        double epsilon = 1e-6;
        long xTile = (int) Math.floor((lon - tileMatrixMinX) / tileSpanX + epsilon);
        long yTile = (int) Math.floor((tileMatrixMaxY - lat) / tileSpanY + epsilon);
        // to avoid requesting out-of-range tiles
        //DEBUG
        System.out.println("xTile: "+xTile+" yTile: "+yTile);
        System.out.println("cols:"+tileMatrixLimits.mincol+" - "+tileMatrixLimits.maxcol);
        System.out.println("rows:"+tileMatrixLimits.minrow+" - "+tileMatrixLimits.maxrow);
        if (xTile >= tileMatrixLimits.maxcol)
            xTile = tileMatrixLimits.maxcol - 1;
        if (yTile >= tileMatrixLimits.maxrow)
            yTile = tileMatrixLimits.maxrow - 1;

        if (xTile < tileMatrixLimits.mincol)
            xTile = tileMatrixLimits.mincol;
        if (yTile < tileMatrixLimits.minrow)
            yTile = tileMatrixLimits.minrow;
        System.out.println("xTile: "+xTile+" yTile: "+yTile);
        LOGGER.fine("fetching tile: " + xTile + " " + yTile + " " + zoomLevel.getZoomLevel());
        /*System.out.println("tile at " + lat + "," + lon + " is z:" + zoomLevel + " x:" + xTile
                + " y:" + yTile);*/
        return new WMTSTile((int) xTile, (int) yTile, zoomLevel, service);
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

    /**
     * @param tileIdentifier
     * @return
     */
    public static ReferencedEnvelope getExtentFromTileName(WMTSTileIdentifier tileIdentifier,
            TileService service) {
        WMTSZoomLevel zl = new WMTSZoomLevel(tileIdentifier.getZ(), (WMTSService) service);
        TileMatrix tileMatrix = ((WMTSService) service).getMatrixSet().getMatrices()
                .get(zl.getZoomLevel());
        
        CoordinateReferenceSystem crs = tileMatrix.getCrs();
        CoordinateSystem coordinateSystem = crs.getCoordinateSystem();
        
        double pixelSpan = getPixelSpan(tileMatrix);
        double tileSpanY = (tileMatrix.getTileHeight() * pixelSpan);
        double tileSpanX = (tileMatrix.getTileWidth() * pixelSpan);
       
        double tileMatrixMinX;
        double tileMatrixMaxY;
        boolean longFirst = TileMatrix.isGeotoolsLongitudeFirstAxisOrderForced()
                || coordinateSystem.getAxis(0).getDirection().equals(AxisDirection.EAST);
        if (longFirst) {
            tileMatrixMinX = tileMatrix.getTopLeft().getX();
            tileMatrixMaxY = tileMatrix.getTopLeft().getY();
        } else {
            tileMatrixMaxY = tileMatrix.getTopLeft().getX();
            tileMatrixMinX = tileMatrix.getTopLeft().getY();
        }
        ReferencedEnvelope ret = new ReferencedEnvelope(crs);
        double minX = tileIdentifier.getX() * tileSpanX + tileMatrixMinX;
        double maxY = tileMatrixMaxY - tileIdentifier.getY() * tileSpanY;
        double maxX = minX + tileSpanX;
        double minY = maxY - tileSpanY;
        if (longFirst) {
            ret.expandToInclude(minX, minY);
            ret.expandToInclude(maxX, maxY);
        } else {
            ret.expandToInclude(minY, minX);
            ret.expandToInclude(maxY, maxX);
        }

        return ret;
    }


    /**
     * @param tileMatrix
     * @param unit
     * @return
     */
    private static double getPixelSpan(TileMatrix tileMatrix) {
        CoordinateSystem coordinateSystem = tileMatrix.getCrs().getCoordinateSystem();
        Unit<?> unit = coordinateSystem.getAxis(0).getUnit();
        double pixelSpan = tileMatrix.denominator * PixelSizeMeters;// now divide by meters per unit!
        if (unit.equals(NonSI.DEGREE_ANGLE)) {
            /*// use the length of a degree at the equator = 60 nautical miles!
            unit = NonSI.NAUTICAL_MILE;
            UnitConverter metersperunit = unit.getConverterTo(SI.METER);
            pixelSpan /= metersperunit.convert(60.0);*/
            
            //constant value from https://msi.nga.mil/MSISiteContent/StaticFiles/Calculators/degree.html
            //apparently - 60.10764611706782 NaMiles
            pixelSpan /= 111319;
        } else {
            UnitConverter metersperunit = unit.getConverterTo(SI.METER);
            pixelSpan /= metersperunit.convert(1);
        }
        return pixelSpan;
    }

}
