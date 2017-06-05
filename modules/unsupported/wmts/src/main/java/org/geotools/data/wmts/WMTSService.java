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
package org.geotools.data.wmts;

import org.eclipse.emf.common.util.EList;
import org.geotools.data.ows.Service;

import net.opengis.ows11.LanguageStringType;
import net.opengis.ows11.ServiceIdentificationType;
import net.opengis.ows11.impl.KeywordsTypeImpl;

/**
 * @author ian
 *
 */
public class WMTSService extends Service {
    /**
     * 
     */
    public WMTSService(ServiceIdentificationType serviceType) {
        setTitle(((LanguageStringType) serviceType.getTitle().get(0)).getValue());
        setName(serviceType.getServiceType().getValue());
        EList kws = serviceType.getKeywords();
        String[] keywords = new String[kws.size()];
        for(int i=0;i<kws.size();i++) {
            keywords[i] = ((KeywordsTypeImpl)kws.get(i)).getKeyword().toString();
        }
        setKeywordList(keywords);
    }
}
