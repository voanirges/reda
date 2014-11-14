
package bigwave.alfresco;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class SqlConnector
{

    public static ArrayList<User> getUsers() throws SQLException , IOException , ClassNotFoundException
    {

        HashMap<String, String> props = GetProperties.getPropValues();
        ArrayList<User> isers = new ArrayList<User>();
        // String query = "select a.sName as UserName, b.sName as GroupName " +
        // "from Alfresco_Users_s a, Alfresco_Groups_s b, Alfresco_GroupsUsers_s c "
        // + "where a.iUserAppId = c.iUserAppId" +
        // "and b.iRightGroupId = c.iRightGroupId" +
        // "order by a.sName, b.sName;";
        String query = props.get(GetProperties.JDBC_USER_QUERY);
        String password = props.get(GetProperties.ALFRESCO_USER_PASS);
        Connection conn;
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        conn = DriverManager.getConnection(props.get(GetProperties.JDBC_URL), props.get("user"), props.get("pass"));

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next())
        {
            String name = rs.getString(1);
            String email = rs.getString(2);
            boolean active = rs.getBoolean(3);
            String firstname = rs.getString(4);
            String lastname = rs.getString(5);

            User usr = new User();
            usr.setEmail(email);
            usr.setUsername(normalizeUserName(name));
            usr.setActive(active);
            usr.setFirstname(normalizeUserName(firstname));
            usr.setLastname(normalizeUserName(lastname));
            
            //usr.setPassword(password);
            usr.setPassword(usr.getUsername());

            isers.add(usr);

        }

        return isers;

    }

    public static ArrayList<String> getGroups() throws SQLException , IOException , ClassNotFoundException
    {

        HashMap<String, String> props = GetProperties.getPropValues();
        ArrayList<String> isers = new ArrayList<String>();
        // String query = "select a.sName as UserName, b.sName as GroupName " +
        // "from Alfresco_Users_s a, Alfresco_Groups_s b, Alfresco_GroupsUsers_s c "
        // + "where a.iUserAppId = c.iUserAppId" +
        // "and b.iRightGroupId = c.iRightGroupId" +
        // "order by a.sName, b.sName;";
        String query = props.get(GetProperties.JDBC_GROUP_QUERY);
        Connection conn;
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        conn = DriverManager.getConnection(props.get(GetProperties.JDBC_URL), props.get("user"), props.get("pass"));

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next())
        {
            String name = rs.getString(1);

            isers.add(normalizeGroupName(name));

        }

        return isers;

    }

    public static ArrayList<String> getGroupsOfUser( String user ) throws IOException , ClassNotFoundException
    {

        HashMap<String, String> props = GetProperties.getPropValues();
        ArrayList<String> isers = new ArrayList<String>();
        String query = "select  b.sName as GroupName " + "from Alfresco_Users_s a, Alfresco_Groups_s b, Alfresco_GroupsUsers_s c "
                    + "where a.iUserAppId = c.iUserAppId" + " and b.iRightGroupId = c.iRightGroupId" + " and a.sName='" + user + "'";
        // String query = props.get(GetProperties.JDBC_GROUP_QUERY);
        Connection conn;
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        try
        {
            conn = DriverManager.getConnection(props.get(GetProperties.JDBC_URL), props.get("user"), props.get("pass"));
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next())
            {
                String name = rs.getString(1);

                isers.add(normalizeGroupName(name));

            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return isers;

    }
    
    private static String normalizeGroupName( String dGroup )
    {
    	//backslash character is the escape character or its a path separator on windows. 
    	//Forward slash is the path separator on pretty much everything else
    	//colon is used to separate the namespace from the name.
        //dGroup = dGroup.replace(":", "_");
        //dGroup = dGroup.replace("/", "_");
        //dGroup = dGroup.replace("\\", "_");
        dGroup = dGroup.replace(" ", "_");
        dGroup = dGroup.replaceAll("\\W", "_");
        dGroup = dGroup.replace("___", "_");
        dGroup = dGroup.replace("__", "_");
        //remove any trailing _
        dGroup = dGroup.replaceAll("_$", "");
        return dGroup;
    }
    
    private static String normalizeUserName( String user )
    {
    	//User must be trimmed of spaces
    	//backslash character is the escape character or its a path separator on windows. 
    	//Forward slash is the path separator on pretty much everything else
    	//colon is used to separate the namespace from the name.
        //dGroup = dGroup.replace(":", "_");
        //dGroup = dGroup.replace("/", "_");
        //dGroup = dGroup.replace("\\", "_");
    	user = user.trim();
    	user = user.replace(".", "");
    	//user = user.replaceAll("\\W", "_");
    	//user = user.replace("___", "_");
    	//user = user.replace("__", "_");
        //remove any trailing _
    	//user = user.replaceAll("_$", "");
        return user;
    }

}
