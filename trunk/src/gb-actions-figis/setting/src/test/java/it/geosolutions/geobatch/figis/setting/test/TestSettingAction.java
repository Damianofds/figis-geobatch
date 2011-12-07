package it.geosolutions.geobatch.figis.setting.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import it.geosolutions.figis.model.Config;
import it.geosolutions.figis.model.Global;
import it.geosolutions.figis.model.Intersection;
import it.geosolutions.figis.model.Intersection.Status;
import it.geosolutions.figis.requester.Request;
import it.geosolutions.figis.requester.requester.util.IEConfigUtils;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;
import it.geosolutions.geobatch.figis.setting.SettingAction;
import it.geosolutions.geobatch.figis.setting.SettingConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;


public class TestSettingAction
{

    private static final Logger log = LoggerFactory.getLogger(TestSettingAction.class);

    private SettingAction settingAction = null;
    private Config xmlConfig = null;
    private Config dbConfig = null;

    @Before
    public void setUp() throws Exception
    {
        File inputFile = null;
        try
        {
            inputFile = File.createTempFile("ie-config", ".xml");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        Queue<EventObject> queue = new LinkedBlockingQueue<EventObject>();
        queue.add(new FileSystemEvent(inputFile, FileSystemEventType.FILE_ADDED));

        SettingConfiguration cronConfiguration = new SettingConfiguration("id", "name", " description");
        settingAction = new SettingAction(cronConfiguration);

        xmlConfig = null;
        dbConfig = null;

        Request.initConfig();
        Request.initIntersection();
    }

    private Config initDBConfig(int version, String geoUrl, String geoUser,
        String geoPwd, String dbSchema, String dbName, String dbHost,
        String port, String dbUser, String dbPwd, List<Intersection> intersections)
    {
        Config config = new Config();
        Global global = new Global();
        global.getGeoserver().setGeoserverUsername(geoUser);
        global.getGeoserver().setGeoserverPassword(geoPwd);
        global.getGeoserver().setGeoserverUrl(geoUrl);
        global.getDb().setDatabase(dbName);
        global.getDb().setHost(dbHost);
        global.getDb().setPassword(dbPwd);
        global.getDb().setPort(port);
        global.getDb().setSchema(dbSchema);
        global.getDb().setUser(dbUser);
        config.setUpdateVersion(version);
        config.setGlobal(global);

        config.intersections = intersections;

        return config;
    }

    @Test
    public void testCase0_InvalidUpdateVersions() throws MalformedURLException, URISyntaxException,
        FileNotFoundException, CloneNotSupportedException
    {
        File ieConfig = new File(TestSettingAction.class.getResource("ie-config.xml").toURI());

        // READ THE XML AND CREATE A CONFIG OBJECT
        xmlConfig = IEConfigUtils.parseXMLConfig(ieConfig.getAbsolutePath());

        dbConfig = initDBConfig(0, null, null, null, null, null, null, null, null, null, null);

        List<Intersection> intersectionsToAdd = new ArrayList<Intersection>();
        SettingAction.compareXMLConfigAndDBConfig(xmlConfig, dbConfig, intersectionsToAdd);

        assertTrue("No intersections to add found", intersectionsToAdd.size() == 0);
        assertTrue("Untouched DB configuration", dbConfig.getUpdateVersion() == 0);
    }

    @Test
    public void testCase1_InsertIntersectionsOnEmptyDB() throws MalformedURLException, URISyntaxException,
        FileNotFoundException, CloneNotSupportedException
    {
        File ieConfig = new File(TestSettingAction.class.getResource("ie-config.xml").toURI());

        // READ THE XML AND CREATE A CONFIG OBJECT
        xmlConfig = IEConfigUtils.parseXMLConfig(ieConfig.getAbsolutePath());

        dbConfig = initDBConfig(-1, null, null, null, null, null, null, null, null, null, null);

        List<Intersection> intersectionsToAdd = new ArrayList<Intersection>();
        SettingAction.compareXMLConfigAndDBConfig(xmlConfig, dbConfig, intersectionsToAdd);

        assertTrue("Found intersections to add", xmlConfig.intersections.size() == intersectionsToAdd.size());
        assertTrue("Updated DB configuration", dbConfig.getUpdateVersion() == 0);

        for (Intersection intersection : intersectionsToAdd)
        {
            assertTrue(intersection.getStatus().equals(Status.TOCOMPUTE));
        }
    }

    @Test
    public void testCase2_ForceOneIntersectionRecomputationLeavingOthersUntouched() throws MalformedURLException,
        URISyntaxException, FileNotFoundException, CloneNotSupportedException
    {
        File ieConfig = new File(TestSettingAction.class.getResource("ie-config-force.xml").toURI());

        // READ THE XML AND CREATE A CONFIG OBJECT
        xmlConfig = IEConfigUtils.parseXMLConfig(ieConfig.getAbsolutePath());

        // new Intersection(mask, force, preserveTrgGeom, "srcLayer", "trgLayer", "srcCodeField", "trgCodeField", "maskLayer", "areaCRS", Status)
        Intersection intersection1 = new Intersection(true, false, false, "fifao:FAO_DIV", "fifao:NJA", "F_SUBAREA",
                "ISO3_TERRI", "fifao:UN_CONTINENT", "EPSG:54012", Status.COMPUTED);
        Intersection intersection2 = new Intersection(true, false, false, "fifao:FAO_SUB_DIV", "fifao:NJA",
                "F_SUBDIVIS",
                "ISO3_TERRI", "fifao:UN_CONTINENT", "EPSG:54012", Status.COMPUTED);
        Intersection intersection3 = new Intersection(true, false, false, "fifao:FAO_MAJOR", "fifao:ICCAT_SMU",
                "F_AREA",
                "ICCAT_SMU", "fifao:UN_CONTINENT", "EPSG:54012", Status.COMPUTED);
        Intersection intersection4 = new Intersection(true, false, false, "fifao:NJA", "fifao:ICCAT_SMU",
                "ISO3_TERRI",
                "ICCAT_SMU", "fifao:UN_CONTINENT", "EPSG:54012", Status.TODELETE);
        dbConfig = initDBConfig(-1, null, null, null, null, null, null, null, null, null,
                Arrays.asList(intersection1, intersection2, intersection3, intersection4));

        List<Intersection> intersectionsToAdd = new ArrayList<Intersection>();
        SettingAction.compareXMLConfigAndDBConfig(xmlConfig, dbConfig, intersectionsToAdd);

        assertTrue("Found intersections to add", intersectionsToAdd.size() == dbConfig.intersections.size());
        assertTrue("Updated DB configuration", dbConfig.getUpdateVersion() == 0);

        assertTrue(intersectionsToAdd.get(0).getStatus().equals(Status.TOCOMPUTE));
        assertTrue(intersectionsToAdd.get(1).getStatus().equals(Status.COMPUTED));
        assertTrue(intersectionsToAdd.get(2).getStatus().equals(Status.COMPUTED));
        assertTrue(intersectionsToAdd.get(3).getStatus().equals(Status.TODELETE));
    }

    @Test
    public void testCase3_AddNewIntersectionsToDBWhileComputing() throws MalformedURLException, URISyntaxException,
        FileNotFoundException, CloneNotSupportedException
    {
        File ieConfig = new File(TestSettingAction.class.getResource("ie-config.xml").toURI());

        // READ THE XML AND CREATE A CONFIG OBJECT
        xmlConfig = IEConfigUtils.parseXMLConfig(ieConfig.getAbsolutePath());

        // new Intersection(mask, force, preserveTrgGeom, "srcLayer", "trgLayer", "srcCodeField", "trgCodeField", "maskLayer", "areaCRS", Status)
        Intersection intersection1 = new Intersection(true, false, false, "fifao:FAO_DIV", "fifao:NJA", "F_SUBAREA",
                "ISO3_TERRI", "fifao:UN_CONTINENT", "EPSG:54012", Status.COMPUTING);
        dbConfig = initDBConfig(-1, null, null, null, null, null, null, null, null, null,
                Arrays.asList(intersection1));

        List<Intersection> intersectionsToAdd = new ArrayList<Intersection>();
        SettingAction.compareXMLConfigAndDBConfig(xmlConfig, dbConfig, intersectionsToAdd);

        assertTrue("Found intersections to add", xmlConfig.intersections.size() == intersectionsToAdd.size());
        assertTrue("Updated DB configuration", dbConfig.getUpdateVersion() == 0);

        assertTrue(intersectionsToAdd.get(0).getStatus().equals(Status.COMPUTING));
        assertTrue(intersectionsToAdd.get(1).getStatus().equals(Status.TOCOMPUTE));
        assertTrue(intersectionsToAdd.get(2).getStatus().equals(Status.TOCOMPUTE));
    }

    @Test
    public void testCase4_CleanOnlyExistingIntersections() throws MalformedURLException, URISyntaxException,
        FileNotFoundException, CloneNotSupportedException
    {
        File ieConfig = new File(TestSettingAction.class.getResource("ie-config-clean.xml").toURI());

        // READ THE XML AND CREATE A CONFIG OBJECT
        xmlConfig = IEConfigUtils.parseXMLConfig(ieConfig.getAbsolutePath());

        // new Intersection(mask, force, preserveTrgGeom, "srcLayer", "trgLayer", "srcCodeField", "trgCodeField", "maskLayer", "areaCRS", Status)
        Intersection intersection1 = new Intersection(true, false, false, "fifao:FAO_DIV", "fifao:NJA", "F_SUBAREA",
                "ISO3_TERRI", "fifao:UN_CONTINENT", "EPSG:54012", Status.COMPUTED);
        Intersection intersection4 = new Intersection(true, false, false, "fifao:NJA", "fifao:ICCAT_SMU",
                "ISO3_TERRI",
                "ICCAT_SMU", "fifao:UN_CONTINENT", "EPSG:54012", Status.FAILED);
        dbConfig = initDBConfig(-1, null, null, null, null, null, null, null, null, null,
                Arrays.asList(intersection1, intersection4));

        List<Intersection> intersectionsToAdd = new ArrayList<Intersection>();
        SettingAction.compareXMLConfigAndDBConfig(xmlConfig, dbConfig, intersectionsToAdd);

        assertTrue("Found intersections to add", intersectionsToAdd.size() == dbConfig.intersections.size());
        assertTrue("Updated DB configuration", dbConfig.getUpdateVersion() == 0);

        assertTrue(intersectionsToAdd.get(0).getStatus().equals(Status.TOCOMPUTE));
        assertTrue(intersectionsToAdd.get(1).getStatus().equals(Status.TODELETE));
    }
}