
package bigwave.alfresco;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
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

    static int                                 totaldeleted          = 0;

    static int                                 totalupdated          = 0;

    static int                                 totalcreated          = 0;

    static int                                 totalfailed           = 0;

    public static void main( String[] args )
    {

        PropertyConfigurator.configure("log4j.properties");
        final long startTime = System.currentTimeMillis();
        try
        {
            AlfrescoUtils.setLogger();
            WebServiceFactory.setEndpointAddress(GetProperties.getProperty(GetProperties.ALFRESCO_URL));
            AuthenticationUtils.startSession(GetProperties.getProperty(GetProperties.ALFRESCO_USER), GetProperties.getProperty(GetProperties.ALFRESCO_PASS));
            users = SqlConnector.getUsers();

            logger.info("Total number of users in database : " + users.size());
            groups = SqlConnector.getGroups();
            logger.info("Total number of groups in database : " + groups.size());

            for (String alfGroup : groups)
            {
                AlfrescoUtils.deleteGroup(alfGroup, false, null);
            }
            for (String dGroup : groups)
            {
                // dGroup = normalizeGroupName(dGroup);
                boolean successCreateGroup = AlfrescoUtils.createGroup(dGroup, false, null);

            }

            boolean existUser = false;
            for (User dbUser : users)
            {

                logger.info("Processing user  : " + dbUser.username + " active :" + dbUser.isActive());
                if (dbUser.isActive())
                {

                    try
                    {
                        UserDetails userDetails = AlfrescoUtils.getUser(dbUser.getUsername());

                        if (userDetails != null)
                        {
                            userDetails.getProperties();
                            existUser = true;
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
                            String firstname = dbUser.username;
                            String lastname = dbUser.username;
                            if (dbUser.username.indexOf(" ") != -1)
                            {
                                firstname = dbUser.username.substring(0, dbUser.username.indexOf(" "));
                                lastname = dbUser.username.substring(dbUser.username.indexOf(" ") + 1);
                            }

                            AlfrescoUtils.createUser(firstname, lastname, dbUser.group, dbUser.username);
                            logger.info("Successfully created user : " + dbUser.username);
                            totalcreated++;
                        }
                        catch (Exception exc)
                        {
                            logger.error("Can not create user : " + dbUser.username + " , already exist !");
                            totalfailed++;
                        }
                    }
                    else
                    {
                        try
                        {
                            String firstname = dbUser.username;
                            String lastname = dbUser.username;
                            if (dbUser.username.indexOf(" ") != -1)
                            {
                                firstname = dbUser.username.substring(0, dbUser.username.indexOf(" "));
                                lastname = dbUser.username.substring(dbUser.username.indexOf(" ") + 1);
                            }

                            AlfrescoUtils.updateUser(firstname, lastname, dbUser.group, dbUser.username);
                            logger.info("Successfully updated user : " + dbUser.username);
                            totalupdated++;
                        }
                        catch (Exception exc)
                        {
                            logger.error("Can not update user : " + dbUser.username);
                            totalfailed++;
                        }
                    }

                    ArrayList<String> userGroups = SqlConnector.getGroupsOfUser(dbUser.username);
                    for (String usrGroup : userGroups)
                    {
                        ArrayList<String> groupUsers = AlfrescoUtils.getAllUsers(usrGroup);

                        try
                        {
                            AlfrescoUtils.addUsersToGroup(dbUser.username, usrGroup);
                            logger.info("User : " + dbUser.username + " successfully added to group " + usrGroup);
                        }
                        catch (Exception exc)
                        {
                            logger.error("Can not create group : " + usrGroup + " , already exist !");
                            totalfailed++;
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
                        totaldeleted++;
                    }
                    catch (Exception exc)
                    {
                        logger.error("Can not cdelete user : ");
                        totalfailed++;
                    }
                }

            }
            logger.info("Total deleted : " + totaldeleted + " total updated : " + totalupdated + " totalcreated " + totalcreated);
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

    private static String normalizeGroupName( String dGroup )
    {

        dGroup = dGroup.replace(" ", "_");
        dGroup = dGroup.replaceAll("\\W", "_");
        return dGroup;
    }
}
