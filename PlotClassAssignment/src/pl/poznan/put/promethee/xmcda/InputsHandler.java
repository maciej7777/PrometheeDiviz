package pl.poznan.put.promethee.xmcda;

import org.xmcda.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Maciej Uniejewski on 2016-12-26.
 */
public class InputsHandler {

    private static final String ALTERNATIVES_TAG_ERROR = "There is a problem with an assignments list - it cannot be read.";

    private InputsHandler() {

    }

    public static class Inputs {
        private List<String> alternativesIds;
        private List<String> categoriesIds;
        private String assignmentType;
        private Map<String, Map<String, String>> assignments;
        private Map<String, Integer> categoriesRanking;

        public List<String> getAlternativesIds() {
            return alternativesIds;
        }

        public void setAlternativesIds(List<String> alternativesIds) {
            this.alternativesIds = alternativesIds;
        }

        public List<String> getCategoriesIds() {
            return categoriesIds;
        }

        public void setCategoriesIds(List<String> categoriesIds) {
            this.categoriesIds = categoriesIds;
        }

        public String getAssignmentType() {
            return assignmentType;
        }

        public void setAssignmentType(String assignmentType) {
            this.assignmentType = assignmentType;
        }

        public Map<String, Map<String, String>> getAssignments() {
            return assignments;
        }

        public void setAssignments(Map<String, Map<String, String>> assignments) {
            this.assignments = assignments;
        }

        public Map<String, Integer> getCategoriesRanking() {
            return categoriesRanking;
        }

        public void setCategoriesRanking(Map<String, Integer> categoriesRanking) {
            this.categoriesRanking = categoriesRanking;
        }
    }

    public static Inputs checkAndExtractInputs(XMCDA xmcda, ProgramExecutionResult xmcdaExecResults) {
        Inputs inputsDict = checkInputs(xmcda, xmcdaExecResults);
        if (xmcdaExecResults.isError())
            return null;
        return inputsDict;
    }

    protected static Inputs checkInputs(XMCDA xmcda, ProgramExecutionResult errors) {
        Inputs inputs = new Inputs();
        checkAndExtractAlternatives(inputs, xmcda, errors);
        checkAndExtractCategories(inputs, xmcda, errors);
        checkCategoriesRanking(inputs, xmcda, errors);
        sortCategories(inputs);
        checkAndExtractAssignments(inputs, xmcda, errors);

        return inputs;
    }

