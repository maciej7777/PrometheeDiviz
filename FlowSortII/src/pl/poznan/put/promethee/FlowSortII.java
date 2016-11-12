package pl.poznan.put.promethee;

import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-11-12.
 */
public class FlowSortII {

    public static OutputsHandler.Output sortWithCentralProfiles(InputsHandler.Inputs inputs) {

        Map<String, String> assignments = new LinkedHashMap<>();

        for (int altI = 0 ; altI < inputs.alternativesIds.size(); altI++) {
            assignments.put(inputs.alternativesIds.get(altI), inputs.categoryProfiles.get(0).getCategory().id());

            for (int catProfI = 1; catProfI < inputs.categoryProfiles.size(); catProfI++) {
                if (inputs.flows.get(inputs.alternativesIds.get(altI)).doubleValue() >
                        (inputs.flows.get(inputs.categoryProfiles.get(catProfI).getCentralProfile().getAlternative().id()).doubleValue() +
                                inputs.flows.get(inputs.categoryProfiles.get(catProfI-1).getCentralProfile().getAlternative().id()).doubleValue())/2) {
                    assignments.put(inputs.alternativesIds.get(altI), inputs.categoryProfiles.get(catProfI).getCategory().id());
                } else {
                    break;
                }
            }
        }


        OutputsHandler.Output output = new OutputsHandler.Output();
        output.assignments = assignments;
        return output;
    }

    public static OutputsHandler.Output sortWithBoundaryProfiles(InputsHandler.Inputs inputs) {

        Map<String, String> assignments = new LinkedHashMap<>();

        for (int altI = 0 ; altI < inputs.alternativesIds.size(); altI++) {
            assignments.put(inputs.alternativesIds.get(altI), inputs.categoryProfiles.get(0).getCategory().id());

            for (int catProfI = 0; catProfI < inputs.categoryProfiles.size()-1; catProfI++) {
                if (inputs.flows.get(inputs.alternativesIds.get(altI)).doubleValue() >=
                        inputs.flows.get(inputs.categoryProfiles.get(catProfI).getUpperBound().getAlternative().id()).doubleValue()) {
                    assignments.put(inputs.alternativesIds.get(altI), inputs.categoryProfiles.get(catProfI+1).getCategory().id());
                } else {
                    break;
                }
            }
        }

        OutputsHandler.Output output = new OutputsHandler.Output();
        output.assignments = assignments;
        return output;
    }


    public static OutputsHandler.Output sort(InputsHandler.Inputs inputs) {

        if (inputs.profilesType.toString().toUpperCase().equals("CENTRAL")) {
            return sortWithCentralProfiles(inputs);
        } else if (inputs.profilesType.toString().toUpperCase().equals("BOUNDING")) {
            return sortWithBoundaryProfiles(inputs);
        } else {
            throw new UnsupportedOperationException("Profiles can be only central or bounding.");
        }
    }
}
