
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

public class AlfrescoUtils
{

    public static final String                         GROUP_AUTHORITY_TYPE       = "GROUP";

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
            logger.error("Can not get users of group : ", e);;
        }
        catch (RemoteException e)
        {
            logger.error("Can not get users of group : ", e);;
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
            for (int i = 0; i < users.length; i++)
            {
                if (username.equals(users[i]))
                {
                    return true;
                }
            }
        }
        catch (AccessControlFault e)
        {

            logger.error("Can not get user group : ", e);
        }
        catch (RemoteException e)
        {
            logger.error("Can not get user group : ", e);
        }

        return false;

    }

    public static UserDetails[] createUser( String firstName , String lastName , String email , String username ) throws AdministrationFault , RemoteException
    {

        NewUserDetails[] properties = new NewUserDetails[4];
        NamedValue[] pfirstName = new NamedValue[]
        {
                    new NamedValue(Constants.PROP_USER_FIRSTNAME, false, firstName, null), new NamedValue(Constants.PROP_USER_FIRSTNAME, false, lastName, null)
        };
        properties[0] = new NewUserDetails(firstName, firstName, pfirstName);
        // NamedValue[] pLastnametName=new NamedValue[]{new
        // NamedValue(Constants.PROP_USER_FIRSTNAME,false,lastName,null)};
        // properties[1] = new NewUserDetails(firstName, lastName,
        // pLastnametName);
        // NamedValue[] pLastnametName=new NamedValue[]{new
        // NamedValue(Constants.PROP_USER_FIRSTNAME,false,lastName,null)};
        // properties[2] = new NewUserDetails(firstName, email, null);
        // properties[3] = new NewUserDetails(firstName, username, null);

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

    public static ArrayList<String> getAllUsers( String group )
    {

        try
        {
            WebServiceFactory.setEndpointAddress(GetProperties.getProperty(GetProperties.ALFRESCO_URL));
        }
        catch (IOException e)
        {

        }
        ArrayList<String> alfGroups = new ArrayList<String>();
        try
        {
            SiblingAuthorityFilter saf = new SiblingAuthorityFilter();
            saf.setAuthorityType("USER");
            String[] users = accessControlService.getChildAuthorities("GROUP_" + group, saf);
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

        // accessControlService.removeChildAuthorities(parentAuthority,
        // cpUsers);

        // System.out.println("results = " + result);
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
            }
            catch (Exception exc)
            {
                logger.error("Can not create group : ", exc);
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
            }
            catch (Exception exc)
            {
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
}
