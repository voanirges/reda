
package bigwave.alfresco;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.webservice.accesscontrol.AccessControlFault;
import org.alfresco.webservice.accesscontrol.AccessControlServiceSoapBindingStub;
import org.alfresco.webservice.accesscontrol.AuthorityFilter;
import org.alfresco.webservice.accesscontrol.NewAuthority;
import org.alfresco.webservice.accesscontrol.SiblingAuthorityFilter;
import org.alfresco.webservice.administration.AdministrationFault;
import org.alfresco.webservice.administration.AdministrationServiceSoapBindingStub;
import org.alfresco.webservice.administration.NewUserDetails;
import org.alfresco.webservice.administration.UserDetails;
import org.alfresco.webservice.administration.UserFilter;
import org.alfresco.webservice.administration.UserQueryResults;
import org.alfresco.webservice.authoring.AuthoringServiceSoapBindingStub;
import org.alfresco.webservice.repository.QueryResult;
import org.alfresco.webservice.types.NamedValue;
import org.alfresco.webservice.types.Query;
import org.alfresco.webservice.types.Store;
import org.alfresco.webservice.util.AuthenticationUtils;
import org.alfresco.webservice.util.Constants;
import org.alfresco.webservice.util.WebServiceFactory;
import org.apache.log4j.PropertyConfigurator;
import org.omg.PortableInterceptor.USER_EXCEPTION;

public class AlfrescoUtils
{

    public static final String                         GROUP_AUTHORITY_TYPE       = "GROUP";
    
    public static final String                         USER_AUTHORITY_TYPE       = "USER";

    public static final String                         ADMIN_USERNAME             = "admin";

    public static final String                         ADMIN_PASSWORD             = "admin";

    public static final String                         CP01_USERNAME              = "cp01";

    protected static final String                      CONTENT_PROVIDER_GROUPNAME = "RBT_CONTENT_PROVIDER";

    private static AccessControlServiceSoapBindingStub accessControlService       = WebServiceFactory.getAccessControlService();

    static AdministrationServiceSoapBindingStub        administrationService      = WebServiceFactory.getAdministrationService();

    static AccessControlServiceSoapBindingStub         authorityService           = WebServiceFactory.getAccessControlService();

    static final org.apache.log4j.Logger               logger                     = org.apache.log4j.Logger.getLogger(AlfrescoUtils.class);

    public static void setLogger() throws IOException
    {

        WebServiceFactory.setEndpointAddress(GetProperties.getProperty(GetProperties.ALFRESCO_URL));
        PropertyConfigurator.configure("log4j.properties");
    }

    public static void main( String a[] )
    {

        try
        {

            AuthenticationUtils.startSession("admin", "admin");

            Store s = new Store(Constants.WORKSPACE_STORE, "Spacesstore");

            AdministrationServiceSoapBindingStub ad = WebServiceFactory.getAdministrationService();
            accessControlService = WebServiceFactory.getAccessControlService();
            String username = "sundarpx";
            String password = "sundarpx";
            String firstname = "sundar";
            String midlename = "p";
            String lastname = "rajan";
            String email = "sundar.p@demo.com";
            String regid = "1";
            String groupName = "REDA_MANAGERS";

            // check the user
            boolean existUser = false;

            try
            {

                createGroups();
                //
                addUsersToGroup(username, groupName);

                // test before adding the user
                Query query = new Query("lucene hnv mjb,jmvhcxc\\ ", "+TYPE:\"cm:person\" AND +@cm\\:userName:\"" + username + "\"");
                QueryResult results = WebServiceFactory.getRepositoryService().query(s, query, true);

                System.out.println(results);

                if (results != null && results.getResultSet() != null)
                {
                    if (results.getResultSet().getRows().length > 0)
                    {
                        existUser = true;

                    }
                    else
                    {
                        existUser = false;

                    }
                }
                else
                {
                    existUser = false;
                }
            }
            catch (Exception e)
            {

                e.printStackTrace();
                existUser = false;
            }

            if (existUser)
            {
                System.out.println("This user already exists");

            }
            else
            {
                NamedValue[] properties = createUserProperties(s.getScheme() + "://" + s.getAddress() + "/", firstname, midlename, lastname, email, regid);
                NewUserDetails dc[] = new NewUserDetails[]
                {
                    new NewUserDetails(username, password, properties)
                };

                ad.createUsers(dc);
            }

            // end session
            AuthenticationUtils.endSession();

        }
        catch (RemoteException ex)
        {

            Logger.getLogger(AlfrescoUtils.class.getName()).log(Level.SEVERE, null, ex);

        }

    }

