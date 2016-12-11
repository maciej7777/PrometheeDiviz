package pl.poznan.put.promethee;

import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-12-10.
 */
public class FlowSortGDSS {

    public static OutputsHandler.Output sort(InputsHandler.Inputs inputs) {
        Map<String, String> assignmetns = new LinkedHashMap<>();

        OutputsHandler.Output output = new OutputsHandler.Output();
        output.assignments = assignmetns;

        return output;
    }
}
