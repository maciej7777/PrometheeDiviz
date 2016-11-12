package pl.poznan.put.promethee.xmcda;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Maciej Uniejewski on 2016-11-02.
 */
public class FlowSortICLI {
    public static void main(String[] args) throws Exception
    {
        final ArrayList<String> argsList = new ArrayList<String>(Arrays.asList(args));
        if ( argsList.remove("--v2") )
        {
            throw new UnsupportedOperationException("Not supported yet!");
        }
        else if ( argsList.remove("--v3") )
        {
            FlowSortIXMCDAv3.main((String[]) argsList.toArray(new String[]{}));
        }
        else
        {
            System.err.println("missing mandatory option --v2 or --v3");
            System.exit(-1);
        }
    }
}
