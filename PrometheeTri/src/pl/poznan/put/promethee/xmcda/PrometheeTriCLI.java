package pl.poznan.put.promethee.xmcda;

import pl.poznan.put.promethee.PrometheeTri;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Maciej Uniejewski on 2016-11-11.
 */
public class PrometheeTriCLI {
    public static void main(String[] args) throws Exception
    {
        final ArrayList<String> argsList = new ArrayList<String>(Arrays.asList(args));
        if ( argsList.remove("--v2") )
        {
            System.err.println("Version 2 is still not supported");
            System.exit(-1);
        }
        else if ( argsList.remove("--v3") )
        {
            PrometheeTriCLI.main((String[]) argsList.toArray(new String[]{}));
        }
        else
        {
            System.err.println("missing mandatory option --v2 or --v3");
            System.exit(-1);
        }
    }
}
