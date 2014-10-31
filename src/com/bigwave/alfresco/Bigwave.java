
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

    static int                                 totaldeleted          = 0;

    static int                                 totalupdated          = 0;

    static int                                 totalcreated          = 0;

    static int                                 totalfailed           = 0;

    static int                                 groupsfailed          = 0;

    static int                                 groupscreated         = 0;

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
                boolean successCreateGroup = AlfrescoUtils.createGroup(dGroup, false, null);
                if (successCreateGroup)
                {
                    groupscreated++;
                }
                else
                {
                    groupsfailed++;
                }

            }

            boolean existUser = false;
            for (User dbUser : users)
            {

                logger.info(getTimestamp() + " Processing user  : " + dbUser.username + " active :" + dbUser.isActive());
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
                            String firstname = dbUser.getFirstname();
                            String lastname = dbUser.getLastname();
                            AlfrescoUtils.createUser(firstname, lastname, dbUser.group, dbUser.username);
                            totalcreated++;
                        }
                        catch (Exception exc)
                        {
                        	exc.printStackTrace();
                          	File file = new File("d:\\yasser\\yasser.txt");
                        	FileOutputStream fos = new FileOutputStream(file, true);
                        	PrintStream ps = new PrintStream(fos);                	
                        	exc.printStackTrace(ps);
                        	ps.close();
                            logger.error(getTimestamp() + " Can not create user : " + dbUser.username);
                            totalfailed++;
                        }
                    }
                    else
                    {
                        try
                        {
                            String firstname = dbUser.getFirstname();
                            String lastname = dbUser.getLastname();
                            AlfrescoUtils.updateUser(firstname, lastname, dbUser.group, dbUser.username);
                            totalupdated++;
                        }
                        catch (Exception exc)
                        {
                            logger.error(getTimestamp() + " Can not update user : " + dbUser.username);
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
                            logger.info(getTimestamp() + " User : " + dbUser.username + " successfully added to group " + usrGroup);
                        }
                        catch (Exception exc)
                        {
                        	exc.printStackTrace();
                            logger.error(getTimestamp() + " Cannot add user <" + dbUser.username + "> to group : " + usrGroup + " , maybe group does not exist");
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
                        logger.error(getTimestamp() + " Can not delete user : <" + dbUser.username + ">");
                        totalfailed++;
                    }
                }

            }
            logger.info("Total users deleted : " + totaldeleted);
            logger.info("Total users  updated : " + totalupdated);
            logger.info("Total user created " + totalcreated);
            logger.info("Total groups created " + groupscreated);
            logger.info("Total groups updated " + groupsfailed);

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

    public static Timestamp getTimestamp()
    {

        java.util.Date date = new java.util.Date();
        return new Timestamp(date.getTime());
    }
}
