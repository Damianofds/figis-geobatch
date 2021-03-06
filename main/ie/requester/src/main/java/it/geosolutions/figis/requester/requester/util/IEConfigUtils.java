/*
 * ====================================================================
 *
 * Intersection Engine
 *
 * Copyright (C) 2007 - 2011 GeoSolutions S.A.S.
 * http://www.geo-solutions.it
 *
 * GPLv3 + Classpath exception
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by developers
 * of GeoSolutions.  For more information on GeoSolutions, please see
 * <http://www.geo-solutions.it/>.
 *
 */
package it.geosolutions.figis.requester.requester.util;

import java.io.FileNotFoundException;

import it.geosolutions.figis.model.Config;
import it.geosolutions.figis.model.ConfigXStreamMapper;
import it.geosolutions.figis.model.Intersection;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;


/**
 * Utility class for accessing ie-db
 * 
 * @author Alessio
 *
 */
public class IEConfigUtils
{

    private static final Logger LOGGER = Logger.getLogger(IEConfigUtils.class);

    // ////////////////////////////////////////////////////////////////////////////
    //
    // UTILITY METHODS
    //
    // ////////////////////////////////////////////////////////////////////////////

    /**
     * Parse the config xml file
     * 
     * @throws FileNotFoundException
     *
     */
    public static Config parseXMLConfig(String configPath) throws FileNotFoundException
    {
        Config xmlConfig = null;
        try
        {
            // READ THE XML AND CREATE A CONFIG OBJECT
            xmlConfig = ConfigXStreamMapper.init(configPath);
            LOGGER.info("Managing : " + configPath);
            // READ THE COMING CONFIG (XMLConfig) AND EVENTUALLY UPDATE THE CURRENT STATUS OF BOTH THE CONFIG AND THE INTERSECTIONS
            if (!validateConfig(xmlConfig))
            {
                LOGGER.error("Some errors exist in the config file, please check the log to discover it");
                throw new IllegalStateException(
                    "Some errors exist in the config file, please check the log to discover it");
            }

            return xmlConfig;
        }
        catch (FileNotFoundException e)
        {
            LOGGER.error("Failed to convert the '" + configPath + "' configuration file: " + e.getLocalizedMessage());
            throw e;
        }
    }

    /**
     * Method to validate configuration
     * 
     * @param config
     * @return
     */
    public static boolean validateConfig(Config config)
    {
        if (config == null)
        {
            LOGGER.error("The config object cannot be null");

            return false;
        }
        if (config.getGlobal() == null)
        {
            LOGGER.error("The global configuration cannot be null");

            return false;
        }
        if (config.getGlobal().getGeoserver() == null)
        {
            LOGGER.error("The geoserver configuration cannot be null");

            return false;
        }
        if (config.getGlobal().getGeoserver().getGeoserverUrl() == null)
        {
            LOGGER.error("The geoserver url  cannot be null");

            return false;
        }
        if (config.getGlobal().getGeoserver().getGeoserverUsername() == null)
        {
            LOGGER.error("The geoserver username  cannot be null");

            return false;
        }
        if (config.getGlobal().getDb() == null)
        {
            LOGGER.error("The db configuration  cannot be null");

            return false;
        }
        if (config.getGlobal().getDb().getDatabase() == null)
        {
            LOGGER.error("The db name  cannot be null");

            return false;
        }
        if (config.getGlobal().getDb().getHost() == null)
        {
            LOGGER.error("The db host  cannot be null");

            return false;
        }
        if (config.getGlobal().getDb().getUser() == null)
        {
            LOGGER.error("The db user name  cannot be null");

            return false;
        }
        if (config.getGlobal().getDb().getPort() == null)
        {
            LOGGER.error("The db port  cannot be null");

            return false;
        }
        if (config.intersections == null)
        {
            LOGGER.error("The intersection list cannot be null");

            return false;
        }
        LOGGER.info("The config check is successfull");

        return true;
    }

    /**********
     * this method checks whether two intersections are different comparing CRS, SrcCodeField, TrgCodeField, MaskLayer, PreserveTrgGeom isMask, srcLayer, trgLayer.
     * @param srcIntersection
     * @param trgIntersection
     * @return true if one of the parameters is different, false in the other case
     */
    public static boolean areIntersectionParameterDifferent(Intersection srcIntersection, Intersection trgIntersection)
    {
        if (!(compareNullAndWhitespaceSafe(srcIntersection.getSrcLayer(),trgIntersection.getSrcLayer())))
        {
            return true;
        }
        if (!(compareNullAndWhitespaceSafe(srcIntersection.getTrgLayer(),trgIntersection.getTrgLayer())))
        {
            return true;
        }
        if (!(compareNullAndWhitespaceSafe(srcIntersection.getAreaCRS(),trgIntersection.getAreaCRS())))
        {
            return true;
        }
        if (!(compareNullAndWhitespaceSafe(srcIntersection.getSrcCodeField(),trgIntersection.getSrcCodeField())))
        {
            return true;
        }
        if (!(compareNullAndWhitespaceSafe(srcIntersection.getTrgCodeField(),trgIntersection.getTrgCodeField())))
        {
            return true;
        }
        if (!(compareNullAndWhitespaceSafe(srcIntersection.getMaskLayer(),trgIntersection.getMaskLayer())))
        {
            return true;
        }
        if (!(srcIntersection.isMask() == trgIntersection.isMask()))
        {
            return true;
        }
        if (!(srcIntersection.isPreserveTrgGeom() == trgIntersection.isPreserveTrgGeom()))
        {
            return true;
        }

        return false;
    }
    
    /**
     * Compare if 2 Strings object are equals.
     * The input String will be trimmed (trail and lead, not inBetween whitheSpace),
     * the input Strings could be null: in case of a null input parameter the comparation will be done with an empty String
     * @param src
     * @param trg
     * @return True if the input are equals, False otherwise
     */
    public static boolean compareNullAndWhitespaceSafe(String src, String trg){
        
        if(src == null){
            src = "";
        }
        src = StringUtils.trimWhitespace(src);
        
        if(trg == null){
            trg = "";
        }
        trg = StringUtils.trimWhitespace(trg);
        
        return src.equals(trg);
       
    }
}
