/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://code.google.com/p/geobatch/
 *  Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.geobatch.figis.intersection;

import it.geosolutions.geobatch.catalog.Configuration;
import it.geosolutions.geobatch.configuration.event.action.ActionConfiguration;


/**
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */

public class IntersectionConfiguration extends ActionConfiguration implements Configuration
{
    String persistencyHost = null;

    int itemsPerPages = -1;

    String ieServiceUsername = null;

    String ieServicePassword = null;

    public IntersectionConfiguration(String id, String name, String description)
    {
        super(id, name, description);
        // TODO INITIALIZE MEMBERS
    }

    public String getIeServiceUsername()
    {
        return ieServiceUsername;
    }

    public void setIeServiceUsername(String ieServiceUsername)
    {
        this.ieServiceUsername = ieServiceUsername;
    }

    public String getIeServicePassword()
    {
        return ieServicePassword;
    }

    public void setIeServicePassword(String ieServicePassword)
    {
        this.ieServicePassword = ieServicePassword;
    }

    // TODO ADD YOUR MEMBERS

    public String getPersistencyHost()
    {
        return persistencyHost;
    }

    public void setPersistencyHost(String persistencyHost)
    {
        this.persistencyHost = persistencyHost;
    }

    public int getItemsPerPages()
    {
        return itemsPerPages;
    }

    public void setItemsPerPages(int itemsPerPages)
    {
        this.itemsPerPages = itemsPerPages;
    }

    @Override
    public IntersectionConfiguration clone()
    {
        final IntersectionConfiguration ret = (IntersectionConfiguration) super.clone();

        // TODO CLONE YOUR MEMBERS
        ret.setPersistencyHost(persistencyHost);
        ret.setWorkingDirectory(this.getWorkingDirectory());
        ret.setItemsPerPages(itemsPerPages);
        ret.setIeServiceUsername(ieServiceUsername);
        ret.setIeServicePassword(ieServicePassword);
        ret.setServiceID(this.getServiceID());
        ret.setListenerConfigurations(ret.getListenerConfigurations());

        return ret;
    }

}
