
package bigwave.alfresco;

public class User
{

    String  username;

    String  group;

    String  password;

    boolean active;

    String  firstname;

    String  lastname;

    public String getFirstname()
    {

        return firstname;
    }

    public void setFirstname( String firstname )
    {

        this.firstname = firstname;
    }

    public String getLastname()
    {

        return lastname;
    }

    public void setLastname( String lastname )
    {

        this.lastname = lastname;
    }

    public boolean isActive()
    {

        return active;
    }

    public void setActive( boolean active )
    {

        this.active = active;
    }

    public String getUsername()
    {

        return username;
    }

    public void setUsername( String username )
    {

        this.username = username;
    }

    public String getGroup()
    {

        return group;
    }

    public void setGroup( String group )
    {

        this.group = group;
    }

    public String getPassword()
    {

        return password;
    }

    public void setPassword( String password )
    {

        this.password = password;
    }

}