    public static String[] getUsersOfGroup( String groupname )
    {

        String[] users = null;
        SiblingAuthorityFilter saf = new SiblingAuthorityFilter();

        saf.setAuthorityType(AuthorityType.USER.name());

        try
        {
            users = accessControlService.getChildAuthorities("GROUP_" + groupname, saf);
        }
        catch (AccessControlFault e)
        {
            logger.error(Bigwave.getTimestamp() +" Can not get users of group <" + groupname + ">");
        }
        catch (RemoteException e)
        {
            logger.error(Bigwave.getTimestamp() +" Can not get users of group <" + groupname + ">");
        }
        return users;
    }

    public static boolean isUserInGroup( String groupname , String username )
    {

        SiblingAuthorityFilter saf = new SiblingAuthorityFilter();

        saf.setAuthorityType(AuthorityType.USER.name());

        try
        {
            String[] users = accessControlService.getChildAuthorities("GROUP_" + groupname, saf);
            if (users != null)
            {
                for (int i = 0; i < users.length; i++)
                {
                    if (username.equals(users[i]))
                    {
                        return true;
                    }
                }
            }
        }
        catch (AccessControlFault e)
        {

            logger.info(Bigwave.getTimestamp() + " can not get user groups for: " + username + " ");
        }
        catch (RemoteException e)
        {
            logger.info(Bigwave.getTimestamp() + " can not get user groups for: " + username + " ");
        }

        return false;

    }

    public static UserDetails[] createUser( String firstName , String lastName , String email , String username, String password ) throws AdministrationFault , RemoteException
    {

        NewUserDetails[] properties = new NewUserDetails[5];
        NamedValue[] pfirstName = new NamedValue[]
        {
                    new NamedValue(Constants.PROP_USERNAME, false, username, null), 
                    new NamedValue(Constants.PROP_USER_EMAIL, false, email, null),
                    new NamedValue(Constants.PROP_NAME, false, firstName + " " + lastName, null),
                    new NamedValue(Constants.PROP_USER_FIRSTNAME, false, firstName, null), 
                    new NamedValue(Constants.PROP_USER_LASTNAME, false, lastName, null)
        };
        properties[0] = new NewUserDetails(username, password, pfirstName);
              
        UserDetails[] details = administrationService.createUsers(properties);

        return details;
    }

    public static UserQueryResults getUsers( UserFilter filter ) throws AdministrationFault , RemoteException
    {

        UserQueryResults details = administrationService.queryUsers(filter);
        return details;
    }

    public static UserDetails getUser( String username ) throws AdministrationFault , RemoteException
    {

        UserDetails details = administrationService.getUser(username);
        return details;
    }

    public static void deleteUsers( String[] users ) throws AdministrationFault , RemoteException
    {

        administrationService.deleteUsers(users);

    }

