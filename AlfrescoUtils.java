package com.bigwave.alfresco;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.alfresco.webservice.accesscontrol.AccessControlFault;
import org.alfresco.webservice.accesscontrol.AccessControlServiceSoapBindingStub;
import org.alfresco.webservice.accesscontrol.NewAuthority;
import org.alfresco.webservice.administration.AdministrationServiceSoapBindingStub;
import org.alfresco.webservice.administration.NewUserDetails;
import org.alfresco.webservice.repository.QueryResult;
import org.alfresco.webservice.types.NamedValue;
import org.alfresco.webservice.types.Query;
import org.alfresco.webservice.types.Store;
import org.alfresco.webservice.util.AuthenticationUtils;
import org.alfresco.webservice.util.Constants;
import org.alfresco.webservice.util.WebServiceFactory;

public class AlfrescoUtils
{

    public static final String GROUP_AUTHORITY_TYPE = "GROUP";

     

    public static void main( String a[] )
    {

        CreateUserCommand c = new CreateUserCommand();
        AuthenticationUtils.startSession("admin", "alfresco");
        Store s = new Store(Constants.WORKSPACE_STORE, "Spacesstore");
        AdministrationServiceSoapBindingStub ad = WebServiceFactory.getAdministrationService();
        String username = "sundarpx";
        String password = "sundarpx";
        String firstname = "sundar";
        String midlename = "p";
        String lastname = "rajan";
        String email = "sundar.p@demo.com";
        String regid = "1";
        boolean existUser = false;
        try
        {
            
            // createGroups();
            //
            // addUsersToGroup();
            
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
            NamedValue[] properties = c.createUserProperties(s.getScheme() + "://" + s.getAddress() + "/", firstname, midlename, lastname, email, regid);
            NewUserDetails dc[] = new NewUserDetails[]
            {
                new NewUserDetails(username, password, properties)
            };
            
            // ad.createUsers(dc);
        }
        AuthenticationUtils.endSession();

    }

    private static void createGroups() throws AccessControlFault , RemoteException
    {

        AccessControlServiceSoapBindingStub accessControlService = WebServiceFactory.getAccessControlService();

        NewAuthority cpGrpAuth = new NewAuthority(GROUP_AUTHORITY_TYPE, "REDA_MANAGERS");
        NewAuthority[] newAuthorities =
        {
            cpGrpAuth
        };
        String result[] = accessControlService.createAuthorities(null, newAuthorities);
    }

    private static void addUsersToGroup() throws AccessControlFault , RemoteException
    {

        System.out.println("entering...");
        String[] cpUsers =
        {
            "sundarpx"
        };
        String parentAuthority = GROUP_AUTHORITY_TYPE + "_" + "REDA_MANAGERS";
        AccessControlServiceSoapBindingStub accessControlService = WebServiceFactory.getAccessControlService();
        String[] result = accessControlService.addChildAuthorities(parentAuthority, cpUsers);

        // accessControlService.removeChildAuthorities(parentAuthority,
        // cpUsers);

        // System.out.println("results = " + result);
    }

    public NamedValue[] createUserProperties( String homefolder , String firstname , String midlename , String lastname , String email , String orgid )
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
}
