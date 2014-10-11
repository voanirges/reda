
package bigwave.alfresco;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.alfresco.webservice.accesscontrol.AccessControlFault;
import org.alfresco.webservice.accesscontrol.AccessControlServiceSoapBindingStub;
import org.alfresco.webservice.accesscontrol.NewAuthority;
import org.alfresco.webservice.administration.AdministrationServiceSoapBindingStub;
import org.alfresco.webservice.administration.NewUserDetails;
import org.alfresco.webservice.administration.UserDetails;
import org.alfresco.webservice.content.Content;
import org.alfresco.webservice.repository.QueryResult;
import org.alfresco.webservice.repository.RepositoryFault;
import org.alfresco.webservice.repository.RepositoryServiceLocator;
import org.alfresco.webservice.repository.RepositoryServiceSoapBindingStub;
import org.alfresco.webservice.repository.UpdateResult;
import org.alfresco.webservice.types.CML;
import org.alfresco.webservice.types.CMLCopy;
import org.alfresco.webservice.types.CMLCreate;
import org.alfresco.webservice.types.CMLDelete;
import org.alfresco.webservice.types.CMLMove;
import org.alfresco.webservice.types.CMLUpdate;
import org.alfresco.webservice.types.ContentFormat;
import org.alfresco.webservice.types.NamedValue;
import org.alfresco.webservice.types.Node;
import org.alfresco.webservice.types.ParentReference;
import org.alfresco.webservice.types.Predicate;
import org.alfresco.webservice.types.Query;
import org.alfresco.webservice.types.QueryConfiguration;
import org.alfresco.webservice.types.Reference;
import org.alfresco.webservice.types.ResultSet;
import org.alfresco.webservice.types.ResultSetRow;
import org.alfresco.webservice.types.Store;
import org.alfresco.webservice.util.AuthenticationDetails;
import org.alfresco.webservice.util.AuthenticationUtils;
import org.alfresco.webservice.util.Constants;
import org.alfresco.webservice.util.ContentUtils;
import org.alfresco.webservice.util.WebServiceFactory;
import org.apache.log4j.Logger;

import org.alfresco.service.cmr.security.PersonService; 

/**
 * 
 * @version 1.0
 * 
 *          This class that represents the Alfresco's manager manages all
 *          functions for interacting with Alfresco's system. Alfresco offers
 *          the web services SOAP and Rest for access it. Then, this class calls
 *          the web services for inserting the users, for creating spaces and
 *          documents, for managing roles and for many other functionalities.
 * 
 */

public class AlfrescoManager
{

    private static Logger                        logger                        = Logger.getLogger(AlfrescoManager.class);

    private static final String                  SERVICE_EP_LUCENE_SEARCH_PATH = "/service/vcdi/luceneSearch?q=";

    public static final Store                    STORE                         = new Store(Constants.WORKSPACE_STORE, "SpacesStore");

