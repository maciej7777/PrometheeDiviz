package pl.poznan.put.promethee;

import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        for (int i = 0; i < inputs.getCategoriesIds().size(); i++) {
            assignedAlternatives.put(inputs.getCategoriesIds().get(i), new LinkedHashSet<>());
            categoriesFlows.put(inputs.getCategoriesIds().get(i), BigDecimal.ZERO);
        }

        for (int altI = 0; altI < inputs.getAlternativesIds().size(); altI++) {
            boolean marked = false;

            for (int catProfI = inputs.getCategoryProfiles().size()-2; catProfI >= 0 ; catProfI--) {
                if (isA1PreferedToA2(inputs.getPositiveFlows().get(inputs.getAlternativesIds().get(altI)),
                        inputs.getNegativeFlows().get(inputs.getAlternativesIds().get(altI)),
                        inputs.getPositiveFlows().get(inputs.getCategoryProfiles().get(catProfI).getUpperBound().getAlternative().id()),
                        inputs.getNegativeFlows().get(inputs.getCategoryProfiles().get(catProfI).getUpperBound().getAlternative().id()))) {

                    finalAssignments.put(inputs.getAlternativesIds().get(altI), inputs.getCategoryProfiles().get(catProfI+1).getCategory().id());

                    Map<String, String> interval = new LinkedHashMap<>();
                    interval.put(LOWER, inputs.getCategoryProfiles().get(catProfI+1).getCategory().id());
                    interval.put(UPPER, inputs.getCategoryProfiles().get(catProfI+1).getCategory().id());
                    firstStepAssignments.put(inputs.getAlternativesIds().get(altI), interval);

                    assignedAlternatives.get(inputs.getCategoryProfiles().get(catProfI+1).getCategory().id()).add(inputs.getAlternativesIds().get(altI));
                    categoriesFlows.put(inputs.getCategoryProfiles().get(catProfI+1).getCategory().id(),
                            categoriesFlows.getOrDefault(inputs.getCategoryProfiles().get(catProfI+1).getCategory().id(), BigDecimal.ZERO).
                                    add(inputs.getPositiveFlows().get(inputs.getAlternativesIds().get(altI))).
                                    subtract(inputs.getNegativeFlows().get(inputs.getAlternativesIds().get(altI))));
                    marked = true;
                } else if (isA1IndifferencedToA2(inputs.getPositiveFlows().get(inputs.getAlternativesIds().get(altI)),
                        inputs.getNegativeFlows().get(inputs.getAlternativesIds().get(altI)),
                        inputs.getPositiveFlows().get(inputs.getCategoryProfiles().get(catProfI).getUpperBound().getAlternative().id()),
                        inputs.getNegativeFlows().get(inputs.getCategoryProfiles().get(catProfI).getUpperBound().getAlternative().id()))) {
                    Map<String, String> interval = new LinkedHashMap<>();
                    interval.put(LOWER, inputs.getCategoryProfiles().get(catProfI).getCategory().id());
                    interval.put(UPPER, inputs.getCategoryProfiles().get(catProfI+1).getCategory().id());
                    firstStepAssignments.put(inputs.getAlternativesIds().get(altI), interval);

                    marked = true;
                    unassignedAlternatives.add(inputs.getAlternativesIds().get(altI));
                }
                if (marked) {
                    break;
                }
            }
            if (!marked) {
                assignedAlternatives.get(inputs.getCategoryProfiles().get(0).getCategory().id()).add(inputs.getAlternativesIds().get(altI));
                categoriesFlows.put(inputs.getCategoryProfiles().get(0).getCategory().id(),
                        categoriesFlows.getOrDefault(inputs.getCategoryProfiles().get(0).getCategory().id(), BigDecimal.ZERO).
                                add(inputs.getPositiveFlows().get(inputs.getAlternativesIds().get(altI))).
                                subtract(inputs.getNegativeFlows().get(inputs.getAlternativesIds().get(altI))));
                finalAssignments.put(inputs.getAlternativesIds().get(altI), inputs.getCategoryProfiles().get(0).getCategory().id());
                Map<String, String> interval = new LinkedHashMap<>();
                interval.put(LOWER, inputs.getCategoryProfiles().get(0).getCategory().id());
                interval.put(UPPER, inputs.getCategoryProfiles().get(0).getCategory().id());
                firstStepAssignments.put(inputs.getAlternativesIds().get(altI), interval);
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


                BigDecimal dkPositive = lengthT.multiply(inputs.getPositiveFlows().get(unassignedAlternatives.get(i)).
                        subtract(inputs.getNegativeFlows().get(unassignedAlternatives.get(i))).
                        subtract( categoriesFlows.get(categoryT)));
                BigDecimal dkNegative = categoriesFlows.get(categoryT1).
                        subtract(lengthT1.multiply(inputs.getPositiveFlows().get(unassignedAlternatives.get(i)).
                                subtract(inputs.getNegativeFlows().get(unassignedAlternatives.get(i)))));

                BigDecimal dk1 = BigDecimal.ZERO;
                if (lengthT.compareTo(BigDecimal.ZERO) > 0) {
                    dk1 = dkPositive.divide(lengthT, 6, RoundingMode.HALF_UP);
                }

                BigDecimal dk2 = BigDecimal.ZERO;
                if (lengthT1.compareTo(BigDecimal.ZERO) > 0) {
                    dk2 = dkNegative.divide(lengthT1, 6, RoundingMode.HALF_UP);
                }

                BigDecimal dk = dk1.subtract(dk2);

                if (dk.compareTo(inputs.getCutPoint()) > 0 || (dk.compareTo(inputs.getCutPoint()) == 0 && inputs.getAssignToABetterClass())) {
                    finalAssignments.put(unassignedAlternatives.get(i), categoryT1);
                } else {
                    finalAssignments.put(unassignedAlternatives.get(i), categoryT);
                }
            }
        }
    }

}
