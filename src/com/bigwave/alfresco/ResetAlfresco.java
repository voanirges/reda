
package bigwave.alfresco;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import org.alfresco.webservice.accesscontrol.AccessControlServiceSoapBindingStub;
import org.alfresco.webservice.administration.AdministrationFault;
import org.alfresco.webservice.administration.AdministrationServiceSoapBindingStub;
import org.alfresco.webservice.util.AuthenticationUtils;
import org.alfresco.webservice.util.WebServiceFactory;

public class ResetAlfresco
{

    /**
     * @param args
     * @throws IOException
     */
    public static void main( String[] args ) throws IOException
    {

        WebServiceFactory.setEndpointAddress(GetProperties.getProperty(GetProperties.ALFRESCO_URL));
        AuthenticationUtils.startSession(GetProperties.getProperty(GetProperties.ALFRESCO_USER), GetProperties.getProperty(GetProperties.ALFRESCO_PASS));
        AdministrationServiceSoapBindingStub administrationService = WebServiceFactory.getAdministrationService();
        AccessControlServiceSoapBindingStub authorityService = WebServiceFactory.getAccessControlService();
        ArrayList<String> allAlfrescoGroups = AlfrescoUtils.getAllGroups();
        for (String alfGroup : allAlfrescoGroups)
        {
            if (!alfGroup.contains("ALFRESCO_ADMINISTRATORS") && !alfGroup.contains("EMAIL_CONTRIBUTERS") && !alfGroup.contains("SITE_ADMINISTRATORS")
                        && !alfGroup.contains("ALFRESCO_SEARCH_ADMINISTRATORS"))
            {
                ArrayList<String> groupUsers = AlfrescoUtils.getAllUsers(alfGroup);
                for (String groupUser : groupUsers)
                {
                    if (!groupUser.equals(GetProperties.getProperty(GetProperties.ALFRESCO_USER)))
                    {
                        try
                        {
                            AlfrescoUtils.deleteUsers(new String[]
                            {
                                groupUser
                            });
                        }
                        catch (AdministrationFault e)
                        {
                            e.printStackTrace();
                        }
                        catch (RemoteException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

    }

}
