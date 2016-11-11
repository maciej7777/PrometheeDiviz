package pl.poznan.put.promethee;

import org.xmcda.AlternativesAssignments;
import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

import java.util.*;

/**
 * Created by Maciej Uniejewski on 2016-11-01.
 */
public class Promsort {

    private static boolean isA1PreferedToA2(double a1PositiveFlow, double a1NegativeFlow, double a2PositiveFlow, double a2NegativeFlow) {
        if ((a1PositiveFlow > a2PositiveFlow && a1NegativeFlow < a2NegativeFlow) ||
                (a1PositiveFlow == a2PositiveFlow && a1NegativeFlow < a2NegativeFlow) ||
                (a1PositiveFlow > a2PositiveFlow && a1NegativeFlow == a2NegativeFlow)) {
            return true;
        }
        return false;
    }

    private static boolean isA1IndifferencedToA2(double a1PositiveFlow, double a1NegativeFlow, double a2PositiveFlow, double a2NegativeFlow) {
        if (a1PositiveFlow == a2PositiveFlow && a1NegativeFlow == a2NegativeFlow){
            return true;
        }
        return false;
    }

    private static boolean isA1IncomparableToA2(double a1PositiveFlow, double a1NegativeFlow, double a2PositiveFlow, double a2NegativeFlow) {
        if ((a1PositiveFlow > a2PositiveFlow && a1NegativeFlow > a2NegativeFlow) ||
                (a1PositiveFlow < a2PositiveFlow && a1NegativeFlow < a2NegativeFlow)){
            return true;
        }
        return false;
    }


