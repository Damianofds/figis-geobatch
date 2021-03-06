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
package it.geosolutions.figis;

import java.net.MalformedURLException;
import java.util.List;

import org.apache.log4j.Logger;


import it.geosolutions.figis.model.Config;
import it.geosolutions.figis.model.Global;
import it.geosolutions.figis.model.Intersection;
import it.geosolutions.figis.model.Intersection.Status;
import it.geosolutions.figis.requester.Request;

import junit.framework.TestCase;


/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase
{
	final static Logger LOGGER = Logger.getLogger(AppTest.class.toString());
    Config config = new Config();
    String host = "http://localhost:9999";
    String ieServiceUsername = "admin";
    String ieServicePassword = "abramisbrama";

    @Override
    protected void setUp()
    {

        try
        {
            Global global = new Global();
            global.getGeoserver().setGeoserverUsername("admin");
            global.getGeoserver().setGeoserverPassword("password");
            global.getGeoserver().setGeoserverUrl("localhost");
            global.getDb().setDatabase("trial");
            global.getDb().setHost("localhost");
            global.getDb().setPassword("password");
            global.getDb().setPort("8082");
            global.getDb().setSchema("empty");
            global.getDb().setUser("dbuser");
            config.setUpdateVersion(1);
            config.setGlobal(global);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    public void testDeleteConfig() throws java.net.MalformedURLException
    {
        LOGGER.trace("Start testDeleteConfig");
        Request.initConfig();

        long id1 = Request.insertConfig(host, config, ieServiceUsername, ieServicePassword);
        long id2 = Request.insertConfig(host, config, ieServiceUsername, ieServicePassword);
        List<Config> list = Request.getConfigs(host, ieServiceUsername, ieServicePassword);
        for (Config conf : list)
        {
            boolean resultDelete = Request.deleteConfig(host, conf.getConfigId(), ieServiceUsername, ieServicePassword);
            assertTrue(resultDelete);
        }
    }

    public void testExistConfigBeforeAndAfter() throws java.net.MalformedURLException
    {
        LOGGER.trace("Start testExistConfigBeforeAndAfter");
        Request.initConfig();

        Config confBefore = Request.existConfig(host, ieServiceUsername, ieServicePassword);
        assertTrue(confBefore == null);

        long id = Request.insertConfig(host, config, ieServiceUsername, ieServicePassword);
        Config confAfter = Request.existConfig(host, ieServiceUsername, ieServicePassword);
        assertTrue(confAfter != null);
    }

    public void testUpdateConfig() throws java.net.MalformedURLException
    {
        LOGGER.trace("testUpdateConfig");

        Config upConfig = new Config();
        // init updating fields
        String newGSpassword = "geoserver";
        String newGSusername = "admin";
        String newGSURL = "localhost:8080";
        String newdatabase = "FIDEVQC";
        String newHost = "localhost";
        String newSBpassword = "FIGIS";
        String newPort = "1521";
        String newSchema = "FIGIS_GIS";
        String newDBuser = "FIGIS_GIS";

        // new update object
        Global global = new Global();
        global.getGeoserver().setGeoserverUsername(newGSusername);
        global.getGeoserver().setGeoserverPassword(newGSpassword);
        global.getGeoserver().setGeoserverUrl(newGSURL);
        global.getDb().setDatabase(newdatabase);
        global.getDb().setHost(newHost);
        global.getDb().setPassword(newSBpassword);
        global.getDb().setPort(newPort);
        global.getDb().setSchema(newSchema);
        global.getDb().setUser(newDBuser);
        upConfig.setUpdateVersion(2);
        upConfig.setGlobal(global);

        Config confBeforeUpdate = Request.existConfig(host, ieServiceUsername, ieServicePassword);
        assertTrue(confBeforeUpdate != null);

        long updateID = Request.updateConfig(host, confBeforeUpdate.getConfigId(), upConfig, ieServiceUsername, ieServicePassword);
        Config confAfterUpdate = Request.getConfigByID(host, updateID, ieServiceUsername, ieServicePassword);
        assertTrue(confAfterUpdate.getGlobal().getGeoserver().getGeoserverUsername().equals(newGSusername));
        assertTrue(confAfterUpdate.getGlobal().getGeoserver().getGeoserverPassword().equals(newGSpassword));
        assertTrue(confAfterUpdate.getGlobal().getGeoserver().getGeoserverUrl().equals(newGSURL));
        assertTrue(confAfterUpdate.getGlobal().getDb().getDatabase().equals(newdatabase));
        assertTrue(confAfterUpdate.getGlobal().getDb().getUser().equals("FIGIS_GIS"));
    }


    public void testListAndDeleteIntersections() throws MalformedURLException
    {
        try
        {
            Request.initIntersection();

            Intersection int1 = new Intersection(false, true, true, "srcLayer", "trgLayer", "srcCodeField",
                    "trgCodeField", "maskLayer", "areaCRS", Status.TOCOMPUTE);
            Request.insertIntersection(host, int1, ieServiceUsername, ieServicePassword);
            LOGGER.trace("AFTER INTERSECTION");

            List<Intersection> list = Request.getAllIntersections(host, ieServiceUsername, ieServicePassword);
            LOGGER.trace("SIZE OF " + list.size());
            for (Intersection intersection : list)
            {
                boolean value = Request.deleteIntersectionById(host, intersection.getId(), ieServiceUsername, ieServicePassword);
                assertTrue(value);
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }

    }

    public void testInsertAndGetAllIntersections() throws MalformedURLException
    {
        Intersection int1 = new Intersection(true, true, true, "srcLayer", "trgLayer", "srcCodeField",
                "trgCodeField", "maskLayer", "areaCRS", Status.TOCOMPUTE);
        Intersection int2 = new Intersection(true, true, false, "srcLayer2", "trgLayer2", "srcCodeField2",
                "trgCodeField", "maskLayer2", "areaCRS2", Status.COMPUTING);
        assertTrue(Request.insertIntersection(host, int1, ieServiceUsername, ieServicePassword) != 0);
        assertTrue(Request.insertIntersection(host, int2, ieServiceUsername, ieServicePassword) != 0);

        List<Intersection> list = Request.getAllIntersections(host, ieServiceUsername, ieServicePassword);
        assertTrue(list.size() == 2);
    }


    public void testDeleteAllandUpdate() throws MalformedURLException
    {
        try
        {
            assertTrue(Request.deleteAllIntersections(host, ieServiceUsername, ieServicePassword));

            Intersection int1 = new Intersection(false, true, true, "sf:restricted", "sf:restricted", "cat",
                    "cat", "maskLayer", "areaCRS", Status.TOCOMPUTE);
            long id = Request.insertIntersection(host, int1, ieServiceUsername, ieServicePassword);
            Intersection int2 = new Intersection(false, true, true, "sf:restricted", "sf:restricted", "cat",
                    "cat", "maskLayer", "areaCRS", Status.TOCOMPUTE);
            Request.updateIntersectionById(host, id, int2, ieServiceUsername, ieServicePassword);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

}
