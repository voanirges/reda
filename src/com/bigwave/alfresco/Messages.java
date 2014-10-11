
package bigwave.alfresco;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages
{

    private static ResourceBundle _resBundle;

    private String                _bundleName;

    public Messages ( String ambient )
    {

        _bundleName = "com.bigwave." + ambient;
        // _bundleName = propLocation;
        _resBundle = ResourceBundle.getBundle(_bundleName);
    }

    public String getString( String key )
    {

        try
        {
            return _resBundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            return '!' + key + '!';
        }
    }
}
