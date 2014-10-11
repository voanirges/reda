
package bigwave.alfresco;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;

import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.webservice.accesscontrol.AccessControlServiceSoapBindingStub;
import org.alfresco.webservice.administration.AdministrationServiceSoapBindingStub;
import org.alfresco.webservice.administration.UserDetails;
import org.alfresco.webservice.authentication.AuthenticationFault;
import org.alfresco.webservice.repository.QueryResult;
import org.alfresco.webservice.types.Query;
import org.alfresco.webservice.types.Store;
import org.alfresco.webservice.util.AuthenticationUtils;
import org.alfresco.webservice.util.Constants;
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

        try
        {
            AlfrescoUtils.setLogger();
            WebServiceFactory.setEndpointAddress(GetProperties.getProperty(GetProperties.ALFRESCO_URL));
            AuthenticationUtils.startSession(GetProperties.getProperty(GetProperties.ALFRESCO_USER), GetProperties.getProperty(GetProperties.ALFRESCO_PASS));
            users = SqlConnector.getUsers();
            logger.info("Total number of users in database : " + users.size());
            groups = SqlConnector.getGroups();
            logger.info("Total number of groups in database : " + groups.size());
            ArrayList<String> allAlfrescoGroups = AlfrescoUtils.getAllGroups();
            for (String alfGroup : allAlfrescoGroups)
            {
                if (groups.contains(alfGroup))
                {
                    AlfrescoUtils.deleteGroup(alfGroup, false, null);
                }
            }

            boolean existUser = false;
            for (User dbUser : users)
            {

                try
                {
                    UserDetails userDetails = AlfrescoUtils.getUser(dbUser.getUsername());

                    if (userDetails != null)
                    {
                        userDetails.getProperties();
                    }
                }
                catch (RemoteException e)
                {

                    try
                    {
                        AlfrescoUtils.createUser(dbUser.username, "", "", dbUser.username);
                        logger.info("Successfully created user : " + dbUser.username);
                        totalcreated++;
                    }
                    catch (Exception exc)
                    {
                        logger.error("Can not create user : ", exc);
                        totalfailed++;
                    }

                }

                ArrayList<String> userGroups = SqlConnector.getGroupsOfUser(dbUser.username);
                for (String usrGroup : userGroups)
                {
                    ArrayList<String> groupUsers = AlfrescoUtils.getAllUsers(usrGroup);
                    for (String groupUser : groupUsers)
                    {

                        boolean contains = false;
                        for (User idbUser : users)
                        {
                            if (idbUser.getUsername().equals(groupUser))
                            {
                                contains = true;
                            }

                        }
                        if (!contains)
                        {
                            try
                            {
                                AlfrescoUtils.deleteUsers(new String[]
                                {
                                    groupUser
                                });
                                totaldeleted++;
                            }
                            catch (Exception exc)
                            {
                                logger.error("Can not cdelete user : " + exc);
                                totalfailed++;
                            }
                        }
                    }

                    if (allAlfrescoGroups.contains(usrGroup))
                    {
                        if (!AlfrescoUtils.isUserInGroup(usrGroup, dbUser.username))
                        {
                            try
                            {
                                AlfrescoUtils.addUsersToGroup(dbUser.username, usrGroup);
                                logger.info("User : " + dbUser.username + " successfully added to group " + usrGroup);
                            }
                            catch (Exception exc)
                            {
                                logger.error("Can not create group : " + exc);
                                totalfailed++;
                            }
                        }
                    }
                    else
                    {
                        try
                        {
                            boolean successCreateGroup = AlfrescoUtils.createGroup(usrGroup, false, null);
                            if (successCreateGroup)
                            {
                                AlfrescoUtils.addUsersToGroup(dbUser.username, usrGroup);
                                logger.info("User : " + dbUser.username + " successfully added to group " + usrGroup);
                                totalupdated++;
                            }
                            else
                            {
                                logger.error("Can not create group : ");
                                totalfailed++;
                            }
                        }
                        catch (Exception excCreateGroup)
                        {
                            logger.error("Can not create group : ", excCreateGroup);
                        }

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

    }
}
