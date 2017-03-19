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
package org.geotools.wmts;

import org.geotools.data.ows.OperationType;
import org.geotools.tile.impl.wmts.WMTSServiceType;

/**
 * @author ian
 *
 */
public class WMTSOperationType extends OperationType {
    WMTSServiceType type;

    /**
     * @return the type
     */
    public WMTSServiceType getType() {
        return type;
    }

    /**
     * @param kvp
     */
    public void setType(WMTSServiceType kvp) {
       type=kvp;
    }
}