    public static final ParentReference          VCDIFolder                    = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL",
                                                                                           Constants.ASSOC_CONTAINS, null);

    public static final ParentReference          VCDIStore                     = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:Store",
                                                                                           Constants.ASSOC_CONTAINS, null);

    protected String                             endPointAddress;

    protected String                             server_username;

    protected String                             server_password;

    public static boolean                        init                          = false;

    private AuthenticationDetails                authenticationDetails;

    private AdministrationServiceSoapBindingStub administrationService;

    private AccessControlServiceSoapBindingStub  accessControlService;

    // EngineConfiguration config = new FileProvider(
    // new ByteArrayInputStream(AlfrescoManager.WS_SECURITY_INFO.getBytes()));
    //
    // RepositoryServiceLocator repositoryServiceLocator = new
    // RepositoryServiceLocator(config);

    // RepositoryServiceSoapBindingStub repositoryService =
    // (RepositoryServiceSoapBindingStub)
    // repositoryServiceLocator.getRepositoryService();

    private AuthenticationDetails getAuthenticationDetails()
    {

        return authenticationDetails;
    }

    private void setAuthenticationDetails( AuthenticationDetails authenticationDetails )
    {

        this.authenticationDetails = authenticationDetails;
    }

    private String getEndPointAddress()
    {

        return endPointAddress;
    }

    private void setEndPointAddress( String endPointAddress )
    {

        this.endPointAddress = endPointAddress;
    }

    /**
     * Class constructor.
     */
    public AlfrescoManager ()
    {

        logger.info("AlfrescoManager::AlfrescoManager()");
        setAuthenticationDetails(null);

    }

    public void afterPropertiesSet() throws Exception
    {

        WebServiceFactory.setEndpointAddress(getEndPointAddress());

        administrationService = WebServiceFactory.getAdministrationService();
        accessControlService = WebServiceFactory.getAccessControlService(); 
    }

    /**
     * Opens a session with Alfresco by setting the parameters needed. The
     * method doesn't have the input parameters.
     */
    public synchronized void authenticateAsAdmin( String user , String pass ) throws Exception
    {

        logger.info("AlfrescoManager::authenticateAsAdmin()");

        try
        {
            // Setting the endpoint address
            WebServiceFactory.setEndpointAddress(getEndPointAddress());
            // Starting the session
            AuthenticationUtils.startSession(user, pass);
            setAuthenticationDetails(AuthenticationUtils.getAuthenticationDetails());
            logger.debug("Connected with the Alfresco's repository as admin");
        }
        catch (Exception e)
        {
            logger.error("Can not load webserviceclient.properties or " + "Can not initiate session with Alfresco server", e);
            throw new Exception("LoginAsAdmin failed");
        }
        logger.debug("authenticateAsAdmin successfully");
    }

    /**
     * Opens a session with Alfresco by setting the parameters needed. The
     * method doesn't have the input parameters.
     */
    public synchronized List<String> authenticateAsUser( String userName , String password ) throws Exception
    {

        logger.info("AlfrescoManager::authenticateAsUser()");
        List<String> result = null;
        try
        {
            // Setting the endpoint address
            WebServiceFactory.setEndpointAddress(getEndPointAddress());
            // Starting the session
            AuthenticationUtils.startSession(userName, password);
            setAuthenticationDetails(AuthenticationUtils.getAuthenticationDetails());
            AccessControlServiceSoapBindingStub accessControlService = WebServiceFactory.getAccessControlService();
            String[] resultarray = accessControlService.getAuthorities();
            result = Arrays.asList(resultarray);
            logger.debug("Connected with the Alfresco's repository");
        }
        catch (Exception e)
        {
            logger.error("error login", e);
            throw new Exception("authenticateAsUser failed");
        }
        logger.debug("authenticateAsUser successfully");
        return result;
    }

    /**
     * Specifies the values of custom properties and not for an document.
     */
    private static NamedValue[] buildCustomProperties( String name , String description , String expiryDate , String signEnabled , String stateOfSign ,
                String attachment )
    {

        logger.info("AlfrescoManager::buildCustomProperties()");
        NamedValue[] properties = new NamedValue[6];
        properties[0] = org.alfresco.webservice.util.Utils.createNamedValue(Constants.PROP_NAME, name);
        properties[1] = org.alfresco.webservice.util.Utils.createNamedValue(Constants.PROP_DESCRIPTION, description);
        properties[2] = org.alfresco.webservice.util.Utils.createNamedValue("{custom.model}ExpiryDate", expiryDate);
        properties[3] = org.alfresco.webservice.util.Utils.createNamedValue("{custom.model}SignEnabled", signEnabled);
        properties[4] = org.alfresco.webservice.util.Utils.createNamedValue("{custom.model}StateOfSign", stateOfSign);
        properties[5] = org.alfresco.webservice.util.Utils.createNamedValue("{custom.model}Attachment", attachment);
        logger.debug("buildCustomProperties successfully");
        return properties;
    }

    /**
     * Return a reference that is the parent of the parameter passed.
     */
    protected static ParentReference ReferenceToParent( Reference spaceref )
    {

        logger.info("AlfrescoManager::ReferenceToParent()");
        ParentReference parent = new ParentReference();
        parent.setStore(STORE);
        parent.setPath(spaceref.getPath());
        parent.setUuid(spaceref.getUuid());
        parent.setAssociationType(Constants.ASSOC_CONTAINS);
        logger.debug("ReferenceToParent successfully");
        return parent;
    }

    /**
     * Blanks are allowed in space names but not in paths. Because the path
     * should depend on the name we need a version of the name which contains no
     * blanks then the function normalizes the name substituting the blanks with
     * the underscore.
     */
    protected static String normilizeNodeName( String name )
    {

        logger.info("AlfrescoManager::normilizeNodeName()");
        return name.replaceAll(" ", "_");
    }

    /**
     * Returns the reference to Company Home.
     */
    protected static ParentReference getCompanyHome()
    {

        logger.info("AlfrescoManager::getCompanyHome()");
        /*
         * Method definition: ParentReference(Store store, java.lang.String
         * uuid, java.lang.String path, java.lang.String associationType,
         * java.lang.String childName)
         */
        ParentReference companyHomeParent = new ParentReference(STORE, null, "/app:company_home", Constants.ASSOC_CONTAINS, null);
        logger.debug("getCompanyHome successfully");
        return companyHomeParent;
    }

    /**
     * Creates a space inside another space.
     */
    public Reference createSpace( Reference parentref , String spacename ) throws Exception
    {

        logger.info("AlfrescoManager::createSpace(Reference, String)");
        Reference space = null;
        ParentReference parent = ReferenceToParent(parentref);
        boolean exist = false;
        try
        {
            logger.debug("Entering space " + spacename);
            space = new Reference(STORE, null, parent.getPath() + "/cm:" + normilizeNodeName(spacename));
            WebServiceFactory.getRepositoryService().get(new Predicate(new Reference[]
            {
                space
            }, STORE, null));
            exist = true;
        }
        catch (Exception e1)
        {
            logger.warn("The space named " + spacename + " does not exist...creating it!");
            parent.setChildName(Constants.createQNameString(Constants.NAMESPACE_CONTENT_MODEL, normilizeNodeName(spacename)));

            // Set the space's property name
            NamedValue[] properties = new NamedValue[]
            {
                org.alfresco.webservice.util.Utils.createNamedValue(Constants.PROP_NAME, spacename)
            };
            // Create the space using CML (Content Manipulation Language)
            CMLCreate create = new CMLCreate("1", parent, null, null, null, Constants.TYPE_FOLDER, properties);
            CML cml = new CML();
            cml.setCreate(new CMLCreate[]
            {
                create
            });

            // Execute the CML create statement
            try
            {
                WebServiceFactory.getRepositoryService().update(cml);
            }
            catch (Exception e2)
            {
                logger.error("Can not create the space");
                throw new Exception(e2);
            }
        }
        if (exist)
            throw new Exception();
        logger.debug("createSpace(Reference, String) successfully");
        return space;
    }

    public void createSpaceAbsolutePath( String path , String spacename ) throws Exception
    {

        logger.info("AlfrescoManager::createSpaceAbsolutePath()");
        Reference space = null;

        ParentReference parentref = new ParentReference(STORE, null, path, Constants.ASSOC_CONTAINS, null);

        ParentReference parent = ReferenceToParent(parentref);
        try
        {
            logger.debug("Entering space " + spacename);
            space = new Reference(STORE, null, parent.getPath() + "/cm:" + normilizeNodeName(spacename));
            WebServiceFactory.getRepositoryService().get(new Predicate(new Reference[]
            {
                space
            }, STORE, null));
        }
        catch (Exception e1)
        {
            logger.warn("The space named " + spacename + " does not exist...creating it!");
            parent.setChildName(Constants.createQNameString(Constants.NAMESPACE_CONTENT_MODEL, normilizeNodeName(spacename)));

            // Set the space's property name
            NamedValue[] properties = new NamedValue[]
            {
                org.alfresco.webservice.util.Utils.createNamedValue(Constants.PROP_NAME, spacename)
            };
            // Create the space using CML (Content Manipulation Language)
            CMLCreate create = new CMLCreate("1", parent, null, null, null, Constants.TYPE_FOLDER, properties);
            CML cml = new CML();
            cml.setCreate(new CMLCreate[]
            {
                create
            });

            // Execute the CML create statement
            try
            {
                WebServiceFactory.getRepositoryService().update(cml);
            }
            catch (Exception e2)
            {
                logger.error("Can not create the space");
                throw new Exception(e2);
            }
        }
        logger.debug("createSpaceAbsolutePath successfully");
    }

    /**
     * Creates a node into a space that represent a document.
     */
    protected Reference createDocument( Reference parentref , String docname , byte[] content , String expiryDate , String mimetype , String description ,
                String signerEnabled , String stateOfSign , String attachment ) throws Exception
    {

        logger.info("AlfrescoManager::createDocument()");
        if (expiryDate != null && expiryDate.trim().equals(""))
        {
            expiryDate = null;
        }
        Reference document = null;
        ParentReference parent = ReferenceToParent(parentref);
        parent.setChildName(Constants.createQNameString(Constants.NAMESPACE_CONTENT_MODEL, normilizeNodeName(docname)));

        NamedValue[] properties = buildCustomProperties(docname, description, expiryDate, signerEnabled, stateOfSign, attachment);

        CMLCreate create = new CMLCreate("1", parent, null, null, null, Constants.TYPE_CONTENT, properties);
        CML cml = new CML();
        cml.setCreate(new CMLCreate[]
        {
            create
        });

        // Execute the CML create statement
        UpdateResult[] results = null;
        try
        {
            logger.debug("Creating the document " + docname);
            results = WebServiceFactory.getRepositoryService().update(cml);
            document = results[0].getDestination();
        }
        catch (Exception e)
        {
            logger.error("Can not create the document", e);
            throw new Exception(e);
        }
        // Set the content
        ContentFormat format = new ContentFormat(mimetype, "UTF-8");
        try
        {
            logger.debug("Setting the content of the document");
            WebServiceFactory.getContentService().write(document, Constants.PROP_CONTENT, content, format);
        }
        catch (Exception e2)
        {
            logger.error("Can not set the content of the document", e2);
            throw new Exception(e2);
        }
        logger.debug("createDocument successfully");
        return document;
    }

    /**
     * Deletes a space.
     */
    public void deleteSpace( Reference space ) throws Exception
    {

        logger.info("AlfrescoManager::deleteSpace()");
        CMLDelete delete = new CMLDelete(new Predicate(new Reference[]
        {
            space
        }, null, null));
        CML cml = new CML();
        cml.setDelete(new CMLDelete[]
        {
            delete
        });

        // Execute the CMLDelete statement
        try
        {
            logger.debug("Deleting the space " + space.getPath());
            WebServiceFactory.getRepositoryService().update(cml);
        }
        catch (Exception e2)
        {
            logger.error("Can not delete the space");
            throw new Exception(e2);
        }
        logger.debug("deleteSpace successfully");
    }

    /**
     * Adds a user into Alfresco.
     */
    public static void userAdd( HashMap<String, String> param ) throws Exception , RepositoryFault
    {

        logger.info("AlfrescoManager::userAdd()");
        RepositoryServiceSoapBindingStub repositoryService = WebServiceFactory.getRepositoryService();
        AdministrationServiceSoapBindingStub administrationService = WebServiceFactory.getAdministrationService();

        int batchSize = 5;
        QueryConfiguration queryCfg = new QueryConfiguration();
        queryCfg.setFetchSize(batchSize);
        WebServiceFactory.getAdministrationService().setHeader(new RepositoryServiceLocator().getServiceName().getNamespaceURI(), "QueryHeader", queryCfg);

        // Check the user
        boolean existUser = false;
        try
        {
            administrationService.getUser(param.get(VCDICostants.USER_NAME));
            existUser = true;
        }
        catch (Exception e)
        {
            existUser = false;
            // throw new Exception(e);
        }

        if (existUser)
        {
            throw new Exception("User " + param.get(VCDICostants.USER_NAME) + " already exists");
        }

        Reference folderReference = new Reference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:" + param.get(VCDICostants.HOMEFOLDER));
        Node[] nodes = null;

        // Try to create the folder
        try
        {
            nodes = repositoryService.get(new Predicate(new Reference[]
            {
                folderReference
            }, STORE, null));
        }
        catch (Exception e)
        {
            // Create parent reference to company home
            ParentReference parentReference = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL", Constants.ASSOC_CONTAINS,
                        Constants.createQNameString(Constants.NAMESPACE_CONTENT_MODEL, param.get(VCDICostants.HOMEFOLDER)));
            // Create folder
            NamedValue[] properties = new NamedValue[]
            {
                new NamedValue(Constants.PROP_NAME, false, param.get(VCDICostants.HOMEFOLDER), null)
            };
            CMLCreate create = new CMLCreate("1", parentReference, null, null, null, Constants.TYPE_FOLDER, properties);
            CML cml = new CML();
            cml.setCreate(new CMLCreate[]
            {
                create
            });
            try
            {
                WebServiceFactory.getRepositoryService().update(cml);
            }
            catch (RemoteException e1)
            {
                e1.printStackTrace();
            }
        }
        // Get the reference for the folder
        Reference userFolder = null;
        try
        {
            nodes = repositoryService.get(new Predicate(new Reference[]
            {
                folderReference
            }, STORE, null));
            if (nodes.length == 1)
            {
                userFolder = nodes[0].getReference();
            }
        }
        catch (Exception e)
        {
            throw new Exception("Unable to create the new folder for user " + param.get(VCDICostants.USER_NAME), e);
        }

        try
        {
            String homeFolder = STORE.getScheme() + "://" + STORE.getAddress() + "/" + userFolder.getUuid();
            // setInheritPermission to false for user home page
            Reference r = new Reference(STORE, userFolder.getUuid(), homeFolder);
            Predicate predicate = new Predicate(new Reference[]
            {
                r
            }, STORE, null);
            WebServiceFactory.getAccessControlService().setInheritPermission(predicate, Boolean.FALSE);
            // Create the new users
            NewUserDetails newUser = new NewUserDetails(param.get(VCDICostants.USER_NAME), param.get(VCDICostants.PASSWORD), createPersonProperties(homeFolder,
                        param.get(VCDICostants.FIRST_NAME), "", param.get(VCDICostants.LAST_NAME), param.get(VCDICostants.E_MAIL),
                        param.get(VCDICostants.USER_ORG)));
            UserDetails[] createUsers = administrationService.createUsers(new NewUserDetails[]
            {
                newUser
            });
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to create the new user", e);
        }

        logger.debug("The user " + param.get(VCDICostants.USER_NAME) + " has been created and her folder too");
        logger.debug("userAdd successfully");
    }

    private static NamedValue[] createPersonProperties( String homeFolder , String firstName , String middleName , String lastName , String email , String orgId )
    {

        // Create the new user objects
        logger.info("AlfrescoManager::createPersonProperties()");
        return new NamedValue[]
        {
                    new NamedValue(Constants.PROP_USER_HOMEFOLDER, false, homeFolder, null),
                    new NamedValue(Constants.PROP_USER_FIRSTNAME, false, firstName, null),
                    new NamedValue(Constants.PROP_USER_MIDDLENAME, false, middleName, null),
                    new NamedValue(Constants.PROP_USER_LASTNAME, false, lastName, null), new NamedValue(Constants.PROP_USER_EMAIL, false, email, null),
                    new NamedValue(Constants.PROP_USER_ORGID, false, orgId, null)
        };
    }

    /**
     * Creates a group.
     */
    public static void createGroups( String[] groupName ) throws AccessControlFault , RemoteException
    {

        logger.info("AlfrescoManager::createGroups()");
        AccessControlServiceSoapBindingStub accessControlService = WebServiceFactory.getAccessControlService();

        NewAuthority cpGrpAuth1 = new NewAuthority("GROUP", groupName[0]);
        NewAuthority cpGrpAuth2 = new NewAuthority("GROUP", groupName[1]);
        NewAuthority cpGrpAuth3 = new NewAuthority("GROUP", groupName[2]);

        NewAuthority[] newAuthorities =
        {
                    cpGrpAuth1, cpGrpAuth2, cpGrpAuth3
        };
        accessControlService.createAuthorities(null, newAuthorities);
        logger.debug("createGroups successfully");
    }

    /**
     * Deletes a group.
     */
    public boolean deleteGroup( String groupname ) throws Exception
    {

        logger.info("AlfrescoManager::deleteGroup()");
        boolean control = true;
        String[] delgroup =
        {
            groupname
        };
        try
        {
            AccessControlServiceSoapBindingStub accessControlService = WebServiceFactory.getAccessControlService();
            accessControlService.deleteAuthorities(delgroup);
        }
        catch (Exception e)
        {
            logger.error("Can not detele the group");
            control = false;
            throw new Exception(e);
        }
        logger.debug("deleteGroup successfully");
        return control;
    }

    /**
     * Adds a user in a group.
     */
    public void addUsersToGroup( String userToAdd , String groupName ) throws AccessControlFault , RemoteException
    {

        logger.info("AlfrescoManager::addUsersToGroup()");
        String[] cpUsers =
        {
            userToAdd
        };
        String parentAuthority = "GROUP" + "_" + groupName;

        AccessControlServiceSoapBindingStub accessControlService = WebServiceFactory.getAccessControlService();
        accessControlService.addChildAuthorities(parentAuthority, cpUsers);
        logger.debug("addUsersToGroup successfully");
    }

    /**
     * Closes the session.
     */
    public synchronized void endSession()
    {

        logger.info("AlfrescoManager::endSession()");
        AuthenticationUtils.endSession();
        setAuthenticationDetails(null);
        logger.debug("Connection closed");
    }

    /**
     * Adds a user into Alfresco.
     * 
     * @throws Exception
     */
    public void addUserVCDI( HashMap<String, String> param ) throws Exception
    {

        logger.info("AlfrescoManager::addUserVCDI()");
        if (param.get(VCDICostants.GROUPNAME) != null && param.get(VCDICostants.GROUPNAME).equals(VCDICostants.GROUPEO))
        {
            addUserEOPO(param);
        }
        else
        {
            addUserCA(param);
        }
        logger.debug("addUserVCDI successfully");
    }

    /**
     * Adds a EO and PO user.
     */
    private void addUserEOPO( HashMap<String, String> param ) throws Exception
    {

        logger.info("AlfrescoManager::addUserEOPO()");
        try
        {
            authenticateAsAdmin(param.get(VCDICostants.USER_NAME), param.get(VCDICostants.PASSWORD));
            userAdd(param);
            addUsersToGroup(param.get(VCDICostants.USER_NAME), param.get(VCDICostants.GROUPNAME));
            endSession();
            authenticateAsUser(param.get(VCDICostants.USER_NAME), param.get(VCDICostants.PASSWORD));

            ParentReference homePageUser = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:" + param.get(VCDICostants.HOMEFOLDER),
                        Constants.ASSOC_CONTAINS, null);
            createSpace(homePageUser, "SAFE");
            createSpace(homePageUser, "WORKSPACE");
            endSession();
        }
        catch (Exception e)
        {
            logger.error("addUserEOPO failed");
            throw new Exception(e);
        }
        logger.debug("addUserEOPO successfully");
    }

    /**
     * Adds a CA user.
     */
    private void addUserCA( HashMap<String, String> param ) throws Exception
    {

        logger.info("AlfrescoManager::addUserCA()");
        try
        {
            authenticateAsAdmin(param.get(VCDICostants.USER_NAME), param.get(VCDICostants.PASSWORD));
            logger.debug("AlfrescoManager::createUser");
            NewUserDetails userDetails = new NewUserDetails();
            userDetails.setUserName(param.get(VCDICostants.USER_NAME));
            userDetails.setPassword(param.get(VCDICostants.PASSWORD));

            NamedValue[] properties = new NamedValue[4];
            properties[0] = new NamedValue(Constants.PROP_USER_FIRSTNAME, false, param.get(VCDICostants.FIRST_NAME), null);
            properties[1] = new NamedValue(Constants.PROP_USER_LASTNAME, false, param.get(VCDICostants.LAST_NAME), null);
            properties[2] = new NamedValue(Constants.PROP_USER_EMAIL, false, param.get(VCDICostants.E_MAIL), null);
            properties[3] = new NamedValue(Constants.PROP_USER_ORGID, false, param.get(VCDICostants.USER_ORG), null);

            userDetails.setProperties(properties);
            NewUserDetails[] newUsers = new NewUserDetails[1];
            newUsers[0] = userDetails;
            WebServiceFactory.getAdministrationService().createUsers(newUsers);
            addUsersToGroup(param.get(VCDICostants.USER_NAME), param.get(VCDICostants.GROUPNAME));
            endSession();
        }
        catch (Exception e)
        {
            logger.error("addUserCA failed", e);
            throw new Exception(e);
        }
        logger.debug("addUserCA successfully");
    }

    /**
     * Loads an evidence.
     */
    public void uploadEvidence( String userName , String fileName , byte[] content , String mimeType , String description , String expiryDate ,
                String stateOfSign , String attachment ) throws Exception
    {

        logger.info("AlfrescoManager::uploadEvidence()");
        try
        {
            ParentReference safeUser = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase() + "/cm:SAFE",
                        Constants.ASSOC_CONTAINS, null);

            createDocument(safeUser, fileName, content, expiryDate, mimeType, description, "", stateOfSign, attachment);

        }
        catch (Exception e)
        {
            logger.error("uploadEvidence failed");
            throw new Exception(e);
        }
        logger.debug("uploadEvidence successfully");
    }

    /**
     * Loads a skeleton.
     */
    public void uploadSkeleton( String customSpaceName , File zipFile , String signerEnabled ) throws Exception
    {

        logger.info("AlfrescoManager::uploadSkeleton()");
        try
        {
            String userName = getAuthenticationDetails().getUserName();
            ParentReference homeUser = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase()
                        + "/cm:WORKSPACE", Constants.ASSOC_CONTAINS, null);
            Reference space = createSpace(homeUser, customSpaceName);
            byte[] content = null;
            // byte[] content = Utils.fileToByteArray(zipFile);

            createDocument(space, zipFile.getName(), content, null, "application/zip", "", signerEnabled, "", "");

        }
        catch (Exception spaceExistException)
        {
            logger.error("uploadSkeleton failed");
            throw spaceExistException;
        }

        logger.debug("uploadSkeleton successfully");
    }

    /**
     * Move a space.
     */
    public void moveSpace( String pathSpace , String pathDest , String nameSpace ) throws Exception
    {

        logger.info("AlfrescoManager::moveSpace()");
        ParentReference refSpace = new ParentReference(STORE, null, pathSpace, Constants.ASSOC_CONTAINS, null);
        ParentReference refDest = new ParentReference(STORE, null, pathDest, Constants.ASSOC_CONTAINS, null);
        ParentReference parentDest = ReferenceToParent(refDest);
        parentDest.setChildName(Constants.createQNameString(Constants.NAMESPACE_CONTENT_MODEL, normilizeNodeName(nameSpace)));

        CMLMove move = new CMLMove();
        move.setTo(parentDest);
        move.setWhere(new Predicate(new Reference[]
        {
            refSpace
        }, STORE, null));

        NamedValue[] properties = new NamedValue[]
        {
            org.alfresco.webservice.util.Utils.createNamedValue(Constants.PROP_NAME, nameSpace)
        };
        CMLUpdate update = new CMLUpdate();
        update.setProperty(properties);
        update.setWhere(new Predicate(new Reference[]
        {
            refSpace
        }, STORE, null));

        CML cml = new CML();
        cml.setMove(new CMLMove[]
        {
            move
        });
        cml.setUpdate(new CMLUpdate[]
        {
            update
        });

        // Execute the CML move and Update statement
        try
        {
            logger.debug("Moving the space with path " + refSpace.getPath() + " or id " + refSpace.getUuid() + "\n" + "to destination space with path "
                        + refDest.getPath() + " or id " + refDest.getUuid() + "\n" + "by using the name " + nameSpace);
            WebServiceFactory.getRepositoryService().update(cml);

        }
        catch (Exception e)
        {
            logger.error("Can not move the space");
            throw new Exception(e);
        }
        logger.debug("moveSpace successfully");
    }

    /**
     * Copy a document.
     */
    public void copyDoc( String pathSpace , String pathDest , String nameDoc ) throws Exception
    {

        logger.info("AlfrescoManager::copyDoc()");
        ParentReference refSpace = new ParentReference(STORE, null, pathSpace, Constants.ASSOC_CONTAINS, null);
        ParentReference refDest = new ParentReference(STORE, null, pathDest, Constants.ASSOC_CONTAINS, null);
        ParentReference parentDest = ReferenceToParent(refDest);
        parentDest.setChildName(Constants.createQNameString(Constants.NAMESPACE_CONTENT_MODEL, normilizeNodeName(nameDoc)));

        CMLCopy copy = new CMLCopy();
        copy.setTo(parentDest);
        copy.setWhere(new Predicate(new Reference[]
        {
            refSpace
        }, STORE, null));

        NamedValue[] properties = new NamedValue[]
        {
            org.alfresco.webservice.util.Utils.createNamedValue(Constants.PROP_NAME, nameDoc)
        };
        CMLUpdate update = new CMLUpdate();
        update.setProperty(properties);
        update.setWhere(new Predicate(new Reference[]
        {
            refSpace
        }, STORE, null));

        CML cml = new CML();
        cml.setCopy(new CMLCopy[]
        {
            copy
        });
        cml.setUpdate(new CMLUpdate[]
        {
            update
        });

        // Execute the CML copy and Update statement
        try
        {
            logger.debug("Copying the space with path " + refSpace.getPath() + " or id " + refSpace.getUuid() + "\n" + "to destination space with path "
                        + refDest.getPath() + " or id " + refDest.getUuid() + "\n" + "by using the name " + nameDoc);
            WebServiceFactory.getRepositoryService().update(cml);

        }
        catch (Exception e)
        {
            logger.error("Can not copy the space");
            throw new Exception(e);
        }
        logger.debug("copyDoc successfully");
    }

    /**
     * Gets the content of a node.
     */
    public byte[] getContent( Reference node ) throws Exception
    {

        logger.info("AlfrescoManager::getContent()");
        Content content = null;
        logger.debug("Getting content of document with path " + node.getPath() + " or id " + node.getUuid() + ".");

        try
        {
            Content[] read = WebServiceFactory.getContentService().read(new Predicate(new Reference[]
            {
                node
            }, STORE, null), Constants.PROP_CONTENT);
            content = read[0];

            logger.debug("Got " + read.length + " content elements.");
            logger.debug("The first content element has a size of " + content.getLength() + " segments");
        }
        catch (Exception e)
        {
            logger.error("Can not get the content");
            throw new Exception(e);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = ContentUtils.getContentAsInputStream(content);
        byte[] buf = new byte[2048];
        int size;
        try
        {
            while ((size = in.read(buf)) != -1)
            {
                out.write(buf, 0, size);
            }
        }
        catch (IOException e)
        {
            throw new Exception(e);
        }
        logger.debug("getContent successfully");
        return out.toByteArray();
    }

    /**
     * General Lucene query.
     */
    public ResultSetRow[] queryLucene( String path , String type ) throws Exception
    {

        logger.info("AlfrescoManager::queryLucene()");
        ResultSetRow[] results = null;
        path = path.replaceAll(" ", "_");
        String luceneQuery = "PATH:" + path + type;
        logger.debug(luceneQuery);
        RepositoryServiceSoapBindingStub repositoryService = WebServiceFactory.getRepositoryService();
        Query query = new Query(Constants.QUERY_LANG_LUCENE, luceneQuery);
        try
        {
            // Execute the query
            QueryResult queryResult = repositoryService.query(STORE, query, false);
            // Display the results
            ResultSet resultSet = queryResult.getResultSet();
            results = resultSet.getRows();
        }
        catch (RepositoryFault e)
        {
            logger.error("RepositoryFault");
            throw new Exception(e);
        }
        catch (Exception e)
        {
            logger.error("queryLucene failed");
            throw new Exception(e);
        }
        logger.debug("queryLucene successfully");
        return results;
    }

    /**
     * Gets the list of VCDPackage names existing into Workspace.
     * 
     * @throws Exception
     */
    public List<String> getVCDPackageName( String userName ) throws Exception
    {

        logger.info("AlfrescoManager::getVCDContainerName()");
        List<String> listFolder = new LinkedList<String>();
        String path = "\"/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase() + "/cm:WORKSPACE/*\"";

        ResultSetRow[] results = queryLucene(path, " AND TYPE:\"cm:folder\"");

        for (ResultSetRow resultRow : results)
        {
            NamedValue[] properties = resultRow.getColumns();

            for (NamedValue array : properties)
            {
                if (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}name"))
                {
                    listFolder.add(array.getValue());
                    logger.debug("Name space: " + array.getValue());
                }
            }
        }
        logger.debug("getVCDContainerName successfully");
        return listFolder;
    }

    /**
     * Gets the list of record existing into Safe.
     * 
     * @throws ParseException
     */
    // public List<DocRefType> getListDocSafe( String userName ) throws
    // ParseException
    // {
    //
    // logger.info("AlfrescoManager::getListRefSafe()");
    // List<DocRefType> listDoc = new LinkedList<DocRefType>();
    //
    // String path = "\"/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" +
    // userName.toUpperCase() + "/cm:SAFE/*\"";
    //
    // ResultSetRow[] results = queryLucene(path, "AND TYPE:\"cm:content\"");
    //
    // if (results != null)
    // {
    // for (ResultSetRow resultRow : results)
    // {
    //
    // NamedValue[] properties = resultRow.getColumns();
    // DocRefType drt = new DocRefType();
    //
    // for (NamedValue array : properties)
    // {
    // if
    // (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}name"))
    // {
    // drt.setFileName(array.getValue());
    // }
    // if
    // (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}description"))
    // {
    // logger.debug(array.getValue());
    // drt.setDescription(array.getValue());
    // }
    // if (array.getName().equalsIgnoreCase("{custom.model}ExpiryDate"))
    // {
    // if (array.getValue() != null)
    // {
    // SimpleDateFormat inputFormatter = new SimpleDateFormat("yyyy-MM-dd");
    // Date d = inputFormatter.parse(array.getValue().substring(0, 10));
    // SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    // drt.setDate(formatter.format(d));
    // }
    // }
    // if (array.getName().equalsIgnoreCase("{custom.model}StateOfSign"))
    // {
    // if (array.getValue() != null)
    // {
    // logger.debug(array.getValue());
    // drt.setStateOfSign(array.getValue());
    // }
    // }
    //
    // }
    // listDoc.add(drt);
    // }
    // }
    // else
    // return null;
    //
    // logger.debug("getListDocSafe successfully");
    // return listDoc;
    // }

    /**
     * Gets the list of VCD existing into VCD Package.
     * 
     * @throws Exception
     */
    public List<String> getVCDsPackList( String userName , String spaceName ) throws Exception
    {

        logger.info("AlfrescoManager::getVCDsPackList()");
        List<String> listFolder = new ArrayList<String>();
        String path = "\"/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase() + "/cm:WORKSPACE/cm:" + spaceName + "/*\"";

        ResultSetRow[] results = queryLucene(path, "AND TYPE:\"cm:folder\"");

        for (ResultSetRow resultRow : results)
        {
            NamedValue[] properties = resultRow.getColumns();

            for (NamedValue array : properties)
            {
                if (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}name"))
                {
                    listFolder.add(array.getValue());
                    logger.debug("Name space: " + array.getValue());
                }
            }
        }
        logger.debug("getVCDsPackList successfully");
        return listFolder;
    }

    /**
     * Gets a XML file of a VCDPackage.
     */
    public byte[] getVCDContainerXML( String spaceName , String userName ) throws Exception
    {

        logger.info("AlfrescoManager::getVCDContainerXML()");
        byte[] fileXML = null;
        String nameFile = null;
        spaceName = spaceName.replaceAll(" ", "_");
        String path = "\"/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase() + "/cm:WORKSPACE/cm:" + spaceName + "/*\"";

        ResultSetRow[] results = queryLucene(path, "AND TYPE:\"cm:content\"");
        boolean found = false;
        for (ResultSetRow resultRow : results)
        {
            NamedValue[] properties = resultRow.getColumns();
            for (NamedValue array : properties)
            {
                logger.debug(array.getName());
                if (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}name") && (array.getValue().contains("xml")))
                {
                    nameFile = array.getValue();
                    logger.debug("Name file: " + array.getValue());
                    ParentReference node = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase()
                                + "/cm:WORKSPACE/cm:" + spaceName + "/cm:" + nameFile, Constants.ASSOC_CONTAINS, null);
                    try
                    {
                        fileXML = getContent(node);
                        found = true;
                        break;
                    }
                    catch (Exception e)
                    {
                        logger.error("getVCDContainerXML failed");
                        throw new Exception(e);
                    }
                }
            }
            if (found)
                break;
        }
        logger.debug("getVCDContainerXML successfully");
        return fileXML;
    }

    /**
     * Update a XML file into Safe.
     */
    public void updateDocRef( String userName , String docRef , String fileName , String description , String expiryDate , String stateOfSign )
                throws Exception
    {

        logger.info("AlfrescoManager::updateDocRef()");

        String xmlFileName = fileName;
        // xmlFileName =
        // xmlFileName.replace(Utils.getFileExtension(xmlFileName), ".xml");

        try
        {
            byte[] file = docRef.getBytes("UTF-8");
            ParentReference safe = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase() + "/cm:SAFE",
                        Constants.ASSOC_CONTAINS, null);
            ParentReference doc = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase() + "/cm:SAFE/cm:"
                        + xmlFileName, Constants.ASSOC_CONTAINS, null);
            deleteSpace(doc);
            createDocument(safe, xmlFileName, file, expiryDate, "application/xml", description, "", "", "false");

            ParentReference attachment = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase()
                        + "/cm:SAFE/cm:" + fileName, Constants.ASSOC_CONTAINS, null);
            NamedValue[] properties = new NamedValue[3];
            properties[0] = org.alfresco.webservice.util.Utils.createNamedValue(Constants.PROP_DESCRIPTION, description);
            properties[1] = org.alfresco.webservice.util.Utils.createNamedValue("{custom.model}ExpiryDate", expiryDate);
            properties[2] = org.alfresco.webservice.util.Utils.createNamedValue("{custom.model}StateOfSign", stateOfSign);

            // update node
            CMLUpdate update = new CMLUpdate();
            update.setProperty(properties);
            update.setWhere(new Predicate(new Reference[]
            {
                attachment
            }, STORE, null));
            CML cmlUpdate = new CML();
            cmlUpdate.setUpdate(new CMLUpdate[]
            {
                update
            });
            // perform a CML update
            WebServiceFactory.getRepositoryService().update(cmlUpdate);

        }
        catch (Exception e)
        {
            logger.error("updateDocRef failed");
            throw new Exception(e);
        }
        logger.debug("updateDocRef successfully");
    }

    /**
     * Delete a record into Safe.
     */
    public void deleteRecord( String userName , String fileName ) throws Exception
    {

        logger.info("AlfrescoManager::deleteRecord()");
        try
        {
            ParentReference doc = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase() + "/cm:SAFE/cm:"
                        + fileName, Constants.ASSOC_CONTAINS, null);
            deleteSpace(doc);
        }
        catch (Exception e)
        {
            logger.error("deleteRecord failed");
            throw new Exception(e);
        }
        logger.debug("deleteRecord successfully");
    }

    /**
     * Gets a record from Safe.
     */
    public byte[] getRecordSafe( String fileName , String userName ) throws Exception
    {

        logger.info("AlfrescoManager::getRecordSafe()");
        byte[] fileXML = null;
        String path = "\"/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase() + "/cm:SAFE/*\"";
        boolean found = false;
        try
        {
            ResultSetRow[] results = queryLucene(path, "AND TYPE:\"cm:content\"");
            for (ResultSetRow resultRow : results)
            {
                NamedValue[] properties = resultRow.getColumns();
                for (NamedValue array : properties)
                {
                    if (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}name") && (array.getValue().equalsIgnoreCase(fileName)))
                    {
                        Reference node = new Reference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase() + "/cm:SAFE/cm:"
                                    + fileName);
                        fileXML = getContent(node);
                        found = true;
                        break;
                    }
                }
                if (found)
                    break;
            }
        }
        catch (Exception e)
        {
            logger.error("getRecordSafe failed");
            throw new Exception(e);
        }
        logger.debug("getRecordSafe successfully");
        return fileXML;
    }

    /**
     * Load a XML file into VCDPackage.
     */
    public void loadVCDPackage( String userName , String packName , String docRef , String fileName , String signerEnabled ) throws Exception
    {

        logger.info("AlfrescoManager::loadVCDPackage()");
        try
        {
            packName = packName.replaceAll(" ", "_");
            byte[] file = docRef.getBytes("UTF-8");
            ParentReference pack = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase()
                        + "/cm:WORKSPACE/cm:" + packName, Constants.ASSOC_CONTAINS, null);
            ParentReference doc = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase()
                        + "/cm:WORKSPACE/cm:" + packName + "/cm:" + fileName, Constants.ASSOC_CONTAINS, null);

            deleteSpace(doc);
            createDocument(pack, fileName, file, null, "application/xml", "", signerEnabled, "", "");
        }
        catch (Exception e)
        {
            logger.error("loadVCDPackage failed", e);
            throw new Exception(e);
        }
        logger.debug("loadVCDPackage successfully");
    }

    /**
     * Load a XML file into VCD.
     */
    public void loadVCD( String userName , String packName , String vcdName , String docRef , String fileName ) throws Exception
    {

        logger.info("AlfrescoManager::loadVCD()");
        try
        {
            packName = packName.replaceAll(" ", "_");
            byte[] file = docRef.getBytes("UTF-8");
            ParentReference vcd = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase()
                        + "/cm:WORKSPACE/cm:" + packName + "/cm:" + vcdName, Constants.ASSOC_CONTAINS, null);
            ParentReference doc = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase()
                        + "/cm:WORKSPACE/cm:" + packName + "/cm:" + vcdName + "/cm:" + fileName, Constants.ASSOC_CONTAINS, null);
            try
            {
                deleteSpace(doc);
            }
            catch (Exception e)
            {
                logger.error("File to be deleted not exists");
            }
            createDocument(vcd, fileName, file, null, "application/xml", "", "", "", "");
        }
        catch (Exception e)
        {
            logger.error("loadVCD failed");
            throw new Exception(e);
        }
        logger.debug("loadVCD successfully");
    }

    /**
     * Adds record to VCD.
     */
    public byte[] addRecordToVCD( String userName , String packName , String vcdName , String fileName ) throws Exception
    {

        logger.info("AlfrescoManager::addRecordToVCD()");
        byte[] xmlFile = null;
        try
        {
            String xmlName = fileName + ".xml";
            ParentReference xmlSafe = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase()
                        + "/cm:SAFE/cm:" + xmlName, Constants.ASSOC_CONTAINS, null);
            xmlFile = getContent(xmlSafe);
        }
        catch (Exception e)
        {
            logger.error("addRecordToVCD failed");
            throw new Exception(e);
        }
        logger.debug("addRecordToVCD successfully");
        return xmlFile;
    }

    /**
     * Move a space.
     */
    public void moveSpace( Reference space , Reference dest , String newname ) throws Exception
    {

        logger.info("AlfrescoManager::moveSpace()");
        ParentReference parentDest = ReferenceToParent(dest);
        parentDest.setChildName(Constants.createQNameString(Constants.NAMESPACE_CONTENT_MODEL, normilizeNodeName(newname)));
        CMLMove move = new CMLMove();
        move.setTo(parentDest);
        move.setWhere(new Predicate(new Reference[]
        {
            space
        }, STORE, null));
        NamedValue[] properties = new NamedValue[]
        {
            org.alfresco.webservice.util.Utils.createNamedValue(Constants.PROP_NAME, newname)
        };
        CMLUpdate update = new CMLUpdate();
        update.setProperty(properties);
        update.setWhere(new Predicate(new Reference[]
        {
            space
        }, STORE, null));

        CML cml = new CML();
        cml.setMove(new CMLMove[]
        {
            move
        });
        cml.setUpdate(new CMLUpdate[]
        {
            update
        });

        // Execute the CML move and Update statement
        try
        {
            logger.debug("Moving the space with path " + space.getPath() + " or id " + space.getUuid() + "\n" + "to destination space with path "
                        + dest.getPath() + " or id " + dest.getUuid() + "\n" + "by using the name " + newname);
            WebServiceFactory.getRepositoryService().update(cml);

        }
        catch (Exception e)
        {
            logger.error("moveSpace failed", e);
            throw new Exception(e);
        }
        logger.debug("moveSpace successfully");
    }

    /**
     * Rename a space.
     * 
     * @throws Exception
     */
    public void renameSpace( Reference parent , Reference space , String newName ) throws Exception
    {

        logger.info("AlfrescoManager::renameSpace()");
        moveSpace(space, parent, newName);
        logger.debug("renameSpace successfully");
    }

    /**
     * Close a VCDPackage.
     */
    // public void closeVCDPack( String userName , String packName , String
    // description ) throws Exception
    // {
    //
    // logger.info("AlfrescoManager::closeVCDPack");
    // String nameXML = null;
    //
    // String path = "\"/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" +
    // userName.toUpperCase() + "/cm:WORKSPACE/cm:" + packName + "/*\"";
    // try
    // {
    //
    // String infoCA = null;
    //
    // ResultSetRow[] results = queryLucene(path, "");
    // for (ResultSetRow resultRow : results)
    // {
    // NamedValue[] properties = resultRow.getColumns();
    // for (NamedValue array : properties)
    // {
    // if
    // (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}name")
    // && (array.getValue().contains("xml")))
    // {
    // nameXML = array.getValue();
    // // String nameZip = nameXML.replace(".xml",
    // // "").replace("Package", "Container");
    // String nameZip = "VCDContainer_" + UUID.randomUUID();
    // logger.debug("Name zip: " + nameZip);
    // ParentReference parent = new ParentReference(STORE, null,
    // "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase()
    // + "/cm:WORKSPACE", Constants.ASSOC_CONTAINS, null);
    // ParentReference pack = new ParentReference(STORE, null,
    // "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase()
    // + "/cm:WORKSPACE/cm:" + normilizeNodeName(packName),
    // Constants.ASSOC_CONTAINS, null);
    // if (!packName.equalsIgnoreCase(nameZip))
    // {
    // // Rename the space with the name of the xml file
    // renameSpace(parent, pack, nameZip);
    // }
    // ParentReference newPack = new ParentReference(STORE, null,
    // "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase()
    // + "/cm:WORKSPACE/cm:" + nameZip, Constants.ASSOC_CONTAINS, null);
    //
    // ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //
    // ZipOutputStream zip = new ZipOutputStream(baos);
    //
    // String initialPath = "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" +
    // userName.toUpperCase() + "/cm:WORKSPACE/cm:" + nameZip + "/cm:";
    //
    // for (String str : getContentSpace(newPack, nameZip, null, userName))
    // {
    // if (Utils.getFileExtension(str).trim().equals(""))
    // {
    // continue;
    // }
    // Reference r = new Reference(STORE, null, initialPath + str);
    // byte[] b = getContent(r);
    // String xmlVCDPAck = new String(b, "UTF-8");
    // if (Utils.getFileExtension(str).trim().equalsIgnoreCase(".xml"))
    // {
    // VCDPackageType vcdp = (VCDPackageType)
    // Utils.XMLStringToObjetct(xmlVCDPAck,
    // "it.infocamere.vcdi.vcd.peppol.wp2.schema.xsd.virtualcompanydossierpackage_1");
    // infoCA =
    // vcdp.getContractingAuthorityParty().getEndpointID().getValue().split(":")[1];
    // }
    // ZipEntry ze = new ZipEntry(str);
    // ze.setSize(b.length);
    // zip.putNextEntry(ze);
    // zip.write(b);
    // zip.closeEntry();
    // }
    //
    // for (String str : getFolderSpace(newPack, nameZip, userName))
    // {
    // logger.debug("-->" + initialPath + str);
    // logger.debug("-->" + initialPath + str.replace(File.separator, "/cm:"));
    // // file.separator
    // Reference r = new Reference(STORE, null, initialPath +
    // str.replace(File.separator, "/cm:"));
    // byte[] b = getContent(r);
    //
    // // bypass alfresco filenaming limitation
    // if (str.contains(VCDICostants.SAFE_PREFIX))
    // {
    // str = str.replace(VCDICostants.SAFE_PREFIX, "");
    // }
    //
    // // bypass alfresco filenaming limitation
    // if (str.startsWith("VCD_") &&
    // Utils.getFileExtension(str).equalsIgnoreCase(".XML"))
    // {
    // String tmpXML = Utils.transformByteIntoXml(b);
    // b = null;
    // tmpXML = tmpXML.replaceAll("<cbc:FileName>" + VCDICostants.SAFE_PREFIX,
    // "<cbc:FileName>");
    // b = tmpXML.getBytes("UTF-8");
    // }
    //
    // ZipEntry ze = new ZipEntry(str);
    // ze.setSize(b.length);
    // zip.putNextEntry(ze);
    // zip.write(b);
    // zip.closeEntry();
    // }
    // zip.close();
    // ParentReference store = new ParentReference(STORE, null,
    // "/app:company_home/cm:VCDI_PEPPOL/cm:STORE", Constants.ASSOC_CONTAINS,
    // null);
    //
    // createDocumentStore(store, nameZip + ".zip", baos.toByteArray(),
    // "application/zip", description, infoCA);
    //
    // deleteSpace(newPack);
    // }
    // }
    // }
    // }
    // catch (Exception e)
    // {
    // logger.error("closeVCDPack failed", e);
    // throw new Exception(e);
    // }
    // logger.debug("closeVCDPack successfully");
    // }

    /**
     * Gets the list of documents existing in a space.
     */
    public List<String> getContentSpace( Reference space , String packName , String prefix , String userName ) throws Exception
    {

        logger.info("AlfrescoManager::getContentSpace()");
        List<String> listContent = new LinkedList<String>();
        String path = "\"/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase() + "/cm:WORKSPACE/cm:" + packName + "/*\"";
        try
        {
            ResultSetRow[] results = queryLucene(path, "AND TYPE:\"cm:content\"");
            for (ResultSetRow resultRow : results)
            {
                NamedValue[] properties = resultRow.getColumns();
                for (NamedValue array : properties)
                {
                    if (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}name"))
                    {
                        if (prefix != null && !prefix.equals(""))
                        {
                            listContent.add(prefix + array.getValue().replace(" ", ""));
                        }
                        else
                        {
                            listContent.add(array.getValue().replace(" ", ""));
                        }

                        logger.debug(array.getValue());
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("getContentSpace failed", e);
            throw new Exception(e);
        }
        logger.debug("getContentSpace successfully");
        return listContent;
    }

    /**
     * Gets the list of folders existing in a space.
     */
    public List<String> getFolderSpace( Reference space , String packName , String userName ) throws Exception
    {

        logger.info("AlfrescoManager::getFolderSpace()");
        List<String> listFolder = new LinkedList<String>();
        String path = "\"/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase() + "/cm:WORKSPACE/cm:" + packName + "/*\"";
        try
        {
            ResultSetRow[] results = queryLucene(path, "AND TYPE:\"cm:folder\"");
            for (ResultSetRow resultRow : results)
            {
                NamedValue[] properties = resultRow.getColumns();
                for (NamedValue array : properties)
                {
                    if (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}name"))
                    {
                        logger.debug(array.getValue());
                        ParentReference folder = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase()
                                    + "/cm:WORKSPACE/cm:" + packName + "/cm:" + array.getValue(), Constants.ASSOC_CONTAINS, null);
                        listFolder.addAll(getContentSpace(folder, packName + "/cm:" + array.getValue(), array.getValue() + File.separator, userName));
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("getFolderSpace failed");
            throw new Exception(e);
        }
        logger.debug("getFolderSpace successfully");
        return listFolder;
    }

    /**
     * Gets the XML file of a VCD.
     */
    public byte[] getVCDXML( String packName , String vcdName , String userName ) throws Exception
    {

        logger.info("AlfrescoManager::getVCDXML()");
        byte[] fileXML = null;
        String nameFile = null;
        packName = packName.replaceAll(" ", "_");
        String path = "\"/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase() + "/cm:WORKSPACE/cm:" + packName + "/cm:" + vcdName + "/*\"";

        ResultSetRow[] results = queryLucene(path, "AND TYPE:\"cm:content\"");
        boolean found = false;
        for (ResultSetRow resultRow : results)
        {
            NamedValue[] properties = resultRow.getColumns();
            for (NamedValue array : properties)
            {
                logger.info(array.getName());
                if (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}name") && (array.getValue().contains("xml")))
                {
                    nameFile = array.getValue();
                    logger.debug("Name file: " + array.getValue());
                    ParentReference node = new ParentReference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase()
                                + "/cm:WORKSPACE/cm:" + packName + "/cm:" + vcdName + "/cm:" + nameFile, Constants.ASSOC_CONTAINS, null);
                    try
                    {
                        fileXML = getContent(node);
                        found = true;
                        break;
                    }
                    catch (Exception e)
                    {
                        logger.error("getVCDXML failed");
                        throw new Exception(e);
                    }
                }
            }
            if (found)
                break;
        }
        logger.debug("getVCDXML successfully");
        return fileXML;
    }

    /**
     * Gets the list of users belong to a group.
     */
    // public List<String> getUserOfGroup( String groupName ) throws Exception
    // {
    //
    // logger.info("AlfrescoManager::getUserOfGroup()");
    // Messages m = new Messages(Utils.getAmbiente());
    // Authenticator.setDefault(new
    // HttpAuthenticator(m.getString("alfresco.server.user"),
    // m.getString("alfresco.server.password")));
    //
    // List<String> ret = null;
    // try
    // {
    // URL url = new URL(m.getString("alfresco.rest") + "groups/" + groupName +
    // "/children");
    //
    // BufferedReader in = new BufferedReader(new
    // InputStreamReader(url.openStream()));
    // String str;
    // String app = "";
    // while ((str = in.readLine()) != null)
    // {
    // app += str;
    // }
    //
    // in.close();
    //
    // logger.debug("JSON RESTPONSE:" + app);
    //
    // JSONArray arr = new JSONArray(app.replace("{    \"data\": ", ""));
    // ret = new ArrayList<String>();
    //
    // for (int i = 0; i < arr.length(); i++)
    // {
    // JSONObject js = new JSONObject(arr.get(i).toString());
    // ret.add((String) js.get("shortName"));
    // }
    // }
    // catch (MalformedURLException e)
    // {
    // logger.error("MalformedURLException");
    // throw new Exception(e);
    // }
    // catch (IOException e)
    // {
    // logger.error("IOException");
    // throw new Exception(e);
    // }
    // catch (JSONException e)
    // {
    // logger.error("JSONException");
    // throw new Exception(e);
    // }
    // logger.debug("getUserOfGroup successfully");
    // return ret;
    // }

    // /**
    // * Gets the first name of a user
    // */
    // public Map<String, String> getUsersFirstNames() throws Exception
    // {
    //
    // logger.info("AlfrescoManager::getUserFirstName");
    // Messages m = new Messages(Utils.getAmbiente());
    // Authenticator.setDefault(new
    // HttpAuthenticator(m.getString("alfresco.server.user"),
    // m.getString("alfresco.server.password")));
    //
    // Map<String, String> ret = new HashMap<String, String>();
    // try
    // {
    // // URL url = new URL(m.getString("alfresco.rest") + "people/" +
    // // userName);
    // URL url = new URL(m.getString("alfresco.rest") + "people?filter=*");
    //
    // BufferedReader in = new BufferedReader(new
    // InputStreamReader(url.openStream()));
    // String str;
    // String app = "";
    // while ((str = in.readLine()) != null)
    // {
    // app += str;
    // }
    //
    // in.close();
    //
    // logger.debug("JSON RESTPONSE:" + app);
    //
    // JSONObject obj = new JSONObject(app);
    //
    // // ret = obj.getString("firstName");
    // JSONArray jsonArray = obj.getJSONArray("people");
    // for (int i = 0; i < jsonArray.length(); ++i)
    // {
    // ret.put(jsonArray.getJSONObject(i).getString("userName"),
    // jsonArray.getJSONObject(i).getString("firstName"));
    // }
    //
    // }
    // catch (MalformedURLException e)
    // {
    // logger.error("MalformedURLException");
    // throw new Exception(e);
    // }
    // catch (IOException e)
    // {
    // logger.error("IOException");
    // throw new Exception(e);
    // }
    // catch (JSONException e)
    // {
    // logger.error("JSONException");
    // throw new Exception(e);
    // }
    // logger.debug("getUserOfGroup successfully");
    // return ret;
    // }

    // /**
    // * Gets the list of names of zip file from Store.
    // */
    // public List<Container> getListStore()
    // {
    //
    // logger.info("AlfrescoManager::getListStore()");
    // List<Container> zipList = new LinkedList<Container>();
    // String path = "\"/app:company_home/cm:VCDI_PEPPOL/cm:STORE/*\"";
    //
    // ResultSetRow[] results = queryLucene(path, " AND TYPE:\"cm:content\"");
    //
    // if (results == null)
    // {
    // logger.debug("Store is empty");
    // return zipList;
    // }
    //
    // for (ResultSetRow resultRow : results)
    // {
    // NamedValue[] properties = resultRow.getColumns();
    // Container dati = new Container();
    // for (NamedValue array : properties)
    // {
    //
    // if
    // (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}name"))
    // {
    // dati.setName(array.getValue());
    // logger.debug("Zip name: " + array.getValue());
    // }
    // if
    // (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}description"))
    // {
    // dati.setDescription(array.getValue());
    // logger.debug("Zip description: " + array.getValue());
    // }
    // if (array.getName().equalsIgnoreCase("{custom.model}PartyID"))
    // {
    // dati.setPartyID(array.getValue());
    // logger.debug("Zip PartyID: " + array.getValue());
    // }
    //
    // }
    // zipList.add(dati);
    // }
    // logger.debug("getListStore successfully");
    // return zipList;
    // }

    /**
     * Gets a zip file from Store.
     */
    public byte[] getContainerStore( String zipName ) throws Exception
    {

        logger.info("AlfrescoManager::getContainerStore()");
        byte[] zipFile = null;
        String path = "\"/app:company_home/cm:VCDI_PEPPOL/cm:STORE/*\"";
        boolean found = false;
        try
        {
            ResultSetRow[] results = queryLucene(path, "AND TYPE:\"cm:content\"");
            for (ResultSetRow resultRow : results)
            {
                NamedValue[] properties = resultRow.getColumns();
                for (NamedValue array : properties)
                {
                    if (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}name") && (array.getValue().equalsIgnoreCase(zipName)))
                    {
                        Reference node = new Reference(STORE, null, "/app:company_home/cm:VCDI_PEPPOL/cm:STORE/cm:" + zipName);
                        zipFile = getContent(node);
                        found = true;
                        break;
                    }
                }
                if (found)
                    break;
            }
        }
        catch (Exception e)
        {
            logger.error("getContainerStore failed");
            throw new Exception(e);
        }
        logger.debug("getContainerStore successfully");
        return zipFile;
    }

    /**
     * Gets the list of properties of a file.
     */
    public List<String> searchPropertiesIntoFile( String path , String valueProperty ) throws Exception
    {

        logger.info("AlfrescoManager::searchPropertiesIntoFile()");
        List<String> listFile = new LinkedList<String>();
        try
        {
            ResultSetRow[] results = queryLucene(path, " AND (@cm\\:name:\"" + valueProperty + "\"" + " OR @cm\\:creator:\"" + valueProperty + "\""
                        + " OR @cm\\:description:\"" + valueProperty + "\"" + " OR @cm\\:title:\"" + valueProperty + "\"" + " OR @cm\\:author:\""
                        + valueProperty + "\"" + " OR @custom\\:ExpiryDate:\"" + valueProperty + "\"" + " OR @custom\\:SignEnabled:\"" + valueProperty + "\""
                        + " OR @custom\\:StateOfSign:\"" + valueProperty + "\"" + " OR @custom\\:Attachment:\"" + valueProperty + "\")");
            String name = null;
            if (results == null)
            {
                return listFile;
            }
            for (ResultSetRow resultRow : results)
            {
                NamedValue[] properties = resultRow.getColumns();
                for (NamedValue array : properties)
                {
                    if (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}name"))
                    {
                        name = array.getValue();
                        logger.debug(name);
                        listFile.add(name);
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("searchPropertiesIntoFile failed");
            throw new Exception(e);
        }
        logger.debug("searchPropertiesIntoFile successfully");
        return listFile;
    }

    // public List<Container> searchPropertiesIntoZip( String path , String
    // property ) throws Exception
    // {
    //
    // logger.info("AlfrescoManager::searchPropertiesIntoZip()");
    // List<Container> listContainer = new LinkedList<Container>();
    // if (property == null || "".equals(property))
    // {
    // property = "";
    // }
    // else
    // {
    // property += "*";
    // }
    // try
    // {
    // ResultSetRow[] results = queryLucene(path, " AND ((@custom\\:PartyID:\""
    // + property + "\") OR (@description:\"" + property + "\"))");
    // if (results == null)
    // {
    // return listContainer;
    // }
    // String name = null;
    // for (ResultSetRow resultRow : results)
    // {
    // NamedValue[] properties = resultRow.getColumns();
    // Container dati = new Container();
    // for (NamedValue array : properties)
    // {
    //
    // if
    // (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}name"))
    // {
    // dati.setName(array.getValue());
    // logger.debug("Zip name: " + array.getValue());
    // }
    // if
    // (array.getName().equalsIgnoreCase("{http://www.alfresco.org/model/content/1.0}description"))
    // {
    // dati.setDescription(array.getValue());
    // logger.debug("Zip description: " + array.getValue());
    // }
    // if (array.getName().equalsIgnoreCase("{custom.model}PartyID"))
    // {
    // dati.setPartyID(array.getValue());
    // logger.debug("Zip PartyID: " + array.getValue());
    // }
    //
    // }
    // listContainer.add(dati);
    // }
    // }
    // catch (Exception e)
    // {
    // logger.error("searchPropertiesIntoZip failed");
    // throw new Exception(e);
    // }
    // logger.debug("searchPropertiesIntoZip successfully");
    // return listContainer;
    // }

    /**
     * Search a property into Store.
     */
    // public List<Container> searchPropertiesIntoStore( String property )
    // throws Exception
    // {
    //
    // logger.info("AlfrescoManager::searchPropertiesIntoStore()");
    // String path = "\"/app:company_home/cm:VCDI_PEPPOL/cm:STORE/*\"";
    // List<Container> listContainer = searchPropertiesIntoZip(path, property);
    // logger.debug("searchPropertiesIntoStore successfully");
    // return listContainer;
    // }

    /**
     * Search a property into Safe.
     */
    public List<String> searchPropertiesIntoSafe( String userName , String valueProperty ) throws Exception
    {

        logger.info("AlfrescoManager::searchPropertiesIntoSafe()");
        String path = "\"/app:company_home/cm:VCDI_PEPPOL/cm:HOMEPAGE_" + userName.toUpperCase() + "/cm:SAFE/*\"";
        List<String> listDoc = searchPropertiesIntoFile(path, valueProperty);
        logger.debug("searchPropertiesIntoSafe successfully");
        return listDoc;
    }

    // private String searchREST( HttpClient httpclient , String query ) throws
    // IOException , JSONException
    // {
    //
    // Messages m = new Messages(Utils.getAmbiente());
    // String url = "http://" + m.getString("alfresco.host") + ":" +
    // m.getString("alfresco.port") + "/alfresco";
    //
    // GetMethod getMethod = new GetMethod(url + SERVICE_EP_LUCENE_SEARCH_PATH +
    // URLEncoder.encode(query, "UTF-8"));
    // int rc = httpclient.executeMethod(getMethod);
    // logger.debug("rc:" + rc + ":" + getMethod.getResponseBodyAsString());
    // return (String) new
    // JSONArray(getMethod.getResponseBodyAsString()).get(0);
    // }
    //
    // public void taskDoneByTaskId( HttpClient httpclient , String taskId )
    // throws Exception
    // {
    //
    // Messages m = new Messages(Utils.getAmbiente());
    // String url = m.getString("alfresco.rest") + "workflow/task/end/" +
    // URLEncoder.encode(taskId, "UTF-8");
    // PostMethod httppost = new PostMethod(url);
    // httpclient.executeMethod(httppost);
    // String out =
    // org.apache.commons.io.IOUtils.toString(httppost.getResponseBodyAsStream());
    // }
    //
    // public String descripTask( HttpClient httpclient , String taskid ) throws
    // Exception
    // {
    //
    // String ris = "";// new ArrayList<String>();
    // Messages m = new Messages(Utils.getAmbiente());
    // String url = m.getString("alfresco.rest") + "task-instances/" +
    // URLEncoder.encode(taskid, "UTF-8");
    // // System.out.println("descripTask=" + url);
    // GetMethod method = new GetMethod(url);
    // httpclient.executeMethod(method);
    // String out =
    // org.apache.commons.io.IOUtils.toString(method.getResponseBodyAsStream());
    // JSONObject jSONArray = new JSONObject(out).getJSONObject("data");
    // ris = jSONArray.getJSONObject("properties").getString("bpm_description");
    // return ris;
    // }
    //
    // public List<TaskInfo> getTasksByUsername( HttpClient httpclient , String
    // username ) throws Exception
    // {
    //
    // List<String> ris = new ArrayList<String>();
    // List<TaskInfo> ret = new ArrayList<TaskInfo>();
    // Messages m = new Messages(Utils.getAmbiente());
    // String url = m.getString("alfresco.rest") + "task-instances?authority=" +
    // username;
    // GetMethod method = new GetMethod(url);
    // httpclient.executeMethod(method);
    // String out =
    // org.apache.commons.io.IOUtils.toString(method.getResponseBodyAsStream());
    // JSONArray jSONArray = new JSONObject(out).getJSONArray("data");
    // List<String> ris2 = new ArrayList<String>();
    //
    // for (int i = 0; i < jSONArray.length(); ++i)
    // {
    // ris.add(jSONArray.getJSONObject(i).getString("id"));
    // ris2.add(jSONArray.getJSONObject(i).getJSONObject("properties").getString("bpm_context"));
    // }
    // /*
    // * List<String> ris2 = getWfInstancesByDefinitionName(httpclient,
    // * "jbpm$buildVCD");
    // */
    // for (int i = 0; i < ris2.size(); i++)
    // {
    // TaskInfo ti = new TaskInfo();
    // ti.setId(ris.get(i));
    // ti.setDescription(descripTask(httpclient, ris.get(i)));
    // ti.setSpaceName(searchREST(httpclient, "ID:\"" + ris2.get(i) + "\""));
    // if (ret.size() > 0 && ti.getSpaceName().equals(ret.get(ret.size() -
    // 1).getSpaceName()))
    // {
    // continue;
    // }
    // ret.add(ti);
    // }
    // return ret;
    // }
    //
    // public List<String> getWfInstancesByDefinitionName( HttpClient httpclient
    // , String definitionName ) throws Exception
    // {
    //
    // List<String> ris = new ArrayList<String>();
    // Messages m = new Messages(Utils.getAmbiente());
    // String url = m.getString("alfresco.rest") +
    // "workflow-instances?definitionName=" + URLEncoder.encode(definitionName,
    // "UTF-8");
    // GetMethod method = new GetMethod(url);
    // httpclient.executeMethod(method);
    // String out =
    // org.apache.commons.io.IOUtils.toString(method.getResponseBodyAsStream());
    // JSONArray jSONArray = new JSONObject(out).getJSONArray("data");
    // for (int i = 0; i < jSONArray.length(); ++i)
    // {
    // ris.add(jSONArray.getJSONObject(i).getString("context"));
    // }
    // return ris;
    // }
    //
    // public String login( HttpClient httpclient ) throws IOException ,
    // JSONException , UnsupportedEncodingException
    // {
    //
    // Messages m = new Messages(Utils.getAmbiente());
    // PostMethod httppost = new PostMethod(m.getString("alfresco.rest") +
    // "login");
    // httppost.setRequestHeader("Content-Type", "application/json");
    // httppost.setRequestEntity(new StringRequestEntity("{ \"username\" : \"" +
    // m.getString("alfresco.server.user") + "\", \"password\" : \""
    // + m.getString("alfresco.server.password") + "\" }", "application/json",
    // null));
    // logger.debug(httpclient.executeMethod(httppost));
    // String out =
    // org.apache.commons.io.IOUtils.toString(httppost.getResponseBodyAsStream());
    // JSONObject jSONObject = new JSONObject(out);
    // return jSONObject.getJSONObject("data").getString("ticket");
    // }
    //
    // public void logout( HttpClient httpclient , String ticket ) throws
    // IOException , JSONException , UnsupportedEncodingException
    // {
    //
    // Messages m = new Messages(Utils.getAmbiente());
    // DeleteMethod delete = new DeleteMethod(m.getString("alfresco.rest") +
    // "login/ticket/" + ticket);
    // httpclient.executeMethod(delete);
    // // String out =
    // //
    // org.apache.commons.io.IOUtils.toString(delete.getResponseBodyAsStream());
    // }
    //
    // public HttpClient getAlfrescoHttpRestClient()
    // {
    //
    // Messages m = new Messages(Utils.getAmbiente());
    // org.apache.commons.httpclient.HttpClient client = new
    // org.apache.commons.httpclient.HttpClient();
    // client.getParams().setAuthenticationPreemptive(true);
    // Credentials defaultcreds = new
    // UsernamePasswordCredentials(m.getString("alfresco.server.user"),
    // m.getString("alfresco.server.password"));
    // client.getState().setCredentials(
    // new AuthScope(m.getString("alfresco.host"),
    // Integer.parseInt(m.getString("alfresco.port").replaceAll(":", "")),
    // AuthScope.ANY_REALM),
    // defaultcreds);
    // return client;
    // }

    /**
     * Creates a node into a space that represent a document.
     */
    protected Reference createDocumentStore( Reference parentref , String docname , byte[] content , String mimetype , String description , String partyID )
                throws Exception
    {

        logger.info("AlfrescoManager::createDocument()");
        Reference document = null;
        ParentReference parent = ReferenceToParent(parentref);
        parent.setChildName(Constants.createQNameString(Constants.NAMESPACE_CONTENT_MODEL, normilizeNodeName(docname)));

        NamedValue[] properties = buildCustomPropertiesStore(docname, description, partyID);

        CMLCreate create = new CMLCreate("1", parent, null, null, null, Constants.TYPE_CONTENT, properties);
        CML cml = new CML();
        cml.setCreate(new CMLCreate[]
        {
            create
        });

        // Execute the CML create statement
        UpdateResult[] results = null;
        try
        {
            logger.debug("Creating the document " + docname);
            results = WebServiceFactory.getRepositoryService().update(cml);
            document = results[0].getDestination();
        }
        catch (Exception e)
        {
            logger.error("Can not create the document", e);
            throw new Exception(e);
        }
        // Set the content
        ContentFormat format = new ContentFormat(mimetype, "UTF-8");
        try
        {
            logger.debug("Setting the content of the document");
            WebServiceFactory.getContentService().write(document, Constants.PROP_CONTENT, content, format);
        }
        catch (Exception e2)
        {
            logger.error("Can not set the content of the document", e2);
            throw new Exception(e2);
        }
        logger.debug("createDocument successfully");
        return document;
    }

    private static NamedValue[] buildCustomPropertiesStore( String name , String description , String partyID )
    {

        logger.info("AlfrescoManager::buildCustomPropertiesStore()");
        NamedValue[] properties = new NamedValue[3];
        properties[0] = org.alfresco.webservice.util.Utils.createNamedValue(Constants.PROP_NAME, name);
        properties[1] = org.alfresco.webservice.util.Utils.createNamedValue(Constants.PROP_DESCRIPTION, description);
        properties[2] = org.alfresco.webservice.util.Utils.createNamedValue("{custom.model}PartyID", partyID);
        logger.debug("buildCustomProperties for Store successfully");
        return properties;
    }
}