    public static OutputsHandler.Output sort(InputsHandler.Inputs inputs) {
        Map<String, Map<String, String>> firstStepAssignments = new LinkedHashMap<>();
        Map<String, String> finalAssignments = new LinkedHashMap<>();
        //test with xmcda object
        AlternativesAssignments firstStepAssignments1 = new AlternativesAssignments();
        AlternativesAssignments finalAssignments1 = new AlternativesAssignments();

        Map<String, Double> categoriesFlows = new LinkedHashMap<>();
        List<String> unassignedAlternatives = new ArrayList<>();
        Map<String, Set<String>> assignedAlternatives = new LinkedHashMap<>();

        for (int i = 0; i < inputs.categoriesIds.size(); i++) {
            assignedAlternatives.put(inputs.categoriesIds.get(i), new LinkedHashSet<>());
            categoriesFlows.put(inputs.categoriesIds.get(i), 0.0);
        }

        for (int altI = 0 ; altI < inputs.alternativesIds.size(); altI++) {
            boolean marked = false;

            for (int catProfI = inputs.categoryProfiles.size()-2; catProfI >= 0 ; catProfI--) {
                if (isA1PreferedToA2(inputs.positiveFlows.get(inputs.alternativesIds.get(altI)),
                        inputs.negativeFlows.get(inputs.alternativesIds.get(altI)),
                        inputs.positiveFlows.get(inputs.categoryProfiles.get(catProfI).getUpperBound().getAlternative().id()),
                        inputs.negativeFlows.get(inputs.categoryProfiles.get(catProfI).getUpperBound().getAlternative().id()))) {

                    finalAssignments.put(inputs.alternativesIds.get(altI), inputs.categoryProfiles.get(catProfI+1).getCategory().id());
/*                    AlternativeAssignment a = new AlternativeAssignment();
                    a.setAlternative(new Alternative(inputs.alternativesIds.get(altI)));
                    a.setCategory(new Category(inputs.categoryProfiles.get(catProfI+1).getCategory().id()));
                    finalAssignments1.add(altI, a);*/
                    Map<String, String> interval = new LinkedHashMap<>();
                    interval.put("LOWER",inputs.categoryProfiles.get(catProfI+1).getCategory().id());
                    interval.put("UPPER", inputs.categoryProfiles.get(catProfI+1).getCategory().id());
                    firstStepAssignments.put(inputs.alternativesIds.get(altI), interval);

                    assignedAlternatives.get(inputs.categoryProfiles.get(catProfI+1).getCategory().id()).add(inputs.alternativesIds.get(altI));
                    categoriesFlows.put(inputs.categoryProfiles.get(catProfI+1).getCategory().id(),
                            categoriesFlows.getOrDefault(inputs.categoryProfiles.get(catProfI+1).getCategory().id(), 0.0) +
                                    inputs.positiveFlows.get(inputs.alternativesIds.get(altI)) - inputs.negativeFlows.get(inputs.alternativesIds.get(altI)));
                    marked = true;
                    break;
                } else if (isA1IndifferencedToA2(inputs.positiveFlows.get(inputs.alternativesIds.get(altI)),
                        inputs.negativeFlows.get(inputs.alternativesIds.get(altI)),
                        inputs.positiveFlows.get(inputs.categoryProfiles.get(catProfI).getUpperBound().getAlternative().id()),
                        inputs.negativeFlows.get(inputs.categoryProfiles.get(catProfI).getUpperBound().getAlternative().id()))) {
                    Map<String, String> interval = new LinkedHashMap<>();
                    interval.put("LOWER",inputs.categoryProfiles.get(catProfI).getCategory().id());
                    interval.put("UPPER", inputs.categoryProfiles.get(catProfI+1).getCategory().id());
                    firstStepAssignments.put(inputs.alternativesIds.get(altI), interval);

                    marked = true;
                    unassignedAlternatives.add(inputs.alternativesIds.get(altI));
                    break;
                }
            }
            if (!marked) {
                assignedAlternatives.get(inputs.categoryProfiles.get(0).getCategory().id()).add(inputs.alternativesIds.get(altI));
                categoriesFlows.put(inputs.categoryProfiles.get(0).getCategory().id(),
                        categoriesFlows.getOrDefault(inputs.categoryProfiles.get(0).getCategory().id(), 0.0) +
                                inputs.positiveFlows.get(inputs.alternativesIds.get(altI)) - inputs.negativeFlows.get(inputs.alternativesIds.get(altI)));
                finalAssignments.put(inputs.alternativesIds.get(altI), inputs.categoryProfiles.get(0).getCategory().id());
                Map<String, String> interval = new LinkedHashMap<>();
                interval.put("LOWER",inputs.categoryProfiles.get(0).getCategory().id());
                interval.put("UPPER", inputs.categoryProfiles.get(0).getCategory().id());
                firstStepAssignments.put(inputs.alternativesIds.get(altI), interval);
            }
        }

        //Now let's calculate second step
        if (unassignedAlternatives.size() != 0) {
            for (int i = 0; i < unassignedAlternatives.size(); i++) {
                String categoryT = firstStepAssignments.get(unassignedAlternatives.get(i)).get("LOWER");
                String categoryT1 = firstStepAssignments.get(unassignedAlternatives.get(i)).get("UPPER");

                Integer lenghtT = assignedAlternatives.get(categoryT).size();
                Integer lenghtT1 = assignedAlternatives.get(categoryT1).size();

                Double dkPositive = lenghtT * (inputs.positiveFlows.get(unassignedAlternatives.get(i)) -
                        inputs.negativeFlows.get(unassignedAlternatives.get(i)) - categoriesFlows.get(categoryT));
                Double dkNegative = categoriesFlows.get(categoryT1) - lenghtT1 * (inputs.positiveFlows.get(unassignedAlternatives.get(i)) -
                        inputs.negativeFlows.get(unassignedAlternatives.get(i)));

                Double dk1 = 0.0;
                if (lenghtT > 0) {
                    dk1 = dkPositive/lenghtT;
                }

                Double dk2 = 0.0;
                if (lenghtT1 > 0) {
                    dk2 = dkNegative/lenghtT1;
                }

                Double dk = dk1 - dk2;

                if (dk > inputs.cutPoint) {
                    finalAssignments.put(unassignedAlternatives.get(i), categoryT1);
                } else if (dk < inputs.cutPoint) {
                    finalAssignments.put(unassignedAlternatives.get(i), categoryT);
                } else if (inputs.assignToABetterClass) {
                    finalAssignments.put(unassignedAlternatives.get(i), categoryT1);
                } else {
                    finalAssignments.put(unassignedAlternatives.get(i), categoryT);
                }
            }
        }

        OutputsHandler.Output output = new OutputsHandler.Output();
        output.firstStepAssignments = firstStepAssignments;
        output.finalAssignments = finalAssignments;
        return output;
    }

}