    public static ArrayList<String> getAllGroups()
    {

        ArrayList<String> alfGroups = new ArrayList<String>();
        try
        {
            AuthorityFilter authorityFilter = new AuthorityFilter();
            authorityFilter.setAuthorityType(GROUP_AUTHORITY_TYPE);
            String[] groups = accessControlService.getAuthorities();
            for (int i = 0; i < groups.length; i++)
            {
                alfGroups.add(groups[i]);
            }
        }
        catch (AccessControlFault e)
        {
            e.printStackTrace();
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
        return alfGroups;
    }
    
    public static ArrayList<String> getAllUsers()
    {

        ArrayList<String> alfUsers = new ArrayList<String>();
        try
        {
            AuthorityFilter authorityFilter = new AuthorityFilter();
            logger.info("Auth Type User: " + AuthorityType.USER.name());
            authorityFilter.setAuthorityType(AuthorityType.USER.name());
            String[] users = accessControlService.getAuthorities();
            for (int i = 0; i < users.length; i++)
            {
            	logger.info("A User: " + users[i]);
                alfUsers.add(users[i]);
            }
        }
        catch (AccessControlFault e)
        {
            e.printStackTrace();
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
        return alfUsers;
    }

    public static ArrayList<String> getAllUsers( String group )
    {

        try
        {
            WebServiceFactory.setEndpointAddress(GetProperties.getProperty(GetProperties.ALFRESCO_URL));
            AuthenticationUtils.startSession(GetProperties.getProperty(GetProperties.ALFRESCO_ADMIN_USER), GetProperties.getProperty(GetProperties.ALFRESCO_ADMIN_PASS));
        }
        catch (IOException e)
        {

        }
        ArrayList<String> alfGroups = new ArrayList<String>();
        try
        {
            SiblingAuthorityFilter saf = new SiblingAuthorityFilter();
            saf.setAuthorityType("USER");
            String[] users = accessControlService.getChildAuthorities(group, saf);
            if (users != null)
            {
                for (int i = 0; i < users.length; i++)
                {
                    alfGroups.add(users[i]);
                }
            }
        }
        catch (AccessControlFault e)
        {
            e.printStackTrace();
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
        return alfGroups;
    }

    public static void createGroups() throws AccessControlFault , RemoteException
    {

        AccessControlServiceSoapBindingStub accessControlService = WebServiceFactory.getAccessControlService();

        NewAuthority cpGrpAuth = new NewAuthority(GROUP_AUTHORITY_TYPE, "REDA_MANAGERS");
        NewAuthority[] newAuthorities =
        {
            cpGrpAuth
        };
        String result[] = accessControlService.createAuthorities(null, newAuthorities);
    }

    public static void addUsersToGroup( String username , String groupName ) throws AccessControlFault , RemoteException
    {

        try
        {
            WebServiceFactory.setEndpointAddress(GetProperties.getProperty(GetProperties.ALFRESCO_URL));
        }
        catch (IOException e)
        {

        }
        String[] cpUsers =
        {
            username
        };
        String parentAuthority = GROUP_AUTHORITY_TYPE + "_" + groupName;// "REDA_MANAGERS";
        AccessControlServiceSoapBindingStub accessControlService = WebServiceFactory.getAccessControlService();
        String[] result = accessControlService.addChildAuthorities(parentAuthority, cpUsers);
    }

    public static NamedValue[] createUserProperties( String homefolder , String firstname , String midlename , String lastname , String email , String orgid )
    {

        return new NamedValue[]
        {

                    new NamedValue(Constants.PROP_USER_HOMEFOLDER, false, homefolder, null),

                    new NamedValue(Constants.PROP_USER_FIRSTNAME, false, firstname, null),

                    new NamedValue(Constants.PROP_USER_MIDDLENAME, false, midlename, null),

                    new NamedValue(Constants.PROP_USER_LASTNAME, false, lastname, null),

                    new NamedValue(Constants.PROP_USER_EMAIL, false, email, null),

                    new NamedValue(Constants.PROP_USER_ORGID, false, orgid, null)

        };

    }

    public static boolean createGroup( String groupName , boolean moreGroups , String[] groupNames )
    {

        try
        {
            WebServiceFactory.setEndpointAddress(GetProperties.getProperty(GetProperties.ALFRESCO_URL));
        }
        catch (IOException e)
        {

        }
        String GROUP_AUTHORITY_TYPE = "GROUP";
        if (!moreGroups)
        {
            try
            {

                String CONTENT_PROVIDER_GROUPNAME = groupName;

                NewAuthority cpGrpAuth = new NewAuthority();
                cpGrpAuth.setAuthorityType(GROUP_AUTHORITY_TYPE);
                cpGrpAuth.setName(CONTENT_PROVIDER_GROUPNAME);

                NewAuthority[] newAuthorities = new NewAuthority[1];
                newAuthorities[0] = cpGrpAuth;

                String[] result = accessControlService.createAuthorities(null, newAuthorities);
                logger.info(Bigwave.getTimestamp() + " Successfully created group <" + groupName + ">");
            }
            catch (Exception exc)
            {
                logger.info(Bigwave.getTimestamp() + " can not create group <" + groupName + ">");
                exc.printStackTrace();
                return false;
            }
        }
        else
        {
            try
            {
                NewAuthority[] newAuthorities = new NewAuthority[groupNames.length];
                for (int i = 0; i < groupNames.length; i++)
                {

                    String CONTENT_PROVIDER_GROUPNAME = groupNames[i];
                    NewAuthority cpGrpAuth = new NewAuthority();
                    cpGrpAuth.setAuthorityType(GROUP_AUTHORITY_TYPE);
                    cpGrpAuth.setName(CONTENT_PROVIDER_GROUPNAME);
                    newAuthorities[i] = cpGrpAuth;
                }
                String[] result = accessControlService.createAuthorities(null, newAuthorities);
                logger.info(Bigwave.getTimestamp() + " Successfully created groups : " + groupNames.toString() + " ");
            }
            catch (Exception exc)
            {
                logger.info(Bigwave.getTimestamp() + " can not create groups : " + groupNames.toString() + " ");
                exc.printStackTrace();
                return false;
            }

        }
        return true;
    }

    private static boolean AddUsertoGroup( String user , String group , boolean moreUsers , String[] users )
    {

        String GROUP_AUTHORITY_TYPE = "GROUP";
        String CONTENT_PROVIDER_GROUPNAME = group;
        String parentAuthority = GROUP_AUTHORITY_TYPE + "_" + CONTENT_PROVIDER_GROUPNAME;
        if (!moreUsers)
        {
            try
            {
                String CP01_USERNAME = user;
                String[] cpUsers =
                {
                    CP01_USERNAME
                };
                String[] result2 = accessControlService.addChildAuthorities(parentAuthority, cpUsers);
            }
            catch (Exception exc)
            {
                return false;
            }
        }
        else
        {
            try
            {

                String[] result2 = accessControlService.addChildAuthorities(parentAuthority, users);

            }
            catch (Exception exc)
            {
                return false;
            }

        }
        return true;

    }

    public static boolean deleteGroup( String groupName , boolean changeUser , String[] groupNames )
    {

    	String GROUP_AUTHORITY_TYPE = "GROUP";
        if (!changeUser)
        {
            try
            {
                String[] temp = new String[1];
                temp[0] = GROUP_AUTHORITY_TYPE + "_" + groupName;
                accessControlService.deleteAuthorities(temp);
                logger.info(Bigwave.getTimestamp() + " Successfully deleted group <" + groupName + ">");
            }
            catch (Exception exc)
            {
            	exc.printStackTrace();
            	logger.info(Bigwave.getTimestamp() + " Failed to delete group <" + groupName + ">");
                return false;
            }
        }
        else
        {
            try
            {
                accessControlService.deleteAuthorities(groupNames);

            }
            catch (Exception exc)
            {
                return false;
            }

        }
        return true;

    }

    private static boolean RemoveUserfromGroup( String user , String group , boolean changeUser , String[] users )
    {

        String GROUP_AUTHORITY_TYPE = "GROUP";
        String CONTENT_PROVIDER_GROUPNAME = group;
        String parentAuthority = GROUP_AUTHORITY_TYPE + "_" + CONTENT_PROVIDER_GROUPNAME;
        if (!changeUser)
        {
            try
            {
                String CP01_USERNAME = user;
                String[] cpUsers =
                {
                    CP01_USERNAME
                };
                accessControlService.removeChildAuthorities(parentAuthority, cpUsers);
            }
            catch (Exception exc)
            {
                return false;
            }
        }
        else
        {
            try
            {

                accessControlService.removeChildAuthorities(parentAuthority, users);

            }
            catch (Exception exc)
            {
                return false;
            }

        }
        return true;

    }

    public static UserDetails[] updateUser( String firstName , String lastName , String email , String username, String password ) throws AdministrationFault , RemoteException
    {

        UserDetails[] properties = new UserDetails[4];
        NamedValue[] pfirstName = new NamedValue[]
        {
                    new NamedValue(Constants.PROP_USER_EMAIL, false, email, null),
                    new NamedValue(Constants.PROP_NAME, false, firstName + " " + lastName, null),
                    new NamedValue(Constants.PROP_USER_FIRSTNAME, false, firstName, null),
                    new NamedValue(Constants.PROP_USER_LASTNAME, false, lastName, null)
        };
        properties[0] = new UserDetails(username, pfirstName);
        properties[0].setUserName(username);

        UserDetails[] details = administrationService.updateUsers(properties);
        logger.info(Bigwave.getTimestamp() + " Successfully updated user <" + firstName + "> <" + lastName + "> <" + email + "> <" + username + ">");
        return details;
    }

}
