
package bigwave.alfresco;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Dalibor
 * 
 */

public class GetProperties
{

    private final static Logger LOGGER           = Logger.getLogger(GetProperties.class.getName());

    private final static String propFileName     = "config.properties";

    public static final String  JDBC_USERNAME    = "user";

    public static final String  JDBC_PASSWORD    = "pass";

    public static final String  ALFRESCO_URL     = "alf_url";

    public static final String  ALFRESCO_ADMIN_USER    = "alf_admin_user";

    public static final String  ALFRESCO_ADMIN_PASS    = "alf_admin_pass";
    
    public static final String  ALFRESCO_USER_PASS = "alf_user_pass";

    public static final String  JDBC_URL         = "url";

    public static final String  JDBC_USER_QUERY  = "db_user_query";

    public static final String  JDBC_GROUP_QUERY = "db_group_query";

    public static HashMap<String, String> getPropValues() throws IOException
    {

        HashMap<String, String> props = new HashMap<String, String>();
        String result = "";
        Properties prop = new Properties();

        InputStream inputStream = GetProperties.class.getResourceAsStream(propFileName);
        prop.load(inputStream);
        Date time = new Date(System.currentTimeMillis());
        String user = prop.getProperty(JDBC_USERNAME);
        String pass = prop.getProperty(JDBC_PASSWORD);
        String url = prop.getProperty(JDBC_URL);
        String alfrescoEndpoint = prop.getProperty(ALFRESCO_URL);
        String alfAdminUser = prop.getProperty(ALFRESCO_ADMIN_USER);
        String alfAdminPass = prop.getProperty(ALFRESCO_ADMIN_PASS);
        String alfUserPass = prop.getProperty(ALFRESCO_USER_PASS);
        String grQuery = prop.getProperty(JDBC_USER_QUERY);
        String usrQuery = prop.getProperty(JDBC_GROUP_QUERY);

        props.put(JDBC_USERNAME, user);
        props.put(JDBC_PASSWORD, pass);
        props.put(JDBC_URL, url);
        props.put(ALFRESCO_URL, alfrescoEndpoint);
        props.put(ALFRESCO_ADMIN_USER, alfAdminUser);
        props.put(ALFRESCO_ADMIN_PASS, alfAdminPass);
        props.put(ALFRESCO_USER_PASS, alfUserPass);
        props.put(JDBC_USER_QUERY, grQuery);
        props.put(JDBC_GROUP_QUERY, usrQuery);

        result = "Properties List = " + pass + ", " + url + ", ";

        LOGGER.log(Level.ALL, "info", result + "\nProgram Ran on " + time + " by user=" + user);
        return props;
    }

    public static String getProperty( String propName ) throws IOException
    {

        String result = "";
        Properties prop = new Properties();

        InputStream inputStream = GetProperties.class.getResourceAsStream(propFileName);
        prop.load(inputStream);
        Date time = new Date(System.currentTimeMillis());
        String propValue = prop.getProperty(propName);
        result = "Properties   " + propName + " = " + propValue + ", ";

        LOGGER.log(Level.ALL, "info", result + "\nProgram Ran on " + time);
        return propValue;
    }
}
