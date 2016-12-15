package pl.poznan.put.promethee;

import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

import java.util.*;

/**
 * Created by Maciej Uniejewski on 2016-12-10.
 */
public class FlowSortGDSS {

    public static OutputsHandler.Output sort(InputsHandler.Inputs inputs) {

        countProfilesSummaryFlows(inputs);
        OutputsHandler.Output output = new OutputsHandler.Output();

        if (inputs.profilesType.toString().toLowerCase().equals("bounding")) {
            sortWithLimitingProfiles(inputs, output);
        } else {
            sortWithCentralProfiles(inputs, output);
        }

        return output;
    }

    private static void countProfilesSummaryFlows(InputsHandler.Inputs inputs) {
        inputs.profilesSummaryFlows = new LinkedHashMap<>();

        for (int i = 0; i < inputs.profilesIds.size(); i++) {
            for (int j = 0; j < inputs.profilesIds.get(i).size(); j++) {
                for (int k = 0; k < inputs.alternativesIds.size(); k++) {
                    String profileId = inputs.profilesIds.get(i).get(j);
                    String alternativeId = inputs.alternativesIds.get(k);

                    Integer profilesNumber = inputs.profilesIds.size() * inputs.profilesIds.get(i).size();
                    Double leftFlow = inputs.profilesFlows.get(profileId) * profilesNumber;
                    Double rightFlow = inputs.preferences.get(i).get(profileId).get(alternativeId) - inputs.preferences.get(i).get(alternativeId).get(profileId);

                    Double flow = (leftFlow + rightFlow) / (profilesNumber + 1);

                    inputs.profilesSummaryFlows.putIfAbsent(profileId, new HashMap<>());
                    inputs.profilesSummaryFlows.get(profileId).put(alternativeId, flow);
                }
            }
        }
    }

    private static void sortWithLimitingProfiles(InputsHandler.Inputs inputs, OutputsHandler.Output output) {
        Map<String, String> assignments = new LinkedHashMap<>();
        Map<String, Map<String, String>> firstStepAssignments = new LinkedHashMap<>();

        for (int i = 0; i < inputs.alternativesIds.size(); i++) {
            Integer firstClassNumber = null;
            Integer secondClassNumber = null;
            String alternativeId = inputs.alternativesIds.get(i);
            for (int decisionMaker = 0; decisionMaker < inputs.profilesIds.size(); decisionMaker++) {
                Integer decisionMakerClassNumber = null;
                for (int profile = 0; profile < inputs.profilesIds.get(decisionMaker).size(); profile++) {
                    String profilesId = inputs.profilesIds.get(decisionMaker).get(profile);
                    if (inputs.alternativesFlowsAverage.get(alternativeId) <= inputs.profilesSummaryFlows.get(profilesId).get(alternativeId)) {
                        if (decisionMakerClassNumber == null) {
                            decisionMakerClassNumber = profile;
                            break;
                        }
                    }
                }
                if (decisionMakerClassNumber == null) {
                    if (inputs.alternativesFlowsAverage.get(alternativeId) > inputs.profilesSummaryFlows.get(inputs.profilesIds.get(decisionMaker).get(0)).get(alternativeId)) {
                        decisionMakerClassNumber = 0;
                    }
                }
                if (firstClassNumber == null) {
                    firstClassNumber = decisionMakerClassNumber;
                } else if (firstClassNumber.intValue() != decisionMakerClassNumber) {
                    secondClassNumber = decisionMakerClassNumber;
                }
            }

            if (secondClassNumber == null) {
                String classId = inputs.categoryProfiles.get(0).get(firstClassNumber).getCategory().id();
                assignments.put(alternativeId, classId);
                Map<String, String> interval = new LinkedHashMap<>();
                interval.put("LOWER",classId);
                interval.put("UPPER", classId);
                firstStepAssignments.put(alternativeId, interval);
            } else {
                String leftClassId = inputs.categoryProfiles.get(0).get(Math.min(firstClassNumber, secondClassNumber)).getCategory().id();
                String rightClassId = inputs.categoryProfiles.get(0).get(Math.max(firstClassNumber, secondClassNumber)).getCategory().id();
                Map<String, String> interval = new LinkedHashMap<>();
                interval.put("LOWER",leftClassId);
                interval.put("UPPER", rightClassId);
                firstStepAssignments.put(alternativeId, interval);

                List<Double> profilesFlowsWeightedSum = countWeightedSumForLimitingProfiles(alternativeId, inputs);

                Double profileK = profilesFlowsWeightedSum.get(Math.min(firstClassNumber, secondClassNumber));
                Double profileK1 = profilesFlowsWeightedSum.get(Math.max(firstClassNumber, secondClassNumber));

                if (profileK - profileK1 > 0) {
                    assignments.put(alternativeId, leftClassId);
                } else if (profileK - profileK1 < 0) {
                    assignments.put(alternativeId, rightClassId);
                } else {
                    if (inputs.assignToABetterClass) {
                        assignments.put(alternativeId, rightClassId);
                    } else {
                        assignments.put(alternativeId, leftClassId);
                    }
                }
            }
        }
        output.assignments = assignments;
        output.firstStepAssignments = firstStepAssignments;
    }

    private static List<Double> countWeightedSumForLimitingProfiles(String alternativeId, InputsHandler.Inputs inputs) {
        List<Double> profilesFlowsWeightedSum = new ArrayList<>();

        //For each profile
        for (int i = 0; i < inputs.profilesIds.get(0).size(); i++) {
            Double sum = 0.0;
            //For each decision maker
            for (int j = 0; j < inputs.profilesIds.size(); j++) {
                String profileId = inputs.profilesIds.get(j).get(i);
                sum += inputs.decisionMakersWages.get(j) * inputs.profilesSummaryFlows.get(profileId).get(alternativeId);
            }
            profilesFlowsWeightedSum.add(sum / inputs.profilesIds.size());
        }
        return profilesFlowsWeightedSum;
    }

    private static void sortWithCentralProfiles(InputsHandler.Inputs inputs, OutputsHandler.Output output) {

    }
}
