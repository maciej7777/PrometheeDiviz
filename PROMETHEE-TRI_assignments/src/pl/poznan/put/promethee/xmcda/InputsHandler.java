package pl.poznan.put.promethee.xmcda;

import org.xmcda.*;
import org.xmcda.Alternative;
import org.xmcda.CategoriesProfiles;
import org.xmcda.CategoriesValues;
import org.xmcda.Category;
import org.xmcda.CategoryProfile;
import org.xmcda.Criterion;
import org.xmcda.XMCDA;
import org.xmcda.utils.Coord;
import pl.poznan.put.promethee.exceptions.InputDataException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Maciej Uniejewski on 2016-11-11.
 */
public class InputsHandler {

    private InputsHandler() {

    }

    public static class Inputs {
        private List<String> alternativesIds;
        private List<String> categoriesIds;
        private List<String> profilesIds;
        private List<String> criteriaIds;
        private Boolean assignToABetterClass;
        private Boolean useMarginalValue;
        private Map<String, BigDecimal> criteriaWeights;
        private Map<String, Integer> categoriesRanking;
        private List<CategoryProfile> categoryProfiles;
        private Map<String, Map<String, Map<String, BigDecimal>>> partialPreferences;


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

        public List<String> getProfilesIds() {
            return profilesIds;
        }

        public void setProfilesIds(List<String> profilesIds) {
            this.profilesIds = profilesIds;
        }

        public Boolean getAssignToABetterClass() {
            return assignToABetterClass;
        }

        public void setAssignToABetterClass(Boolean assignToABetterClass) {
            this.assignToABetterClass = assignToABetterClass;
        }

        public Map<String, Integer> getCategoriesRanking() {
            return categoriesRanking;
        }

        public void setCategoriesRanking(Map<String, Integer> categoriesRanking) {
            this.categoriesRanking = categoriesRanking;
        }

        public List<CategoryProfile> getCategoryProfiles() {
            return categoryProfiles;
        }

        public void setCategoryProfiles(List<CategoryProfile> categoryProfiles) {
            this.categoryProfiles = categoryProfiles;
        }

        public Map<String, Map<String, Map<String, BigDecimal>>> getPartialPreferences() {
            return partialPreferences;
        }

        public void setPartialPreferences(Map<String, Map<String, Map<String, BigDecimal>>> partialPreferences) {
            this.partialPreferences = partialPreferences;
        }

        public List<String> getCriteriaIds() {
            return criteriaIds;
        }

        public void setCriteriaIds(List<String> criteriaIds) {
            this.criteriaIds = criteriaIds;
        }

        public Boolean getUseMarginalValue() {
            return useMarginalValue;
        }

        public void setUseMarginalValue(Boolean useMarginalValue) {
            this.useMarginalValue = useMarginalValue;
        }

        public Map<String, BigDecimal> getCriteriaWeights() {
            return criteriaWeights;
        }

        public void setCriteriaWeights(Map<String, BigDecimal> criteriaWeights) {
            this.criteriaWeights = criteriaWeights;
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
            checkAndExtractParameters(inputs, xmcda, errors);
            checkAndExtractCategories(inputs, xmcda, errors);
            checkCategoriesRanking(inputs, xmcda, errors);
            checkAndExtractProfilesIds(inputs, xmcda, errors);
            checkAndExtractCriteria(inputs, xmcda, errors);
            checkAndExtractPartialPreferences(inputs, xmcda, errors);
        } catch (InputDataException exception) {
            //Just catch the exceptions and skip other functions
        }

        return inputs;
    }