    protected static void checkAndExtractAlternatives(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.alternatives.isEmpty()) {
            errors.addError("No alternatives list has been supplied");
        } else {
            List<String> alternativesIds = xmcda.alternatives.getActiveAlternatives().stream().filter(a -> "alternatives".equals(a.getMarker())).map(
                    Alternative::id).collect(Collectors.toList());
            if (alternativesIds.isEmpty())
                errors.addError("The alternatives list can not be empty");

            inputs.alternativesIds = alternativesIds;
        }
    }

    protected static void checkAndExtractCategories(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.categories.isEmpty()) {
            errors.addError("No categories has been supplied.");
        } else if (xmcda.categories.size() == 1) {
            errors.addError("You should supply at least 2 categories.");
        } else {
            List<String> categories = xmcda.categories.getActiveCategories().stream().filter(a -> "categories".equals(a.getMarker())).map(
                    Category::id).collect(Collectors.toList());
            inputs.setCategoriesIds(categories);
            if (categories.isEmpty())
                errors.addError("The category list can not be empty.");
        }
    }

    protected static void checkCategoriesRanking(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.categoriesValuesList.isEmpty()) {
            errors.addError("No categories values list has been supplied");
        } else if (xmcda.categoriesValuesList.size() > 1) {
            errors.addError("More than one categories values list has been supplied");
        }
        CategoriesValues categoriesValuesList = xmcda.categoriesValuesList.get(0);
        if (!categoriesValuesList.isNumeric()) {
            errors.addError("Each of the categories ranks must be integer");
        }
        Map<String, Integer> categoriesValues = new LinkedHashMap<>();

        checkRanks(categoriesValuesList, categoriesValues, inputs, xmcda, errors);
        findRankingDuplicates(categoriesValues, errors);
    }

    public static void checkRanks(CategoriesValues categoriesValuesList, Map<String, Integer> categoriesValues,
                                  Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        try {
            CategoriesValues<Integer> categoriesValuesClass = categoriesValuesList.convertTo(Integer.class);
            xmcda.categoriesValuesList.set(0, categoriesValuesClass);

            int min = Integer.MAX_VALUE;
            int max = -1;

            for (Map.Entry<Category, LabelledQValues<Integer>> a : categoriesValuesClass.entrySet()) {
                if (a.getValue().get(0).getValue() < min) {
                    min = a.getValue().get(0).getValue();
                }
                if (a.getValue().get(0).getValue() > max) {
                    max = a.getValue().get(0).getValue();
                }
                categoriesValues.put(a.getKey().id(), a.getValue().get(0).getValue());
            }
            if (min != 1) {
                errors.addError("Minimal rank should be equal to 1.");
                return;
            }
            if (max != inputs.categoriesIds.size()) {
                errors.addError("Maximal rank should be equal to number of categories.");
                return;
            }

            inputs.setCategoriesRanking(categoriesValues);
        } catch (Exception e) {
            errors.addError("An error occurred: " + e + ". Remember that each rank has to be integer.");
        }
    }

    public static void findRankingDuplicates(Map<String, Integer> categoriesValues, ProgramExecutionResult errors) {
        for (Map.Entry<String, Integer> categoryA : categoriesValues.entrySet()) {
            for (Map.Entry<String, Integer> categoryB : categoriesValues.entrySet()) {
                if (categoryA.getValue() == categoryB.getValue() && categoryA.getKey() != categoryB.getKey()) {
                    errors.addError("There can not be two categories with the same rank.");
                    return;
                }
            }
        }
    }

    protected static void sortCategories(Inputs inputs) {
        Collections.sort(inputs.categoriesIds, (o1, o2) -> inputs.categoriesRanking.get(o1) - inputs.categoriesRanking.get(o2));
    }

    protected static void checkAndExtractAssignments(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.alternativesAssignmentsList.size() != 1) {
            errors.addError("You need to provide one list of alternatives assignments.");
            return;
        }

        if (xmcda.alternativesAssignmentsList.get(0).isEmpty()) {
            errors.addError("Assignments list can not be empty.");
            return;
        }

        Set<String> categoriesSet = new HashSet<>();
        categoriesSet.addAll(inputs.getCategoriesIds());

        inputs.assignments = new HashMap<>();

        for(AlternativeAssignment<?> assignment: xmcda.alternativesAssignmentsList.get(0)) {
            Map<String, String> assignmentMap = new HashMap<>();

            if (assignment == null) {
                errors.addError(ALTERNATIVES_TAG_ERROR);
                return;
            }
            String alternativeId = assignment.getAlternative().id();
            if (assignment.getCategory() == null) {
                checkAndExtractIndirectInAssignment(assignment, categoriesSet, assignmentMap, inputs, errors);
            } else {
                checkAndExtractDirectInAssignment(assignment, categoriesSet, assignmentMap, inputs, errors);
            }
            inputs.assignments.put(alternativeId, assignmentMap);
        }

        checkAlternativesInAssignments(inputs, errors);
    }

    protected static void checkAndExtractIndirectInAssignment(AlternativeAssignment assignment, Set<String> categoriesSet,
                                                            Map<String, String> assignmentMap, Inputs inputs, ProgramExecutionResult errors) {
        if (assignment.getCategoryInterval() == null || assignment.getCategoryInterval().getLowerBound() == null ||
                assignment.getCategoryInterval().getUpperBound() == null) {
            errors.addError(ALTERNATIVES_TAG_ERROR);
            return;
        }

        String lowerCategory = assignment.getCategoryInterval().getLowerBound().id();
        String upperCategory = assignment.getCategoryInterval().getUpperBound().id();
        if (!categoriesSet.contains(lowerCategory) || !categoriesSet.contains(upperCategory)) {
            errors.addError("There are some categories in assignment list that were not be added to categories list.");
            return;
        }
        checkCategories(lowerCategory, upperCategory, inputs, errors);

        inputs.setAssignmentType("indirect");
        assignmentMap.put("LOWER", lowerCategory);
        assignmentMap.put("UPPER", upperCategory);
    }

    protected static void checkCategories(String lower, String upper, Inputs inputs, ProgramExecutionResult errors) {
        Integer lowerRank = inputs.getCategoriesRanking().get(lower);
        Integer upperRank = inputs.getCategoriesRanking().get(upper);

        if (lowerRank == null || upperRank == null || lowerRank > upperRank) {
            errors.addError("Each lower category in assignments should have better mark then upper category for the same alternative.");
            return;
        }
    }

    protected static void checkAndExtractDirectInAssignment(AlternativeAssignment assignment, Set<String> categoriesSet,
                                                              Map<String, String> assignmentMap, Inputs inputs, ProgramExecutionResult errors) {
        if (assignment.getCategory().id() == null) {
            errors.addError(ALTERNATIVES_TAG_ERROR);
            return;
        }

        String category = assignment.getCategory().id();

        if (!categoriesSet.contains(category)) {
            errors.addError("There are some categories in assignment list that were not be added to categories list.");
            return;
        }
        if (inputs.getAssignmentType() == null) {
            inputs.setAssignmentType("direct");
        }
        assignmentMap.put("LOWER", category);
        assignmentMap.put("UPPER", category);
    }

    protected static void checkAlternativesInAssignments(Inputs inputs, ProgramExecutionResult errors) {
        for (int i = 0; i < inputs.getAlternativesIds().size(); i++) {
            String alternativeId = inputs.getAlternativesIds().get(i);
            if (inputs.assignments.get(alternativeId) == null) {
                errors.addError("There are some missing alternatives in assignment list.");
                return;
            }
        }
    }

}
