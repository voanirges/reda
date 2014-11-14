
package bigwave.alfresco;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import org.alfresco.webservice.accesscontrol.AccessControlServiceSoapBindingStub;
import org.alfresco.webservice.administration.AdministrationFault;
import org.alfresco.webservice.administration.AdministrationServiceSoapBindingStub;
import org.alfresco.webservice.util.AuthenticationUtils;
import org.alfresco.webservice.util.WebServiceFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class ResetAlfresco
{

    /**
     * @param args
     * @throws IOException
     */
    private static ArrayList<User>             Rusers;

    static final Logger                        logger                = Logger.getLogger(Bigwave.class);

    static ArrayList<String>                   Rgroups            = new ArrayList<String>();
    
    static ArrayList<String>                   Agroups            = new ArrayList<String>();

    AdministrationServiceSoapBindingStub       administrationService = WebServiceFactory.getAdministrationService();

    static AccessControlServiceSoapBindingStub authorityService      = WebServiceFactory.getAccessControlService();

    static int                                 totalUsersdeleted          = 0;

    static int                                 totalUsersfailed           = 0;

    static int                                 totalGroupsfailed          = 0;
    
    static int                                 totalGroupsdeleted         = 0;
    

    public static void main( String[] args ) throws IOException
    {
    	  	
    	boolean successDeleteGroup = false;
        PropertyConfigurator.configure("log4j.properties");
        final long startTime = System.currentTimeMillis();
        try
        {
            AlfrescoUtils.setLogger();
            WebServiceFactory.setEndpointAddress(GetProperties.getProperty(GetProperties.ALFRESCO_URL));
            AuthenticationUtils.startSession(GetProperties.getProperty(GetProperties.ALFRESCO_ADMIN_USER), GetProperties.getProperty(GetProperties.ALFRESCO_ADMIN_PASS));
            Rusers = SqlConnector.getUsers();
            Agroups = AlfrescoUtils.getAllGroups();
            logger.info("Total number of users in R database : " + Rusers.size());
            Rgroups = SqlConnector.getGroups();
            logger.info("Total number of groups in R database : " + Rgroups.size());
            logger.info("Total number of groups in A database : " + Agroups.size());

            for (String alfGroup : Rgroups)
            {
            	if (Agroups.contains(alfGroup)) {
            		successDeleteGroup = AlfrescoUtils.deleteGroup(alfGroup, false, null);
            		if (successDeleteGroup) totalGroupsdeleted++;
            	}
            }
            for (User dbUser : Rusers)
            {
            	try {
            		AlfrescoUtils.deleteUsers(new String[]{dbUser.getUsername()});
            		totalUsersdeleted++;
            	} catch (Exception e){
            		logger.info("User <" + dbUser.getUsername() + "> Failed to Delete");
            		e.printStackTrace();
            		totalUsersfailed++;
            	}
            } 
        } catch (Exception e){
        		e.printStackTrace();
        }
        
        logger.info("Total users deleted : " + totalUsersdeleted);
        logger.info("Total users failed  : " + totalUsersfailed);
        logger.info("Total groups deleted: " + totalGroupsdeleted);
        logger.info("Total groups failed : " + totalGroupsfailed);

    }

}
