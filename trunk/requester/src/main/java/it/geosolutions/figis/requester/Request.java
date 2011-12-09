/*
* Request - 
*
* Copyright (C) 2007,2011 GeoSolutions S.A.S.
* http://www.geo-solutions.it
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*/
package it.geosolutions.figis.requester;

import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import it.geosolutions.figis.model.Config;
import it.geosolutions.figis.model.DB;
import it.geosolutions.figis.model.Geoserver;
import it.geosolutions.figis.model.Global;
import it.geosolutions.figis.model.Intersection;


/**************************
 * @description this class allows to query the REST interface of the it.geosloutions.figis.model classes
 ************************/
/**
 *
 * @author Luca
 */
public class Request
{
    static XStream xStreamConfig = null;
    static XStream xStreamIntersection = null;

    /***********************
     * initialize the XSTREAM parser for the XML representation of it.geosolutions.figis.requester.model.Config object
     */
    public static void initConfig()
    {
        xStreamConfig = new XStream(new DomDriver());
        xStreamConfig.aliasType("configs", List.class);
        xStreamConfig.aliasType("config", Config.class);
        xStreamConfig.aliasType("global", Global.class);
        xStreamConfig.aliasType("geoserver", Geoserver.class);
        xStreamConfig.aliasType("db", DB.class);
        xStreamConfig.aliasType("intersections", List.class);
        xStreamConfig.alias("intersection", it.geosolutions.figis.model.Intersection.class);
    }

    /***********************
     * initialize the XSTREAM parser for the XML representation of it.geosolutions.figis.requester.model.Intersection object
     */
    public static void initIntersection()
    {
        xStreamIntersection = new XStream(new DomDriver());
        xStreamIntersection.aliasType("intersections", List.class);
        xStreamIntersection.alias("intersection", it.geosolutions.figis.model.Intersection.class);
    }

    /***************
     * Return all the Config instances
     * @param host the host name where address request
     * @return a list containing the instances
     * @throws java.net.MalformedURLException  in case the URL is not valid
     */
    @SuppressWarnings("unchecked")
    public static List<Config> getConfigs(String host, String ieServiceUsername, String ieServicePassword)
        throws java.net.MalformedURLException
    {
        String result = HTTPUtils.get(host + "/ie-services/config/", ieServiceUsername, ieServicePassword);
        
        if (result == null)
        {
            return null;
        }

        List<Config> configs = (List<Config>) xStreamConfig.fromXML(result);

        return configs;
    }

    /************************
     * Check if a Config object exists in the DB
     * @param host the host name where address request
     * @return a reference to the found Config object
     * @throws java.net.MalformedURLException in case the URL is not valid
     */
    public static Config existConfig(String host, String ieServiceUsername, String ieServicePassword)
        throws java.net.MalformedURLException
    {
        List<Config> configs = getConfigs(host, ieServiceUsername, ieServicePassword);
        if ((configs == null) || (configs.size() == 0))
        {
            return null;
        }

        return configs.get(0);
    }

    /********************************
     * Update the data of the Config instance
     * @param host the host name where address request
     * @param id the id of the instance to update
     * @param config the instance containing changes
     * @return the id of the changed instance
     */
    public static long updateConfig(String host, long id, Config config, String ieServiceUsername,
        String ieServicePassword)
    {
        String xml = xStreamConfig.toXML(config);
     
        String result = HTTPUtils.put(host + "/ie-services/config/" + id, xml, "text/xml", ieServiceUsername, ieServicePassword);
       
        Long value = Long.parseLong(result);

        return value;
    }

    /***************
     * Returns the Config instance identified by id
     * @param host the host name where address request
     * @param id the id of the Config instance to look up
     * @return the Config instance found
     * @throws java.net.MalformedURLException in case the URL is not valid
     */
    public static Config getConfigByID(String host, Long id, String ieServiceUsername, String ieServicePassword)
        throws java.net.MalformedURLException
    {

        String result = HTTPUtils.get(host + "/ie-services/config/" + id, ieServiceUsername, ieServicePassword);
      
        Config config = (Config) xStreamConfig.fromXML(result);

        return config;
    }

