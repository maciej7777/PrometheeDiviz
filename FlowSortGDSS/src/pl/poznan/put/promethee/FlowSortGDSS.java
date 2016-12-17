package pl.poznan.put.promethee;

import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

import java.util.*;

/**
 * Created by Maciej Uniejewski on 2016-12-10.
 */
public class FlowSortGDSS {

    private static final String LOWER = "LOWER";
    private static final String UPPER = "UPPER";

    private FlowSortGDSS() {
        throw new IllegalAccessError("Utility class");
    }

    public static OutputsHandler.Output sort(InputsHandler.Inputs inputs) {

        countProfilesSummaryFlows(inputs);
        OutputsHandler.Output output = new OutputsHandler.Output();

        if ("bounding".equalsIgnoreCase(inputs.profilesType.toString())) {
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
            Set<Integer> firstClassDecisionMakers = new HashSet<>();
            Set<Integer> secondClassDecisionMakers = new HashSet<>();
            String alternativeId = inputs.alternativesIds.get(i);
            for (int decisionMaker = 0; decisionMaker < inputs.profilesIds.size(); decisionMaker++) {
                Integer decisionMakerClassNumber = null;
                for (int profile = 0; profile < inputs.profilesIds.get(decisionMaker).size(); profile++) {
                    String profilesId = inputs.profilesIds.get(decisionMaker).get(profile);
                    if (inputs.alternativesFlowsAverage.get(alternativeId) <= inputs.profilesSummaryFlows.get(profilesId).get(alternativeId) && decisionMakerClassNumber == null) {
                        decisionMakerClassNumber = profile;
                        break;
                    }
                }
                if (decisionMakerClassNumber == null && inputs.alternativesFlowsAverage.get(alternativeId) > inputs.profilesSummaryFlows.get(inputs.profilesIds.get(decisionMaker).get(inputs.profilesIds.get(decisionMaker).size() - 1)).get(alternativeId)) {
                    decisionMakerClassNumber = inputs.profilesIds.get(decisionMaker).size();
                }
                if (firstClassNumber == null) {
                    firstClassNumber = decisionMakerClassNumber;
                    firstClassDecisionMakers.add(decisionMaker);
                } else if (firstClassNumber.intValue() != decisionMakerClassNumber) {
                    secondClassNumber = decisionMakerClassNumber;
                    secondClassDecisionMakers.add(decisionMaker);
                } else {
                    firstClassDecisionMakers.add(decisionMaker);
                }
            }

            if (secondClassNumber == null) {
                String classId = inputs.categoryProfiles.get(0).get(firstClassNumber).getCategory().id();
                assignments.put(alternativeId, classId);
                Map<String, String> interval = new LinkedHashMap<>();
                interval.put(LOWER,classId);
                interval.put(UPPER, classId);
                firstStepAssignments.put(alternativeId, interval);
            } else {
                String leftClassId = inputs.categoryProfiles.get(0).get(Math.min(firstClassNumber, secondClassNumber)).getCategory().id();
                String rightClassId = inputs.categoryProfiles.get(0).get(Math.max(firstClassNumber, secondClassNumber)).getCategory().id();
                Map<String, String> interval = new LinkedHashMap<>();
                interval.put(LOWER,leftClassId);
                interval.put(UPPER, rightClassId);
                firstStepAssignments.put(alternativeId, interval);

                Double profileK;
                Double profileK1;

                if (firstClassNumber.intValue() < secondClassNumber.intValue()) {
                    profileK = countDkForLimitingProfiles(alternativeId, firstClassNumber, firstClassDecisionMakers, inputs);
                    profileK1 = countDk1ForLimitingProfiles(alternativeId, secondClassNumber, secondClassDecisionMakers, inputs);
                } else {
                    profileK = countDkForLimitingProfiles(alternativeId, secondClassNumber, secondClassDecisionMakers, inputs);
                    profileK1 = countDk1ForLimitingProfiles(alternativeId, firstClassNumber, firstClassDecisionMakers, inputs);
                }

                if (profileK1 - profileK > 0) {
                    assignments.put(alternativeId, rightClassId);
                } else if (profileK1 - profileK < 0) {
                    assignments.put(alternativeId, leftClassId);
                } else {
                    if (inputs.assignToABetterClass) {
                        assignments.put(alternativeId, rightClassId);
                    } else {
                        assignments.put(alternativeId, leftClassId);
                    }
                }
            }
        }
        output.setAssignments(assignments);
        output.setFirstStepAssignments(firstStepAssignments);
    }

    private static double countDkForLimitingProfiles(String alternativeId, Integer profile, Set<Integer> decisionMakers, InputsHandler.Inputs inputs) {
        double sum = 0.0;

        for (Integer decisionMaker: decisionMakers) {
            String profileId = inputs.profilesIds.get(decisionMaker).get(profile);
            sum += inputs.decisionMakersWages.get(decisionMaker) * (inputs.alternativesFlowsAverage.get(alternativeId) - inputs.profilesSummaryFlows.get(profileId).get(alternativeId));
        }

        return sum;
    }

