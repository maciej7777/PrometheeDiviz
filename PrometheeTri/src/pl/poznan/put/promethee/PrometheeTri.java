package pl.poznan.put.promethee;

import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

import java.util.LinkedHashMap;
import java.util.Map;

public class PrometheeTri {

    public static OutputsHandler.Output sort(InputsHandler.Inputs inputs) {
        Map<String, String> assignmetns = new LinkedHashMap<>();

        for (int altI = 0 ; altI < inputs.alternativesIds.size(); altI++) {
            assignmetns.put(inputs.alternativesIds.get(altI), inputs.categoryProfiles.get(0).getCategory().id());
            Double bestDiff = Math.abs(inputs.flows.get(inputs.categoryProfiles.get(0).getCentralProfile().getAlternative().id()) - inputs.flows.get(inputs.alternativesIds.get(altI)));

            for (int i = 1; i < inputs.categoryProfiles.size(); i++) {
                Double tempDiff = Math.abs(inputs.flows.get(inputs.categoryProfiles.get(i).getCentralProfile().getAlternative().id()) - inputs.flows.get(inputs.alternativesIds.get(altI)));
                if ((tempDiff.doubleValue() < bestDiff.doubleValue()) || (tempDiff.doubleValue() == bestDiff.doubleValue() && inputs.assignToABetterClass)) {
                    assignmetns.put(inputs.alternativesIds.get(altI), inputs.categoryProfiles.get(i).getCategory().id());
                }
            }
        }

        OutputsHandler.Output output = new OutputsHandler.Output();
        output.assignments = assignmetns;

        return output;
    }
}