    /********************
     * Insert a new Config instance into the DB
     * @param host the host name where address request
     * @param config the instance to insert in the DB
     * @return the id of the instance into the DB
     * @throws java.net.MalformedURLException in case the URL is not valid
     */
    public static long insertConfig(String host, Config config, String ieServiceUsername, String ieServicePassword)
        throws java.net.MalformedURLException
    {
        String xml = xStreamConfig.toXML(config);
        
        String result = HTTPUtils.post(host + "/ie-services/config", xml, "text/xml", ieServiceUsername, ieServicePassword);
        
        if (result == null)
        {
            return -1;
        }

        Long value = Long.parseLong(result);

        return value;
    }

    /*******************
     * Delete the Config instance identified by id
     * @param host the host name where address request
     * @param id the id of the instance to delete from the DB
     * @return true if the request has success
     * @throws java.net.MalformedURLException in case the URL is not valid
     */
    public static boolean deleteConfig(String host, long id, String ieServiceUsername, String ieServicePassword)
        throws java.net.MalformedURLException
    {
        boolean result = HTTPUtils.delete(host + "/ie-services/config/" + id, ieServiceUsername, ieServicePassword);

        return result;
    }

    /**********************
     * Insert a new Intersection instance into the DB
     * @param host the host name where address request
     * @param intersection the Intersection instance to insert into the DB
     * @return the id of the instance into the DB
     * @throws java.net.MalformedURLException in case the URL is not valid
     */
    public static long insertIntersection(String host, Intersection intersection, String ieServiceUsername,
        String ieServicePassword) throws java.net.MalformedURLException
    {
        String xml = xStreamIntersection.toXML(intersection);
       
        String result = HTTPUtils.post(host + "/ie-services/intersection", xml, "text/xml", ieServiceUsername, ieServicePassword);
        
        if (result != null)
        {
            Long value = Long.parseLong(result);

            return value;
        }

        return -1;
    }

    /***********************
     * Returns all the Intersection instance from the DB
     * @param host the host name where address request
     * @return a list of all Intersection instances
     * @throws java.net.MalformedURLException in case the URL is not valid
     */
    @SuppressWarnings("unchecked")
    public static List<Intersection> getAllIntersections(String host, String ieServiceUsername,
        String ieServicePassword) throws java.net.MalformedURLException
    {
        String result = HTTPUtils.get(host + "/ie-services/intersection", ieServiceUsername, ieServicePassword);
       
        if (result != null)
        {
            if (xStreamIntersection == null)
            {
                initIntersection();
            }

            return (List<Intersection>) xStreamIntersection.fromXML(result);
        }

        return null;
    }

    /**************************
     * Delete all the Intersection instance from the DB
     * @param host the host name where address request
     * @return true if all the Intersection instances where deleted from the DB
     * @throws java.net.MalformedURLException
     */
    public static boolean deleteAllIntersections(String host, String ieServiceUsername, String ieServicePassword)
        throws java.net.MalformedURLException
    {
        boolean result = HTTPUtils.delete(host + "/ie-services/intersection/", ieServiceUsername, ieServicePassword);

        return result;
    }

    /***************
     * Delete the Intersection instance identified by id
     * @param host the host name where address request
     * @param id the id of the Intersection instance to delete
     * @return true in case of success
     */
    public static boolean deleteIntersectionById(String host, long id, String ieServiceUsername,
        String ieServicePassword)
    {
        boolean result = HTTPUtils.delete(host + "/ie-services/intersection/" + id, ieServiceUsername, ieServicePassword);

        return result;
    }

    /*******************
     * Update the status of the Intersection instance identified by id
     * @param id the identifier
     * @param status the new status
     * @return the identifier of the returned instance
     */
    public static long updateIntersectionById(String host, long id, Intersection intersection, String ieServiceUsername,
        String ieServicePassword)
    {
        String xml = xStreamIntersection.toXML(intersection);
      
        String result = HTTPUtils.put(host + "/ie-services/intersection/" + id, xml, "text/xml", ieServiceUsername, ieServicePassword);
        
        if (result != null)
        {
            Long value = Long.parseLong(result);

            return value;
        }

        return -1;
    }
}
