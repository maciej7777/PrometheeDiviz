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
            String alternativeId = inputs.getAlternativesIds().get(altI);

            String positiveAssignment = inputs.getCategoryProfiles().get(0).getCategory().id();
            String negativeAssignment = inputs.getCategoryProfiles().get(0).getCategory().id();

            for (int catProfI = 1; catProfI < inputs.getCategoryProfiles().size() ; catProfI++) {
                if (inputs.getPositiveFlows().get(alternativeId) >=
                        (inputs.getPositiveFlows().get(inputs.getCategoryProfiles().get(catProfI).getCentralProfile().getAlternative().id()) +
                                inputs.getPositiveFlows().get(inputs.getCategoryProfiles().get(catProfI-1).getCentralProfile().getAlternative().id()))/2) {
                    positiveAssignment = inputs.getCategoryProfiles().get(catProfI).getCategory().id();
                } else {
                    break;
                }
            }

            for (int catProfI = 1; catProfI < inputs.getCategoryProfiles().size() ; catProfI++) {
                if (inputs.getNegativeFlows().get(alternativeId) <
                        (inputs.getNegativeFlows().get(inputs.getCategoryProfiles().get(catProfI).getCentralProfile().getAlternative().id()) +
                                inputs.getNegativeFlows().get(inputs.getCategoryProfiles().get(catProfI - 1).getCentralProfile().getAlternative().id()))/2) {
                    negativeAssignment = inputs.getCategoryProfiles().get(catProfI).getCategory().id();
                } else {
                    break;
                }
            }

            Map<String, String> interval = new LinkedHashMap<>();

            if (inputs.getCategoriesRanking().get(positiveAssignment) >= inputs.getCategoriesRanking().get(negativeAssignment)) {
                interval.put(UPPER, positiveAssignment);
                interval.put(LOWER, negativeAssignment);
            } else {
                interval.put(LOWER, positiveAssignment);
                interval.put(UPPER, negativeAssignment);
            }

            assignments.put(alternativeId, interval);


        }

        OutputsHandler.Output output = new OutputsHandler.Output();
        output.setAssignments(assignments);
        return output;
    }

    public static OutputsHandler.Output sortWithBoundaryProfiles(InputsHandler.Inputs inputs) {

        Map<String, Map<String, String>> assignments = new LinkedHashMap<>();

        for (int altI = 0; altI < inputs.getAlternativesIds().size(); altI++) {
            String alternativeId = inputs.getAlternativesIds().get(altI);

            String positiveAssignment = inputs.getCategoryProfiles().get(0).getCategory().id();
            String negativeAssignment = inputs.getCategoryProfiles().get(0).getCategory().id();

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

            Map<String, String> interval = new LinkedHashMap<>();

            if (inputs.getCategoriesRanking().get(positiveAssignment) >= inputs.getCategoriesRanking().get(negativeAssignment)) {
                interval.put(UPPER, positiveAssignment);
                interval.put(LOWER, negativeAssignment);
            } else {
                interval.put(LOWER, positiveAssignment);
                interval.put(UPPER, negativeAssignment);
            }

            assignments.put(alternativeId, interval);
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
