package pl.poznan.put.promethee;

import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PrometheeTri {

    private PrometheeTri() {

    }

    public static OutputsHandler.Output sort(InputsHandler.Inputs inputs) {
        Map<String, String> assignments = new LinkedHashMap<>();

        Map<String, Map<String, BigDecimal>> criterionFlows = countSingleCriterionNetFlows(inputs);

        for (String alternativeId: inputs.getAlternativesIds()) {
            BigDecimal bestDistance = null;
            Integer bestCategoryNumber = null;

            for (int i = 0; i < inputs.getProfilesIds().size(); i++) {
                String profileId = inputs.getProfilesIds().get(i);
                BigDecimal distance = BigDecimal.ZERO;
                for (String criterionId: inputs.getCriteriaIds()) {
                    BigDecimal alternativeFlow = criterionFlows.get(alternativeId).get(criterionId);
                    BigDecimal profileFlow = criterionFlows.get(profileId).get(criterionId);
                    BigDecimal subtractionResult = alternativeFlow.subtract(profileFlow);

                    if (inputs.getUseMarginalValue()) {
                        subtractionResult = subtractionResult.abs();
                    }

                    BigDecimal weight = inputs.getCriteriaWeights().get(criterionId);

                    distance = distance.add(subtractionResult.multiply(weight));
                }

                distance = distance.abs();

                if (bestDistance == null || bestDistance.compareTo(distance) > 0 || (bestDistance.compareTo(distance) == 0 && inputs.getAssignToABetterClass())) {
                    bestDistance = distance;
                    bestCategoryNumber = i;
                }
            }

            String categoryId = inputs.getCategoryProfiles().get(bestCategoryNumber).getCategory().id();
            assignments.put(alternativeId, categoryId);
        }

        OutputsHandler.Output output = new OutputsHandler.Output();
        output.setAssignments(assignments);

        return output;
    }

    private static Map<String, Map<String, BigDecimal>> countSingleCriterionNetFlows(InputsHandler.Inputs inputs) {
        Map<String, Map<String, BigDecimal>> criterionFlows = new LinkedHashMap<>();

        for (int i = 0; i < inputs.getAlternativesIds().size(); i++) {
            String alternativeId = inputs.getAlternativesIds().get(i);

            Map <String, BigDecimal> alternativeCriteria = new LinkedHashMap<>();

            for (int c = 0; c < inputs.getCriteriaIds().size(); c++) {
                String criterionId = inputs.getCriteriaIds().get(c);
                BigDecimal criterionSum = BigDecimal.ZERO;

                for (int j = 0; j < inputs.getProfilesIds().size(); j++) {
                    String profileId = inputs.getProfilesIds().get(j);

                    BigDecimal alternativePreference = inputs.getPartialPreferences().get(alternativeId).get(profileId).get(criterionId);
                    BigDecimal profilePreference = inputs.getPartialPreferences().get(profileId).get(alternativeId).get(criterionId);

                    BigDecimal difference = alternativePreference.subtract(profilePreference);

                    criterionSum = criterionSum.add(difference);
                }

                int profilesCount = inputs.getProfilesIds().size();
                criterionSum = criterionSum.divide(BigDecimal.valueOf(profilesCount), 6, RoundingMode.HALF_UP);

                alternativeCriteria.put(criterionId, criterionSum);
            }

            criterionFlows.put(alternativeId, alternativeCriteria);
        }

        List<String> alternativesAndProfiles = new ArrayList<>();
        alternativesAndProfiles.addAll(inputs.getAlternativesIds());
        alternativesAndProfiles.addAll(inputs.getProfilesIds());


        for (int i = 0; i < inputs.getProfilesIds().size(); i++) {
            String firstProfileId = inputs.getProfilesIds().get(i);

            Map <String, BigDecimal> alternativeCriteria = new LinkedHashMap<>();

            for (int c = 0; c < inputs.getCriteriaIds().size(); c++) {
                String criterionId = inputs.getCriteriaIds().get(c);
                BigDecimal criterionSum = BigDecimal.ZERO;

                for (int j = 0; j < inputs.getProfilesIds().size(); j++) {
                    if (i == j) {
                        continue;
                    }

                    String secondProfileId = inputs.getProfilesIds().get(j);

                    BigDecimal firstProfilePreference = inputs.getPartialPreferences().get(firstProfileId).get(secondProfileId).get(criterionId);
                    BigDecimal secondProfilePreference = inputs.getPartialPreferences().get(secondProfileId).get(firstProfileId).get(criterionId);

                    BigDecimal difference = firstProfilePreference.subtract(secondProfilePreference);

                    criterionSum = criterionSum.add(difference);
                }

                int profilesCount = inputs.getProfilesIds().size();
                criterionSum = criterionSum.divide(BigDecimal.valueOf(profilesCount - 1L), 6, RoundingMode.HALF_UP);

                alternativeCriteria.put(criterionId, criterionSum);
            }

            criterionFlows.put(firstProfileId, alternativeCriteria);
        }

        return criterionFlows;
    }
}
