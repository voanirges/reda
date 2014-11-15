
package bigwave.alfresco;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import org.alfresco.webservice.accesscontrol.AccessControlServiceSoapBindingStub;
import org.alfresco.webservice.administration.AdministrationServiceSoapBindingStub;
import org.alfresco.webservice.administration.UserDetails;
import org.alfresco.webservice.authentication.AuthenticationFault;
import org.alfresco.webservice.util.AuthenticationUtils;
import org.alfresco.webservice.util.WebServiceFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Bigwave
{

    private static ArrayList<User>             users;

    static final Logger                        logger                = Logger.getLogger(Bigwave.class);

    static ArrayList<String>                   groups                = new ArrayList<String>();

    AdministrationServiceSoapBindingStub       administrationService = WebServiceFactory.getAdministrationService();

    static AccessControlServiceSoapBindingStub authorityService      = WebServiceFactory.getAccessControlService();

    static int                                 totalUsersdeleted          = 0;

    static int                                 totalUsersupdated          = 0;

    static int                                 totalUserscreated          = 0;

    static int                                 totalUsersfailed           = 0;

    static int                                 totalGroupsfailed          = 0;

    static int                                 totalGroupscreated         = 0;
    
    static int                                 totalGroupsdeleted         = 0;
    
    static int                                 totalGroupsupdated         = 0;

    public static void main( String[] args )
    {

        PropertyConfigurator.configure("log4j.properties");
        final long startTime = System.currentTimeMillis();
        try
        {
            AlfrescoUtils.setLogger();
            WebServiceFactory.setEndpointAddress(GetProperties.getProperty(GetProperties.ALFRESCO_URL));
            AuthenticationUtils.startSession(GetProperties.getProperty(GetProperties.ALFRESCO_ADMIN_USER), GetProperties.getProperty(GetProperties.ALFRESCO_ADMIN_PASS));
            users = SqlConnector.getUsers();

            logger.info("Total number of users in database : " + users.size());
            groups = SqlConnector.getGroups();
            logger.info("Total number of groups in database : " + groups.size());

            for (String alfGroup : groups)
            {
            	boolean successDeleteGroup = AlfrescoUtils.deleteGroup(alfGroup, false, null);
            	if (successDeleteGroup) totalGroupsdeleted++;
            }
            
            for (String dGroup : groups)
            {
                boolean successCreateGroup = AlfrescoUtils.createGroup(dGroup, false, null);
                if (successCreateGroup)
                {
                	totalGroupscreated++;
                }
                else
                {
                	totalGroupsfailed++;
                }

            }

            boolean existUser = false;
            for (User dbUser : users)
            {

                logger.info(getTimestamp() + " Processing user <" + dbUser.username + "> email <" + dbUser.getEmail() + "> active <" + dbUser.isActive() + ">");
                if (dbUser.isActive())
                {

                    try
                    {
                        UserDetails userDetails = AlfrescoUtils.getUser(dbUser.getUsername());

                        if (userDetails != null)
                        {
                            userDetails.getProperties();
                            existUser = true;
                            logger.info(getTimestamp() + " user exists in Alfresco  <" + userDetails.getUserName() + ">");
                        }
                    }
                    catch (RemoteException e)
                    {
                        existUser = false;
                    }
                    if (!existUser)
                    {
                        try
                        {
                            String firstname = dbUser.getFirstname();
                            String lastname = dbUser.getLastname();
                            AlfrescoUtils.createUser(firstname, lastname, dbUser.email, dbUser.username, dbUser.password);
                            logger.info(Bigwave.getTimestamp() + " Successfully created user <" + firstname + "> <" + lastname + "> <" + dbUser.email + "> <" + dbUser.username +">");
                            totalUserscreated++;
                        }
                        catch (Exception exc)
                        {
                        	exc.printStackTrace();
                          	//File file = new File("d:\\yasser\\yasser.txt");
                        	//FileOutputStream fos = new FileOutputStream(file, true);
                        	//PrintStream ps = new PrintStream(fos);                	
                        	//exc.printStackTrace(ps);
                        	//ps.close();
                            logger.error(getTimestamp() + " Cannot create user <" + dbUser.username + ">");
                            totalUsersfailed++;
                        }
                    }
                    else
                    {
                        try
                        {
                            String firstname = dbUser.getFirstname();
                            String lastname = dbUser.getLastname();
                            AlfrescoUtils.updateUser(firstname, lastname, dbUser.email, dbUser.username, dbUser.password);
                            totalUsersupdated++;
                        }
                        catch (Exception exc)
                        {
                            logger.error(getTimestamp() + " Cannot update user <" + dbUser.username +">");
                            totalUsersfailed++;
                        }
                    }

                    ArrayList<String> userGroups = SqlConnector.getGroupsOfUser(dbUser.username);
                    for (String usrGroup : userGroups)
                    {
                        ArrayList<String> groupUsers = AlfrescoUtils.getAllUsers(usrGroup);

                        try
                        {
                            AlfrescoUtils.addUsersToGroup(dbUser.username, usrGroup);
                            logger.info(getTimestamp() + " User <" + dbUser.username + "> successfully added to group <" + usrGroup + ">");
                            totalGroupsupdated++;
                        }
                        catch (Exception exc)
                        {
                        	exc.printStackTrace();
                            logger.error(getTimestamp() + " Cannot add user <" + dbUser.username + "> to group <" + usrGroup + ">");
                            totalGroupsfailed++;
                        }

                    }

                }
                else
                {
                    try
                    {
                        AlfrescoUtils.deleteUsers(new String[]
                        {
                            dbUser.username
                        });
                        totalUsersdeleted++;
                    }
                    catch (Exception exc)
                    {
                    	exc.printStackTrace();
                        logger.error(getTimestamp() + " Can not delete user : <" + dbUser.username + ">");
                        totalUsersfailed++;
                    }
                }

            }

            logger.info("Total users deleted : " + totalUsersdeleted);
            logger.info("Total users updated : " + totalUsersupdated);
            logger.info("Total users created : " + totalUserscreated);
            logger.info("Total users failed  : " + totalUsersfailed);
            logger.info("Total groups deleted: " + totalGroupsdeleted);
            logger.info("Total groups created: " + totalGroupscreated);
            logger.info("Total groups updated: " + totalGroupsupdated);
            logger.info("Total groups failed : " + totalGroupsfailed);

        }
        catch (AuthenticationFault e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        final long endTime = System.currentTimeMillis();
        logger.info("Total execution time: " + (endTime - startTime));

    }


    public static Timestamp getTimestamp()
    {

        java.util.Date date = new java.util.Date();
        return new Timestamp(date.getTime());
    }
}
