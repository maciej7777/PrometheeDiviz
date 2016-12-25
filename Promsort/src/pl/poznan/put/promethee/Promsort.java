package pl.poznan.put.promethee;

import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by Maciej Uniejewski on 2016-11-01.
 */
public class Promsort {

    private static final String LOWER = "LOWER";
    private static final String UPPER = "UPPER";

    private Promsort() {

    }

    private static boolean preferenceCondition1(BigDecimal a1PositiveFlow, BigDecimal a1NegativeFlow, BigDecimal a2PositiveFlow, BigDecimal a2NegativeFlow) {
        return a1PositiveFlow.compareTo(a2PositiveFlow) > 0 && a1NegativeFlow.compareTo(a2NegativeFlow) < 0;
    }

    private static boolean preferenceCondition2(BigDecimal a1PositiveFlow, BigDecimal a1NegativeFlow, BigDecimal a2PositiveFlow, BigDecimal a2NegativeFlow) {
        return a1PositiveFlow.compareTo(a2PositiveFlow) == 0 && a1NegativeFlow.compareTo(a2NegativeFlow) < 0;
    }

    private static boolean preferenceCondition3(BigDecimal a1PositiveFlow, BigDecimal a1NegativeFlow, BigDecimal a2PositiveFlow, BigDecimal a2NegativeFlow) {
        return a1PositiveFlow.compareTo(a2PositiveFlow) > 0 && a1NegativeFlow.compareTo(a2NegativeFlow) == 0;
    }


    private static boolean isA1PreferedToA2(BigDecimal a1PositiveFlow, BigDecimal a1NegativeFlow, BigDecimal a2PositiveFlow, BigDecimal a2NegativeFlow) {
        if ( preferenceCondition1(a1PositiveFlow, a1NegativeFlow, a2PositiveFlow, a2NegativeFlow) ||
                preferenceCondition2(a1PositiveFlow, a1NegativeFlow, a2PositiveFlow, a2NegativeFlow) ||
                preferenceCondition3(a1PositiveFlow, a1NegativeFlow, a2PositiveFlow, a2NegativeFlow)) {
            return true;
        }
        return false;
    }

    private static boolean isA1IndifferencedToA2(BigDecimal a1PositiveFlow, BigDecimal a1NegativeFlow, BigDecimal a2PositiveFlow, BigDecimal a2NegativeFlow) {
        if (a1PositiveFlow.compareTo(a2PositiveFlow) == 0 && a1NegativeFlow.compareTo(a2NegativeFlow) == 0){
            return true;
        }
        return false;
    }

