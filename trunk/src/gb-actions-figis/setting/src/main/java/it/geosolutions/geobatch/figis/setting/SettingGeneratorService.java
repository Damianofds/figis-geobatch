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
package it.geosolutions.geobatch.figis.setting;

import java.util.EventObject;

import it.geosolutions.geobatch.actions.tools.configuration.Path;
import it.geosolutions.geobatch.catalog.impl.BaseService;
import it.geosolutions.geobatch.flow.event.action.ActionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class SettingGeneratorService extends BaseService implements ActionService<EventObject, SettingConfiguration>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingGeneratorService.class);

    public SettingGeneratorService(String id, String name, String description)
    {
        super(id, name, description);
    }

    public SettingAction createAction(SettingConfiguration configuration)
    {
        try
        {
            return new SettingAction(configuration);
        }
        catch (Exception e)
        {
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info(e.getLocalizedMessage(), e);
            }

            return null;
        }
    }

    public boolean canCreateAction(SettingConfiguration configuration)
    {
        LOGGER.info("------------------->Checking setting parameters");
        try
        {
            // absolutize working dir
            String wd = Path.getAbsolutePath(configuration.getWorkingDirectory());
            String defaultMaskLayer = configuration.getDefaultMaskLayer();
            String host = configuration.getPersistencyHost();
            if (wd != null)
            {
                configuration.setWorkingDirectory(wd);
                // return true;
            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("SettingGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get an absolute working dir.");
                }

                return false;
            }

            if (host != null)
            {
                LOGGER.info("Host value is " + host);

            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("SettingGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the persistence host.");
                }

                return false;
            }

            if (defaultMaskLayer != null)
            {
                LOGGER.info("The default mask layer is " + defaultMaskLayer);

            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("SettingGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the default mask layer.");
                }

                return false;
            }
        }
        catch (Throwable e)
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(e.getLocalizedMessage(), e);
            }

            return false;
        }

        return true;
    }

}
