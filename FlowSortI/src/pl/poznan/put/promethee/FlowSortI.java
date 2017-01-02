package pl.poznan.put.promethee;

import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-11-11.
 */
public class FlowSortI {

    private static final String LOWER = "LOWER";
    private static final String UPPER = "UPPER";

    private FlowSortI() {

    }

    public static OutputsHandler.Output sortWithCentralProfiles(InputsHandler.Inputs inputs) {

        Map<String, Map<String, String>> assignments = new LinkedHashMap<>();

        for (int altI = 0; altI < inputs.getAlternativesIds().size(); altI++) {
            Map<String, String> interval = new LinkedHashMap<>();
            interval.put(LOWER, inputs.getCategoryProfiles().get(0).getCategory().id());
            interval.put(UPPER, inputs.getCategoryProfiles().get(0).getCategory().id());
            assignments.put(inputs.getAlternativesIds().get(altI), interval);

            for (int catProfI = 1; catProfI < inputs.getCategoryProfiles().size() ; catProfI++) {
                if (inputs.getPositiveFlows().get(inputs.getAlternativesIds().get(altI)) >=
                        (inputs.getPositiveFlows().get(inputs.getCategoryProfiles().get(catProfI).getCentralProfile().getAlternative().id()) +
                                inputs.getPositiveFlows().get(inputs.getCategoryProfiles().get(catProfI-1).getCentralProfile().getAlternative().id()))/2) {
                    assignments.get(inputs.getAlternativesIds().get(altI)).put(UPPER, inputs.getCategoryProfiles().get(catProfI).getCategory().id());
                } else {
                    break;
                }
            }

            for (int catProfI = 1; catProfI < inputs.getCategoryProfiles().size() ; catProfI++) {
                if (inputs.getNegativeFlows().get(inputs.getAlternativesIds().get(altI)) <
                        (inputs.getNegativeFlows().get(inputs.getCategoryProfiles().get(catProfI).getCentralProfile().getAlternative().id()) +
                                inputs.getNegativeFlows().get(inputs.getCategoryProfiles().get(catProfI - 1).getCentralProfile().getAlternative().id()))/2) {
                    assignments.get(inputs.getAlternativesIds().get(altI)).put(LOWER, inputs.getCategoryProfiles().get(catProfI).getCategory().id());
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

        Map<String, Map<String, String>> assignments = new LinkedHashMap<>();

        for (int altI = 0; altI < inputs.getAlternativesIds().size(); altI++) {
            Map<String, String> interval = new LinkedHashMap<>();
            String alternativeId = inputs.getAlternativesIds().get(altI);

            interval.put(LOWER, inputs.getCategoryProfiles().get(0).getCategory().id());
            interval.put(UPPER, inputs.getCategoryProfiles().get(0).getCategory().id());
            assignments.put(alternativeId, interval);

            String positiveAssignment = null;
            String negativeAssignment = null;

            for (int catProfI = 0; catProfI < inputs.getCategoryProfiles().size()-1; catProfI++) {
                String upperProfileId = inputs.getCategoryProfiles().get(catProfI).getUpperBound().getAlternative().id();

                if (inputs.getPositiveFlows().get(alternativeId) >=
                        inputs.getPositiveFlows().get(upperProfileId)){
                    positiveAssignment = inputs.getCategoryProfiles().get(catProfI+1).getCategory().id();
                } else {
                    break;
                }
            }

            for (int catProfI = 0; catProfI < inputs.getCategoryProfiles().size()-1; catProfI++) {
                if (inputs.getNegativeFlows().get(alternativeId) <
                        inputs.getNegativeFlows().get(inputs.getCategoryProfiles().get(catProfI).getUpperBound().getAlternative().id())){
                    negativeAssignment = inputs.getCategoryProfiles().get(catProfI+1).getCategory().id();
                } else {
                    break;
                }
            }

            if (inputs.getCategoriesRanking().get(positiveAssignment) >= inputs.getCategoriesRanking().get(negativeAssignment)) {
                assignments.get(alternativeId).put(UPPER, positiveAssignment);
                assignments.get(alternativeId).put(LOWER, negativeAssignment);
            } else {
                assignments.get(alternativeId).put(LOWER, positiveAssignment);
                assignments.get(alternativeId).put(UPPER, negativeAssignment);
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