    public static OutputsHandler.Output sort(InputsHandler.Inputs inputs) {
        Map<String, Map<String, String>> firstStepAssignments = new LinkedHashMap<>();
        Map<String, String> finalAssignments = new LinkedHashMap<>();

        Map<String, BigDecimal> categoriesFlows = new LinkedHashMap<>();
        List<String> unassignedAlternatives = new ArrayList<>();
        Map<String, Set<String>> assignedAlternatives = new LinkedHashMap<>();

        for (int i = 0; i < inputs.categoriesIds.size(); i++) {
            assignedAlternatives.put(inputs.categoriesIds.get(i), new LinkedHashSet<>());
            categoriesFlows.put(inputs.categoriesIds.get(i), BigDecimal.ZERO);
        }

        for (int altI = 0 ; altI < inputs.alternativesIds.size(); altI++) {
            boolean marked = false;

            for (int catProfI = inputs.categoryProfiles.size()-2; catProfI >= 0 ; catProfI--) {
                if (isA1PreferedToA2(inputs.positiveFlows.get(inputs.alternativesIds.get(altI)),
                        inputs.negativeFlows.get(inputs.alternativesIds.get(altI)),
                        inputs.positiveFlows.get(inputs.categoryProfiles.get(catProfI).getUpperBound().getAlternative().id()),
                        inputs.negativeFlows.get(inputs.categoryProfiles.get(catProfI).getUpperBound().getAlternative().id()))) {

                    finalAssignments.put(inputs.alternativesIds.get(altI), inputs.categoryProfiles.get(catProfI+1).getCategory().id());

                    Map<String, String> interval = new LinkedHashMap<>();
                    interval.put(LOWER,inputs.categoryProfiles.get(catProfI+1).getCategory().id());
                    interval.put(UPPER, inputs.categoryProfiles.get(catProfI+1).getCategory().id());
                    firstStepAssignments.put(inputs.alternativesIds.get(altI), interval);

                    assignedAlternatives.get(inputs.categoryProfiles.get(catProfI+1).getCategory().id()).add(inputs.alternativesIds.get(altI));
                    categoriesFlows.put(inputs.categoryProfiles.get(catProfI+1).getCategory().id(),
                            categoriesFlows.getOrDefault(inputs.categoryProfiles.get(catProfI+1).getCategory().id(), BigDecimal.ZERO).
                                    add(inputs.positiveFlows.get(inputs.alternativesIds.get(altI))).
                                    subtract(inputs.negativeFlows.get(inputs.alternativesIds.get(altI))));
                    marked = true;
                    break;
                } else if (isA1IndifferencedToA2(inputs.positiveFlows.get(inputs.alternativesIds.get(altI)),
                        inputs.negativeFlows.get(inputs.alternativesIds.get(altI)),
                        inputs.positiveFlows.get(inputs.categoryProfiles.get(catProfI).getUpperBound().getAlternative().id()),
                        inputs.negativeFlows.get(inputs.categoryProfiles.get(catProfI).getUpperBound().getAlternative().id()))) {
                    Map<String, String> interval = new LinkedHashMap<>();
                    interval.put(LOWER,inputs.categoryProfiles.get(catProfI).getCategory().id());
                    interval.put(UPPER, inputs.categoryProfiles.get(catProfI+1).getCategory().id());
                    firstStepAssignments.put(inputs.alternativesIds.get(altI), interval);

                    marked = true;
                    unassignedAlternatives.add(inputs.alternativesIds.get(altI));
                    break;
                }
            }
            if (!marked) {
                assignedAlternatives.get(inputs.categoryProfiles.get(0).getCategory().id()).add(inputs.alternativesIds.get(altI));
                categoriesFlows.put(inputs.categoryProfiles.get(0).getCategory().id(),
                        categoriesFlows.getOrDefault(inputs.categoryProfiles.get(0).getCategory().id(), BigDecimal.ZERO).
                                add(inputs.positiveFlows.get(inputs.alternativesIds.get(altI))).
                                subtract(inputs.negativeFlows.get(inputs.alternativesIds.get(altI))));
                finalAssignments.put(inputs.alternativesIds.get(altI), inputs.categoryProfiles.get(0).getCategory().id());
                Map<String, String> interval = new LinkedHashMap<>();
                interval.put(LOWER,inputs.categoryProfiles.get(0).getCategory().id());
                interval.put(UPPER, inputs.categoryProfiles.get(0).getCategory().id());
                firstStepAssignments.put(inputs.alternativesIds.get(altI), interval);
            }
        }

        performSecondStep(unassignedAlternatives, firstStepAssignments, assignedAlternatives, inputs, finalAssignments,
                categoriesFlows);

        OutputsHandler.Output output = new OutputsHandler.Output();
        output.setFirstStepAssignments(firstStepAssignments);
        output.setFinalAssignments(finalAssignments);
        return output;
    }

    private static void performSecondStep(List<String> unassignedAlternatives, Map<String, Map<String, String>> firstStepAssignments,
                                          Map<String, Set<String>> assignedAlternatives, InputsHandler.Inputs inputs,
                                          Map<String, String> finalAssignments, Map<String, BigDecimal> categoriesFlows) {
        if (!unassignedAlternatives.isEmpty()) {
            for (int i = 0; i < unassignedAlternatives.size(); i++) {
                String categoryT = firstStepAssignments.get(unassignedAlternatives.get(i)).get(LOWER);
                String categoryT1 = firstStepAssignments.get(unassignedAlternatives.get(i)).get(UPPER);

                BigDecimal lengthT = new BigDecimal(assignedAlternatives.get(categoryT).size());
                BigDecimal lengthT1 = new BigDecimal(assignedAlternatives.get(categoryT1).size());


                BigDecimal dkPositive = lengthT.multiply(inputs.positiveFlows.get(unassignedAlternatives.get(i)).
                        subtract(inputs.negativeFlows.get(unassignedAlternatives.get(i))).
                        subtract( categoriesFlows.get(categoryT)));
                BigDecimal dkNegative = categoriesFlows.get(categoryT1).
                        subtract(lengthT1.multiply(inputs.positiveFlows.get(unassignedAlternatives.get(i)).
                                subtract(inputs.negativeFlows.get(unassignedAlternatives.get(i)))));

                BigDecimal dk1 = BigDecimal.ZERO;
                if (lengthT.compareTo(BigDecimal.ZERO) > 0) {
                    dk1 = dkPositive.divide(lengthT);
                }

                BigDecimal dk2 = BigDecimal.ZERO;
                if (lengthT1.compareTo(BigDecimal.ZERO) > 0) {
                    dk2 = dkNegative.divide(lengthT1);
                }

                BigDecimal dk = dk1.subtract(dk2);

                if (dk.compareTo(inputs.cutPoint) > 0 || (dk.compareTo(inputs.cutPoint) == 0 && inputs.assignToABetterClass)) {
                    finalAssignments.put(unassignedAlternatives.get(i), categoryT1);
                } else {
                    finalAssignments.put(unassignedAlternatives.get(i), categoryT);
                }
            }
        }
    }

}