    private static double countDk1ForLimitingProfiles(String alternativeId, Integer profile, Set<Integer> decisionMakers, InputsHandler.Inputs inputs) {
        double sum = 0.0;

        for (Integer decisionMaker: decisionMakers) {
            String profileId = inputs.profilesIds.get(decisionMaker).get(profile);
            sum += inputs.decisionMakersWages.get(decisionMaker) * (inputs.profilesSummaryFlows.get(profileId).get(alternativeId) - inputs.alternativesFlowsAverage.get(alternativeId));
        }

        return sum;
    }

    private static void sortWithCentralProfiles(InputsHandler.Inputs inputs, OutputsHandler.Output output) {
        Map<String, String> assignments = new LinkedHashMap<>();
        Map<String, Map<String, String>> firstStepAssignments = new LinkedHashMap<>();

        for (int i = 0; i < inputs.alternativesIds.size(); i++) {
            Integer firstClassNumber = null;
            Integer secondClassNumber = null;
            Set<Integer> firstClassDecisionMakers = new HashSet<>();
            Set<Integer> secondClassDecisionMakers = new HashSet<>();
            String alternativeId = inputs.alternativesIds.get(i);
            for (int decisionMaker = 0; decisionMaker < inputs.profilesIds.size(); decisionMaker++) {
                Integer nearestCalss = null;
                double distance = 0.0;
                for (int profile = 0; profile < inputs.profilesIds.get(decisionMaker).size(); profile++) {
                    String profilesId = inputs.profilesIds.get(decisionMaker).get(profile);
                    double tmpDist = Math.abs(inputs.profilesSummaryFlows.get(profilesId).get(alternativeId) - inputs.alternativesFlowsAverage.get(alternativeId));

                    if (tmpDist < distance || nearestCalss == null) {
                        nearestCalss = profile;
                        distance = tmpDist;
                    }
                }

                if (firstClassNumber == null) {
                    firstClassNumber = nearestCalss;
                    firstClassDecisionMakers.add(decisionMaker);
                } else if (nearestCalss != null && firstClassNumber.intValue() != nearestCalss.intValue()) {
                    secondClassNumber = nearestCalss;
                    secondClassDecisionMakers.add(decisionMaker);
                } else {
                    firstClassDecisionMakers.add(decisionMaker);
                }
            }
            if (secondClassNumber == null) {
                String classId = inputs.categoryProfiles.get(0).get(firstClassNumber).getCategory().id();
                assignments.put(alternativeId, classId);
                Map<String, String> interval = new LinkedHashMap<>();
                interval.put(LOWER,classId);
                interval.put(UPPER, classId);
                firstStepAssignments.put(alternativeId, interval);
            } else {
                String leftClassId = inputs.categoryProfiles.get(0).get(Math.min(firstClassNumber, secondClassNumber)).getCategory().id();
                String rightClassId = inputs.categoryProfiles.get(0).get(Math.max(firstClassNumber, secondClassNumber)).getCategory().id();
                Map<String, String> interval = new LinkedHashMap<>();
                interval.put(LOWER,leftClassId);
                interval.put(UPPER, rightClassId);
                firstStepAssignments.put(alternativeId, interval);

                Double profileK;
                Double profileK1;

                if (firstClassNumber.intValue() < secondClassNumber.intValue()) {
                    profileK = countDkForCentralProfiles(alternativeId, firstClassNumber, firstClassDecisionMakers, inputs);
                    profileK1 = countDkForCentralProfiles(alternativeId, secondClassNumber, secondClassDecisionMakers, inputs);
                } else {
                    profileK = countDkForCentralProfiles(alternativeId, secondClassNumber, secondClassDecisionMakers, inputs);
                    profileK1 = countDkForCentralProfiles(alternativeId, firstClassNumber, firstClassDecisionMakers, inputs);
                }

                if (profileK1 - profileK > 0) {
                    assignments.put(alternativeId, rightClassId);
                } else if (profileK - profileK1 < 0) {
                    assignments.put(alternativeId, leftClassId);
                } else {
                    if (inputs.assignToABetterClass) {
                        assignments.put(alternativeId, rightClassId);
                    } else {
                        assignments.put(alternativeId, leftClassId);
                    }
                }
            }
        }
        output.setAssignments(assignments);
        output.setFirstStepAssignments(firstStepAssignments);
    }

    private static double countDkForCentralProfiles(String alternativeId, Integer profile, Set<Integer> decisionMakers, InputsHandler.Inputs inputs) {
        double sum = 0.0;

        for (Integer decisionMaker: decisionMakers) {
            String profileId = inputs.profilesIds.get(decisionMaker).get(profile);
            sum += inputs.decisionMakersWages.get(decisionMaker) * Math.abs(inputs.profilesSummaryFlows.get(profileId).get(alternativeId) - inputs.alternativesFlowsAverage.get(alternativeId));
        }

        return sum;
    }
}