    protected static void checkAndExtractAlternatives(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        if (xmcda.alternatives.isEmpty()) {
            String errorMessage = "No alternatives list has been supplied.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        List<String> alternativesIds = xmcda.alternatives.getActiveAlternatives().stream().filter(a -> "alternatives".equals(a.getMarker())).map(
                Alternative::id).collect(Collectors.toList());
        if (alternativesIds.isEmpty()) {
            String errorMessage = "The alternatives list can not be empty.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        inputs.setAlternativesIds(alternativesIds);
    }

    protected static void checkAndExtractParameters(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {

        if (xmcda.programParametersList.size() > 1) {
            String errorMessage = "Only one programParameter is expected.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        if (xmcda.programParametersList.isEmpty()) {
            String errorMessage = "No programParameter found.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        if (xmcda.programParametersList.get(0).size() != 2) {
            String errorMessage = "Parameter's list must contain exactly two elements.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        checkAndExtractAssignToABetterClass(inputs, xmcda, errors);
        checkAndExtractUseMarginalValue(inputs, xmcda, errors);
    }

    protected static void checkAndExtractAssignToABetterClass(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        Boolean assignToABetterClass;

        final ProgramParameter<?> prgParam1 = xmcda.programParametersList.get(0).get(0);
        if (!"assignToABetterClass".equalsIgnoreCase(prgParam1.id())) {
            String errorMessage = String.format("Invalid parameter w/ id '%s'", prgParam1.id());
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        if (prgParam1.getValues() == null || (prgParam1.getValues() != null && prgParam1.getValues().size() != 1)) {
            String errorMessage = "Parameter assignToABetterClass must have a single (boolean) value only";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        try {
            assignToABetterClass = (Boolean) prgParam1.getValues().get(0).getValue();
            if (assignToABetterClass == null) {
                String errorMessage = "Invalid value for parameter assignToABetterClass, it must be true or false.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
            inputs.setAssignToABetterClass(assignToABetterClass);
        } catch (InputDataException e) {
            throw e;
        } catch (Exception exception) {
            String err = "Invalid value for parameter assignToABetterClass, it must be true or false.";
            errors.addError(err);
            throw new InputDataException(err);
        }
    }

    protected static void checkAndExtractUseMarginalValue(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        Boolean useMarginalValue;

        final ProgramParameter<?> prgParam = xmcda.programParametersList.get(0).get(1);
        if (!"useMarginalValue".equalsIgnoreCase(prgParam.id())) {
            String errorMessage = String.format("Invalid parameter w/ id '%s'", prgParam.id());
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        if (prgParam.getValues() == null || (prgParam.getValues() != null && prgParam.getValues().size() != 1)) {
            String errorMessage = "Parameter useMarginalValue must have a single (boolean) value only";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        try {
            useMarginalValue = (Boolean) prgParam.getValues().get(0).getValue();
            if (useMarginalValue == null) {
                String errorMessage = "Invalid value for parameter useMarginalValue, it must be true or false.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
            inputs.setUseMarginalValue(useMarginalValue);
        } catch (InputDataException e) {
            throw e;
        } catch (Exception exception) {
            String err = "Invalid value for parameter useMarginalValue, it must be true or false.";
            errors.addError(err);
            throw new InputDataException(err);
        }
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
            if (max != inputs.getCategoriesIds().size()) {
                String errorMessage = "Maximal rank should be equal to number of categories.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }

            for (Map.Entry<String, Integer> categoryA : categoriesValues.entrySet()) {
                for (Map.Entry<String, Integer> categoryB : categoriesValues.entrySet()) {
                    if (categoryA.getValue().intValue() == categoryB.getValue() && !categoryA.getKey().equals(categoryB.getKey())) {
                        String errorMessage = "There can not be two categories with the same rank.";
                        errors.addError(errorMessage);
                        throw new InputDataException(errorMessage);
                    }
                }
            }
            inputs.setCategoriesRanking(categoriesValues);

        } catch (InputDataException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = "An error occurred: " + e + ". Remember that each rank has to be integer.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
    }

    protected static void checkAndExtractProfilesIds(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        inputs.setProfilesIds(new ArrayList<>());

        if (xmcda.categoriesProfilesList.isEmpty()) {
            String errorMessage = "No categories profiles list has been supplied";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        if (xmcda.categoriesProfilesList.size() > 1) {
            String errorMessage = "You can not supply more then 1 categories profiles list";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        inputs.setCategoryProfiles(new ArrayList<>());

        CategoriesProfiles categoriesProfiles = xmcda.categoriesProfilesList.get(0);
        if (inputs.getCategoriesRanking().size() != categoriesProfiles.size()) {
            String errorMessage = "There is a problem with categories rank list or categories profiles list. Each category has to be added to categories profiles list.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        for (Object profile : categoriesProfiles) {
            CategoryProfile tmpProfile = (CategoryProfile) profile;
            if (!"central".equalsIgnoreCase(tmpProfile.getType().name())) {
                String errorMessage = "There is a problem with categories rank list or categories profiles list. You need to provide central profiles for categories.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
            inputs.getCategoryProfiles().add(tmpProfile);
        }

        Collections.sort(inputs.getCategoryProfiles(), (left, right) -> Integer.compare(inputs.getCategoriesRanking().get(left.getCategory().id()), inputs.getCategoriesRanking().get(right.getCategory().id())));


        inputs.setProfilesIds(new ArrayList<>());
        checkAndExtractCentralProfilesIds(errors, inputs);
    }

    protected static void checkAndExtractCentralProfilesIds(ProgramExecutionResult errors, InputsHandler.Inputs inputs) throws InputDataException {
        for (int j = 0; j < inputs.getCategoryProfiles().size(); j++) {
            if (inputs.getCategoryProfiles().get(j).getCentralProfile() != null) {
                inputs.getProfilesIds().add(inputs.getCategoryProfiles().get(j).getCentralProfile().getAlternative().id());
            } else {
                String errorMessage = "There is a problem with categories profiles. You need to provide central for categories.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
        }
    }

    protected static void checkAndExtractCriteria(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        if (xmcda.criteria.getActiveCriteria().isEmpty()) {
            String errorMessage = "You need to provide a not empty criteria list.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        inputs.setCriteriaIds(xmcda.criteria.getActiveCriteria().stream().filter(a -> "criteria".equals(a.getMarker())).map(
                Criterion::id).collect(Collectors.toList()));

        checkAndExtractCriteriaWeights(inputs, xmcda, errors);
    }

    protected static void checkAndExtractCriteriaWeights(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        if (xmcda.criteriaValuesList.size() != 1) {
            String errorMessage = "You need to provide 1 alternatives values list for criteria weights.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        checkAndExtractCriteriaFromList(inputs, xmcda, errors);
    }

    protected static void checkAndExtractCriteriaFromList(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {

        inputs.setCriteriaWeights(new LinkedHashMap<>());

        org.xmcda.CriteriaValues criteriaWeights = xmcda.criteriaValuesList.get(0);
        if (!criteriaWeights.isNumeric()) {
            String errorMessage = "Each criterion weight must have numeric type.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        try {
            Map<Criterion, LabelledQValues<Double>> criteriaWeightsMap = criteriaWeights;
            for (Map.Entry<Criterion, LabelledQValues<Double>> weight : criteriaWeightsMap.entrySet()) {
                Double tmpValue = weight.getValue().get(0).convertToDouble().getValue();
                BigDecimal bigDecimalValue = BigDecimal.valueOf(tmpValue);
                inputs.getCriteriaWeights().put(weight.getKey().id(), bigDecimalValue);
            }
        } catch (Exception exception) {
            String errorMessage = "An error occurred: " + exception + ". Each flow must have numeric type.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        checkMissingValuesInCriteriaWeights(inputs, errors);
    }

    protected static void checkMissingValuesInCriteriaWeights(Inputs inputs, ProgramExecutionResult errors) throws InputDataException {
        for (int j = 0; j < inputs.getCriteriaIds().size(); j++) {
            String criterionId = inputs.getCriteriaIds().get(j);
            if (!inputs.getCriteriaWeights().containsKey(criterionId)) {
                String errorMessage = "There are some missing values in criteria weights.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
        }
    }

    protected static void checkAndExtractPartialPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        if (xmcda.alternativesMatricesList.size() != 1) {
            String errorMessage = "You need to provide 1 alternatives values list for partial preferences.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        checkAndExtractPreferences(inputs, xmcda, errors);
    }

    protected static void checkAndExtractPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {

        inputs.partialPreferences = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        AlternativesMatrix<Double> matrix = (AlternativesMatrix<Double>) xmcda.alternativesMatricesList.get(0);

        List<String> alternativesAndProfiles = new ArrayList<>();
        alternativesAndProfiles.addAll(inputs.getAlternativesIds());
        alternativesAndProfiles.addAll(inputs.getProfilesIds());

        for (String alternative : inputs.getAlternativesIds()) {
            for (String profile : inputs.getProfilesIds()) {
                putPreferencesIntoMap(inputs, errors, matrix, alternative, profile);
            }
        }

        for (String profile : inputs.getProfilesIds()) {
            for (String alternativeProfile : alternativesAndProfiles) {
                putPreferencesIntoMap(inputs, errors, matrix, profile, alternativeProfile);
            }
        }
    }

    private static void putPreferencesIntoMap(Inputs inputs, ProgramExecutionResult errors,
                                              AlternativesMatrix<Double> matrix, String first, String second) throws InputDataException {
        inputs.partialPreferences.putIfAbsent(first, new LinkedHashMap<>());
        inputs.partialPreferences.get(first).putIfAbsent(second, new LinkedHashMap<>());
        Alternative alt1 = new Alternative(first);
        Alternative alt2 = new Alternative(second);
        Coord<Alternative, Alternative> coord = new Coord<>(alt1, alt2);
        QualifiedValues<Double> values = matrix.getOrDefault(coord, null);

        if (values == null) {
            if (!first.equals(second)) {
                String errorMessage = "List of partial preferences does not contain value for coord (" + first + "," + second + ")";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
            return;
        }
        if (values.size() != inputs.criteriaIds.size()) {
            String errorMessage = "List of partial preferences does not contain correct criteria list";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        for (QualifiedValue<Double> value : values) {
            if (inputs.criteriaIds.contains(value.id())) {
                if (inputs.partialPreferences.get(first).get(second).containsKey(value.id())) {
                    String errorMessage = "List of partial preferences contains duplicates of criteria";
                    errors.addError(errorMessage);
                    throw new InputDataException(errorMessage);
                }
                BigDecimal bigDecimalValue = BigDecimal.valueOf(value.getValue());
                inputs.partialPreferences.get(first).get(second).put(value.id(), bigDecimalValue);
            }
        }
    }




/*    protected static void checkAndExtractAlternativesFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.alternativesValuesList.size() != 1) {
            errors.addError("You need to provide 1 alternatives values list for net flows.");
            return;
        }
        checkAndExtractNetFlows(inputs, xmcda, errors);
    }

    protected static void checkAndExtractNetFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {

        inputs.setFlows(new LinkedHashMap<>());

        AlternativesValues netFlows = xmcda.alternativesValuesList.get(0);
        if (!netFlows.isNumeric()) {
            errors.addError("Each flow must have numeric type.");
            return;
        }

        try {
            Map<Alternative, LabelledQValues<Double>> netFlowsMap = netFlows;
            for (Map.Entry<Alternative, LabelledQValues<Double>> flow : netFlowsMap.entrySet()) {
                Double tmpValue = flow.getValue().get(0).convertToDouble().getValue();
                BigDecimal bigDecimalValue = BigDecimal.valueOf(tmpValue);
                inputs.getFlows().put(flow.getKey().id(), bigDecimalValue);
            }
        } catch (Exception exception) {
            errors.addError("An error occurred: " + exception + ". Each flow must have numeric type.");
            return;
        }

        checkMissingValuesInCriteriaWeights(inputs, errors);
    }

    protected static void checkMissingValuesInCriteriaWeights(Inputs inputs, ProgramExecutionResult errors) {
        for (int j = 0; j < inputs.getAlternativesIds().size(); j++) {
            String alternativeId = inputs.getAlternativesIds().get(j);
            if (!inputs.getFlows().containsKey(alternativeId)) {
                errors.addError("There are some missing values in flows.");
                return;
            }
        }

        BigDecimal lastFlow = BigDecimal.valueOf(-Double.MAX_VALUE);
        for (int i = 0; i < inputs.getProfilesIds().size(); i++) {
            String profileId = inputs.getProfilesIds().get(i);
            if (!inputs.getFlows().containsKey(profileId)) {
                errors.addError("There are some missing values in flows.");
                return;
            }
            BigDecimal currentFlow = inputs.getFlows().get(profileId);
            if (currentFlow.compareTo(lastFlow) <= 0) {
                errors.addError("There are some errors in profiles flows. Better profiles should have bigger flows.");
                return;
            }
            lastFlow = currentFlow;
        }
    }*/
}
