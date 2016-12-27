package pl.poznan.put.promethee;

import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-11-12.
 */
public class FlowSortII {

    private FlowSortII() {

    }

    public static OutputsHandler.Output sortWithCentralProfiles(InputsHandler.Inputs inputs) {

        Map<String, String> assignments = new LinkedHashMap<>();

        for (int altI = 0; altI < inputs.getAlternativesIds().size(); altI++) {
            assignments.put(inputs.getAlternativesIds().get(altI), inputs.getCategoryProfiles().get(0).getCategory().id());

            for (int catProfI = 1; catProfI < inputs.getCategoryProfiles().size(); catProfI++) {
                if (inputs.getFlows().get(inputs.getAlternativesIds().get(altI)) >
                        (inputs.getFlows().get(inputs.getCategoryProfiles().get(catProfI).getCentralProfile().getAlternative().id()) +
                                inputs.getFlows().get(inputs.getCategoryProfiles().get(catProfI-1).getCentralProfile().getAlternative().id()))/2) {
                    assignments.put(inputs.getAlternativesIds().get(altI), inputs.getCategoryProfiles().get(catProfI).getCategory().id());
                } else {
                    break;
                }
            }
        }

        OutputsHandler.Output output = new OutputsHandler.Output();
        output.setAssignments(assignments);
        return output;
    }

    public static OutputsHandler.Output sortWithBoundaryProfiles(InputsHandler.Inputs inputs) {

        Map<String, String> assignments = new LinkedHashMap<>();

        for (int altI = 0; altI < inputs.getAlternativesIds().size(); altI++) {
            assignments.put(inputs.getAlternativesIds().get(altI), inputs.getCategoryProfiles().get(0).getCategory().id());

            for (int catProfI = 0; catProfI < inputs.getCategoryProfiles().size()-1; catProfI++) {
                if (inputs.getFlows().get(inputs.getAlternativesIds().get(altI)) >=
                        inputs.getFlows().get(inputs.getCategoryProfiles().get(catProfI).getUpperBound().getAlternative().id())) {
                    assignments.put(inputs.getAlternativesIds().get(altI), inputs.getCategoryProfiles().get(catProfI+1).getCategory().id());
                } else {
                    break;
                }
            }
        }

        OutputsHandler.Output output = new OutputsHandler.Output();
        output.setAssignments(assignments);
        return output;
    }


    public static OutputsHandler.Output sort(InputsHandler.Inputs inputs) {

        if ("CENTRAL".equalsIgnoreCase(inputs.getProfilesType().toString())) {
            return sortWithCentralProfiles(inputs);
        } else if ("BOUNDING".equalsIgnoreCase(inputs.getProfilesType().toString())) {
            return sortWithBoundaryProfiles(inputs);
        } else {
            throw new UnsupportedOperationException("Profiles can be only central or bounding.");
        }
    }
}
