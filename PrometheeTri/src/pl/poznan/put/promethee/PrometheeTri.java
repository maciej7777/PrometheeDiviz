package pl.poznan.put.promethee;

import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class PrometheeTri {

    private PrometheeTri() {

    }

    public static OutputsHandler.Output sort(InputsHandler.Inputs inputs) {
        Map<String, String> assignments = new LinkedHashMap<>();

        for (int altI = 0 ; altI < inputs.alternativesIds.size(); altI++) {
            assignments.put(inputs.alternativesIds.get(altI), inputs.categoryProfiles.get(0).getCategory().id());
            BigDecimal firstProfileFlow = inputs.flows.get(inputs.categoryProfiles.get(0).getCentralProfile().getAlternative().id());
            BigDecimal alternativeFlow = inputs.flows.get(inputs.alternativesIds.get(altI));
            BigDecimal bestDiff = firstProfileFlow.subtract(alternativeFlow).abs();

            for (int i = 1; i < inputs.categoryProfiles.size(); i++) {

                BigDecimal profileFlow = inputs.flows.get(inputs.categoryProfiles.get(i).getCentralProfile().getAlternative().id());
                BigDecimal tempDiff = profileFlow.subtract(alternativeFlow).abs();

                if (tempDiff.compareTo(bestDiff) < 0 || (tempDiff.compareTo(bestDiff) == 0 && inputs.assignToABetterClass)) {
                    bestDiff = tempDiff;
                    assignments.put(inputs.alternativesIds.get(altI), inputs.categoryProfiles.get(i).getCategory().id());
                }
            }
        }

        OutputsHandler.Output output = new OutputsHandler.Output();
        output.setAssignments(assignments);

        return output;
    }
}
