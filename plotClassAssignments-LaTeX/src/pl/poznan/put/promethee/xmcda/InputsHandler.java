package pl.poznan.put.promethee.xmcda;

import org.xmcda.*;
import pl.poznan.put.promethee.exceptions.InputDataException;

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

        try {
            checkAndExtractAlternatives(inputs, xmcda, errors);
            checkAndExtractCategories(inputs, xmcda, errors);
            checkCategoriesRanking(inputs, xmcda, errors);
            sortCategories(inputs);
            checkAndExtractAssignments(inputs, xmcda, errors);
        } catch (InputDataException exception) {
            //Just catch the exceptions and skip other functions
        }

        return inputs;
    }

    protected static void checkAndExtractAlternatives(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        if (xmcda.alternatives.isEmpty()) {
            String errorMessage = "No alternatives list has been supplied";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        List<String> alternativesIds = xmcda.alternatives.getActiveAlternatives().stream().filter(a -> "alternatives".equals(a.getMarker())).map(
                Alternative::id).collect(Collectors.toList());
        if (alternativesIds.isEmpty()) {
            String errorMessage = "The alternatives list can not be empty";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        inputs.alternativesIds = alternativesIds;
    }

    protected static void checkAndExtractCategories(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        if (xmcda.categories.isEmpty()) {
            String errorMessage = "No categories has been supplied.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        if (xmcda.categories.size() == 1) {
            String errorMessage = "You should supply at least 2 categories.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        List<String> categories = xmcda.categories.getActiveCategories().stream().filter(a -> "categories".equals(a.getMarker())).map(
                Category::id).collect(Collectors.toList());
        inputs.setCategoriesIds(categories);
        if (categories.isEmpty()) {
            String errorMessage = "The category list can not be empty.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
    }

    protected static void checkCategoriesRanking(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        if (xmcda.categoriesValuesList.isEmpty()) {
            String errorMessage = "No categories values list has been supplied";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        if (xmcda.categoriesValuesList.size() > 1) {
            String errorMessage = "More than one categories values list has been supplied";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        CategoriesValues categoriesValuesList = xmcda.categoriesValuesList.get(0);
        if (!categoriesValuesList.isNumeric()) {
            String errorMessage = "Each of the categories ranks must be integer";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        Map<String, Integer> categoriesValues = new LinkedHashMap<>();

        checkRanks(categoriesValuesList, categoriesValues, inputs, xmcda, errors);
        findRankingDuplicates(categoriesValues, errors);
    }

    public static void checkRanks(CategoriesValues categoriesValuesList, Map<String, Integer> categoriesValues,
                                  Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
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
                String errorMessage = "Minimal rank should be equal to 1.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
            if (max != inputs.categoriesIds.size()) {
                String errorMessage = "Maximal rank should be equal to number of categories.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
            inputs.setCategoriesRanking(categoriesValues);
        } catch (InputDataException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = "An error occurred while checking the categories rank. Remember that each rank has to be integer.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
    }

    public static void findRankingDuplicates(Map<String, Integer> categoriesValues, ProgramExecutionResult errors) throws InputDataException {
        for (Map.Entry<String, Integer> categoryA : categoriesValues.entrySet()) {
            for (Map.Entry<String, Integer> categoryB : categoriesValues.entrySet()) {
                if (categoryA.getValue().equals(categoryB.getValue()) && !categoryA.getKey().equals(categoryB.getKey())) {
                    String errorMessage = "There can not be two categories with the same rank.";
                    errors.addError(errorMessage);
                    throw new InputDataException(errorMessage);
                }
            }
        }
    }

    protected static void sortCategories(Inputs inputs) {
        if (inputs.getCategoriesRanking() == null) {
            return;
        }
        Collections.sort(inputs.categoriesIds, (o1, o2) -> inputs.categoriesRanking.get(o1) - inputs.categoriesRanking.get(o2));
    }

    protected static void checkAndExtractAssignments(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        if (xmcda.alternativesAssignmentsList.size() != 1) {
            String errorMessage = "You need to provide one list of alternatives assignments.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        if (xmcda.alternativesAssignmentsList.get(0).isEmpty()) {
            String errorMessage = "Assignments list can not be empty.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        Set<String> categoriesSet = new HashSet<>();
        categoriesSet.addAll(inputs.getCategoriesIds());

        inputs.assignments = new HashMap<>();

        for (AlternativeAssignment<?> assignment : xmcda.alternativesAssignmentsList.get(0)) {
            Map<String, String> assignmentMap = new HashMap<>();

            if (assignment == null) {
                errors.addError(ALTERNATIVES_TAG_ERROR);
                throw new InputDataException(ALTERNATIVES_TAG_ERROR);
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
                                                              Map<String, String> assignmentMap, Inputs inputs, ProgramExecutionResult errors) throws InputDataException {
        if (assignment.getCategoryInterval() == null || assignment.getCategoryInterval().getLowerBound() == null ||
                assignment.getCategoryInterval().getUpperBound() == null) {
            errors.addError(ALTERNATIVES_TAG_ERROR);
            throw new InputDataException(ALTERNATIVES_TAG_ERROR);
        }

        String lowerCategory = assignment.getCategoryInterval().getLowerBound().id();
        String upperCategory = assignment.getCategoryInterval().getUpperBound().id();
        if (!categoriesSet.contains(lowerCategory) || !categoriesSet.contains(upperCategory)) {
            String errorMessage = "There are some categories in assignment list that were not be added to categories list.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        checkCategories(lowerCategory, upperCategory, inputs, errors);

        inputs.setAssignmentType("indirect");
        assignmentMap.put("LOWER", lowerCategory);
        assignmentMap.put("UPPER", upperCategory);
    }

    protected static void checkCategories(String lower, String upper, Inputs inputs, ProgramExecutionResult errors) throws InputDataException {
        Integer lowerRank = inputs.getCategoriesRanking().get(lower);
        Integer upperRank = inputs.getCategoriesRanking().get(upper);

        if (lowerRank == null || upperRank == null || lowerRank > upperRank) {
            String errorMessage = "Each lower category in assignments should have better mark then upper category for the same alternative.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
    }

    protected static void checkAndExtractDirectInAssignment(AlternativeAssignment assignment, Set<String> categoriesSet,
                                                            Map<String, String> assignmentMap, Inputs inputs, ProgramExecutionResult errors) throws InputDataException {
        if (assignment.getCategory().id() == null) {
            errors.addError(ALTERNATIVES_TAG_ERROR);
            throw new InputDataException(ALTERNATIVES_TAG_ERROR);
        }

        String category = assignment.getCategory().id();

        if (!categoriesSet.contains(category)) {
            String errorMessage = "There are some categories in assignment list that were not be added to categories list.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        if (inputs.getAssignmentType() == null) {
            inputs.setAssignmentType("direct");
        }
        assignmentMap.put("LOWER", category);
        assignmentMap.put("UPPER", category);
    }

    protected static void checkAlternativesInAssignments(Inputs inputs, ProgramExecutionResult errors) throws InputDataException {
        for (int i = 0; i < inputs.getAlternativesIds().size(); i++) {
            String alternativeId = inputs.getAlternativesIds().get(i);
            if (inputs.assignments.get(alternativeId) == null) {
                String errorMessage = "There are some missing alternatives in assignment list.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
        }
    }

}
