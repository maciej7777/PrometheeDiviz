package pl.poznan.put.promethee.xmcda;

import org.xmcda.*;
import org.xmcda.Alternative;
import org.xmcda.AlternativesValues;
import org.xmcda.CategoriesProfiles;
import org.xmcda.CategoriesValues;
import org.xmcda.Category;
import org.xmcda.CategoryProfile;
import org.xmcda.XMCDA;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Maciej Uniejewski on 2016-11-01.
 */
public class InputsHandler {

    private InputsHandler() {

    }

    public enum ComparisonWithProfiles {
        CENTRAL("central"),
        BOUNDING("bounding");
        private String label;

        private ComparisonWithProfiles(String operatorLabel) {
            label = operatorLabel;
        }

        public final String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }

        public static ComparisonWithProfiles fromString(String operatorLabel) {
            if (operatorLabel == null)
                throw new NullPointerException("ComparisonWithProfiles is null.");
            for (ComparisonWithProfiles op : ComparisonWithProfiles.values()) {
                if (op.toString().equals(operatorLabel))
                    return op;
            }
            throw new IllegalArgumentException("No enum comparisonWithProfiles with label " + operatorLabel);
        }
    }

    public static class Inputs {
        private List<String> alternativesIds;
        private List<String> categoriesIds;
        private List<String> profilesIds;
        private Map<String, Double> positiveFlows;
        private Map<String, Double> negativeFlows;
        private Map<String, Integer> categoriesRanking;
        private List<CategoryProfile> categoryProfiles;
        private ComparisonWithProfiles profilesType;
        private List<String> criteriaIds;
        private Map<String, String> criteriaPreferencesDirection;
        private Map<String, Map<String, Double>> profilesPerformance;

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

        public Map<String, Double> getPositiveFlows() {
            return positiveFlows;
        }

        public void setPositiveFlows(Map<String, Double> positiveFlows) {
            this.positiveFlows = positiveFlows;
        }

        public Map<String, Double> getNegativeFlows() {
            return negativeFlows;
        }

        public void setNegativeFlows(Map<String, Double> negativeFlows) {
            this.negativeFlows = negativeFlows;
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

        public ComparisonWithProfiles getProfilesType() {
            return profilesType;
        }

        public void setProfilesType(ComparisonWithProfiles profilesType) {
            this.profilesType = profilesType;
        }

        private List<String> getCriteriaIds() {
            return criteriaIds;
        }

        private void setCriteriaIds(List<String> criteriaIds) {
            this.criteriaIds = criteriaIds;
        }

        private Map<String, String> getCriteriaPreferencesDirection() {
            return criteriaPreferencesDirection;
        }

        private void setCriteriaPreferencesDirection(Map<String, String> criteriaPreferencesDirection) {
            this.criteriaPreferencesDirection = criteriaPreferencesDirection;
        }

        private Map<String, Map<String, Double>> getProfilesPerformance() {
            return profilesPerformance;
        }

        private void setProfilesPerformance(Map<String, Map<String, Double>> profilesPerformance) {
            this.profilesPerformance = profilesPerformance;
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
        checkAndExtractParameters(inputs, xmcda, errors);
        checkAndExtractCategories(inputs, xmcda, errors);
        checkCategoriesRanking(inputs, xmcda, errors);
        checkAndExtractProfilesIds(inputs, xmcda, errors);
        checkAndExtractAlternativesFlows(inputs, xmcda, errors);
        checkAndExtractCriteria(inputs, xmcda, errors);
        checkAndExtractProfilesPerformance(inputs, xmcda, errors);
        checkDominanceProperty(inputs, errors);

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

            inputs.setAlternativesIds(alternativesIds);
        }
    }

    protected static void checkAndExtractParameters(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {

        if (xmcda.programParametersList.size() > 1) {
            errors.addError("Only one programParameter is expected.");
            return;
        }
        if (xmcda.programParametersList.isEmpty()) {
            errors.addError("No programParameter found.");
            return;
        }
        if (xmcda.programParametersList.get(0).size() != 1) {
            errors.addError("Parameter's list must contain exactly one element.");
            return;
        }

        checkAndExtractProfilesType(inputs, xmcda, errors);
    }

    protected static void checkAndExtractProfilesType(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        ComparisonWithProfiles profilesType;

        final ProgramParameter<?> prgParam = xmcda.programParametersList.get(0).get(0);
        if (!"comparisonWithProfiles".equalsIgnoreCase(prgParam.id())) {
            errors.addError(String.format("Invalid parameter w/ id '%s'", prgParam.id()));
            return;
        }
        if (prgParam.getValues() == null || (prgParam.getValues() != null && prgParam.getValues().size() != 1)) {
            errors.addError("Parameter comparisonWithProfiles must have a single (label) value only");
            return;
        }
        try {
            final String operatorValue = (String) prgParam.getValues().get(0).getValue();
            profilesType = ComparisonWithProfiles.fromString(operatorValue);
        } catch (Exception exception) {
            StringBuilder validValues = new StringBuilder();
            for (ComparisonWithProfiles op : ComparisonWithProfiles.values()) {
                validValues.append(op.getLabel()).append(", ");
            }
            String err = "Invalid value for parameter comparisonWithProfiles, it must be a label, ";
            err += "possible values are: " + validValues.substring(0, validValues.length() - 2);
            errors.addError(err);
            profilesType = null;
        }
        inputs.setProfilesType(profilesType);
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
            if (max != inputs.getCategoriesIds().size()) {
                errors.addError("Maximal rank should be equal to number of categories.");
                return;
            }

            for (Map.Entry<String, Integer> categoryA : categoriesValues.entrySet()) {
                for (Map.Entry<String, Integer> categoryB : categoriesValues.entrySet()) {
                    if (categoryA.getValue() == categoryB.getValue() && categoryA.getKey() != categoryB.getKey()) {
                        errors.addError("There can not be two categories with the same rank.");
                        return;
                    }
                }
            }

            inputs.setCategoriesRanking(categoriesValues);
        } catch (Exception e) {
            errors.addError("An error occurred: " + e + ". Remember that each rank has to be integer.");
        }
    }

    protected static void checkAndExtractProfilesIds(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        inputs.setProfilesIds(new ArrayList<>());

        if (xmcda.categoriesProfilesList.isEmpty()) {
            errors.addError("No categories profiles list has been supplied.");
        }
        if (xmcda.categoriesProfilesList.size() > 1) {
            errors.addError("You can not supply more then 1 categories profiles list.");
        }

        if (inputs.getCategoriesRanking() == null) {
            return;
        }

        inputs.setCategoryProfiles(new ArrayList<>());

        CategoriesProfiles categoriesProfiles = xmcda.categoriesProfilesList.get(0);
        if (inputs.getCategoriesRanking().size() != categoriesProfiles.size()) {
            errors.addError("There is a problem with categories rank list or categories profiles list. Each category has to be added to categories profiles list.");
            return;
        }

        for (Object profile : categoriesProfiles) {
            CategoryProfile tmpProfile = (CategoryProfile) profile;
            if (!tmpProfile.getType().name().equalsIgnoreCase(inputs.getProfilesType().toString())) {
                errors.addError("There is a problem with categories rank list or categories profiles list. Each category has to be added to categories profiles list.");
                return;
            } else {
                inputs.getCategoryProfiles().add(tmpProfile);
            }
        }

        Collections.sort(inputs.getCategoryProfiles(), (left, right) -> Integer.compare(
                inputs.getCategoriesRanking().get(left.getCategory().id()), inputs.getCategoriesRanking().get(right.getCategory().id())));

        if ("BOUNDING".equalsIgnoreCase(inputs.getProfilesType().toString())) {
            checkAndExtractBoundaryProfilesIds(errors, inputs);
        } else if ("CENTRAL".equalsIgnoreCase(inputs.getProfilesType().toString())) {
            checkAndExtractCentralProfilesIds(errors, inputs);
        }
    }

    protected static void checkAndExtractBoundaryProfilesIds(ProgramExecutionResult errors, Inputs inputs) {
        for (int j = 0; j < inputs.getCategoryProfiles().size() - 1; j++) {
            if (inputs.getCategoryProfiles().get(j).getUpperBound() != null && inputs.getCategoryProfiles().get(j + 1).getLowerBound() != null) {
                inputs.getProfilesIds().add(inputs.getCategoryProfiles().get(j).getUpperBound().getAlternative().id());
                if (!inputs.getCategoryProfiles().get(j).getUpperBound().getAlternative().id().equals(inputs.getCategoryProfiles().get(j + 1).getLowerBound().getAlternative().id())) {
                    errors.addError("Each two closest categories have to be separated by same boundary profile.");
                    return;
                }
            } else {
                errors.addError("There is a problem with categories profiles. You need to provide boundary profiles for categories");
                return;
            }
        }
    }

    protected static void checkAndExtractCentralProfilesIds(ProgramExecutionResult errors, InputsHandler.Inputs inputs) {
        for (int j = 0; j < inputs.getCategoryProfiles().size(); j++) {
            if (inputs.getCategoryProfiles().get(j).getCentralProfile() != null) {
                inputs.getProfilesIds().add(inputs.getCategoryProfiles().get(j).getCentralProfile().getAlternative().id());
            } else {
                errors.addError("There is a problem with categories profiles. You need to provide central for categories.");
                break;
            }
        }
    }

    protected static void checkAndExtractAlternativesFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.alternativesValuesList.size() != 2) {
            errors.addError("You need to provide 2 alternatives values lists - one for positive flows  and one for negative flows.");
            return;
        }
        checkAndExtractPositiveFlows(inputs, xmcda, errors);
        checkAndExtractNegativeFlows(inputs, xmcda, errors);
        checkProfilesFlows(inputs, errors);

    }

    protected static void checkAndExtractPositiveFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {

        inputs.setPositiveFlows(new LinkedHashMap<>());

        AlternativesValues positiveFlows = xmcda.alternativesValuesList.get(0);
        if (!positiveFlows.isNumeric()) {
            errors.addError("Each flow must have numeric type.");
            return;
        }

        try {
            Map<Alternative, LabelledQValues<Double>> positiveFlowsMap = positiveFlows;
            for (Map.Entry<Alternative, LabelledQValues<Double>> flow : positiveFlowsMap.entrySet()) {
                Double tmpValue = flow.getValue().get(0).convertToDouble().getValue();
                inputs.getPositiveFlows().put(flow.getKey().id(), tmpValue);
            }
        } catch (Exception exception) {
            errors.addError("An error occurred: " + exception + ". Each flow must have numeric type.");
            return;
        }

        checkMissingValuesInPositiveFlows(inputs, errors);
    }

    protected static void checkMissingValuesInPositiveFlows(Inputs inputs, ProgramExecutionResult errors) {
        for (int j = 0; j < inputs.getAlternativesIds().size(); j++) {
            String alternativeId = inputs.getAlternativesIds().get(j);
            if (!inputs.getPositiveFlows().containsKey(alternativeId)) {
                errors.addError("There are some missing values in positive flows.");
                return;
            }
        }

        for (int i = 0; i < inputs.getProfilesIds().size(); i++) {
            String profileId = inputs.getProfilesIds().get(i);
            if (!inputs.getPositiveFlows().containsKey(profileId)) {
                errors.addError("There are some missing values in positive flows.");
                return;
            }
        }
    }

    protected static void checkAndExtractNegativeFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {

        inputs.setNegativeFlows(new LinkedHashMap<>());

        AlternativesValues negativeFlows = xmcda.alternativesValuesList.get(1);
        if (!negativeFlows.isNumeric()) {
            errors.addError("Each flow must have numeric type.");
            return;
        }

        try {
            Map<Alternative, LabelledQValues<Double>> negativeFlowsMap = negativeFlows;
            for (Map.Entry<Alternative, LabelledQValues<Double>> flow : negativeFlowsMap.entrySet()) {
                Double tmpValue = flow.getValue().get(0).convertToDouble().getValue();
                inputs.getNegativeFlows().put(flow.getKey().id(), tmpValue);
            }
        } catch (Exception exception) {
            errors.addError("An error occurred: " + exception + ". Each flow must have numeric type.");
            return;
        }

        checkMissingValuesInNegativeFlows(inputs, errors);
    }

    protected static void checkMissingValuesInNegativeFlows(Inputs inputs, ProgramExecutionResult errors) {
        for (int j = 0; j < inputs.getAlternativesIds().size(); j++) {
            String alternativeId = inputs.getAlternativesIds().get(j);
            if (!inputs.getNegativeFlows().containsKey(alternativeId)) {
                errors.addError("There are some missing values in negative flows.");
                return;
            }
        }

        for (int i = 0; i < inputs.getProfilesIds().size(); i++) {
            String profileId = inputs.getProfilesIds().get(i);
            if (!inputs.getNegativeFlows().containsKey(profileId)) {
                errors.addError("There are some missing values in negative flows.");
                return;
            }
        }
    }

    protected static void checkProfilesFlows(Inputs inputs, ProgramExecutionResult errors) {
        if (errors.size() != 0) {
            return;
        }
        for (int i = 1; i < inputs.profilesIds.size(); i++) {
            String profile1 = inputs.profilesIds.get(i-1);
            String profile2 = inputs.profilesIds.get(i);
            if (!isPreffered(profile2, profile1, inputs)) {
                errors.addError("There are some errors in profiles flows. Better profiles need to be preferred to the worse ones.");
                return;
            }
        }
    }


    private static boolean preferenceCondition1(double a1PositiveFlow, double a1NegativeFlow, double a2PositiveFlow, double a2NegativeFlow) {
        return a1PositiveFlow > a2PositiveFlow && a1NegativeFlow < a2NegativeFlow;
    }

    private static boolean preferenceCondition2(double a1PositiveFlow, double a1NegativeFlow, double a2PositiveFlow, double a2NegativeFlow) {
        return a1PositiveFlow == a2PositiveFlow && a1NegativeFlow < a2NegativeFlow;
    }

    private static boolean preferenceCondition3(double a1PositiveFlow, double a1NegativeFlow, double a2PositiveFlow, double a2NegativeFlow) {
        return a1PositiveFlow > a2PositiveFlow && a1NegativeFlow == a2NegativeFlow;
    }

    private static boolean isPreffered(String profile1, String profile2, Inputs inputs) {

        Double positiveFlowProfile1 = inputs.getPositiveFlows().get(profile1);
        Double positiveFlowProfile2 = inputs.getPositiveFlows().get(profile2);
        Double negativeFlowProfile1 = inputs.getNegativeFlows().get(profile1);
        Double negativeFlowProfile2 = inputs.getNegativeFlows().get(profile2);

        if (preferenceCondition1(positiveFlowProfile1, negativeFlowProfile1, positiveFlowProfile2, negativeFlowProfile2) ||
                preferenceCondition2(positiveFlowProfile1, negativeFlowProfile1, positiveFlowProfile2, negativeFlowProfile2) ||
                preferenceCondition3(positiveFlowProfile1, negativeFlowProfile1, positiveFlowProfile2, negativeFlowProfile2)) {
            return true;
        }
        return false;
    }

    protected static void checkAndExtractCriteria(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.criteria.getActiveCriteria().isEmpty()) {
            errors.addError("You need to provide a not empty criteria list.");
            return;
        }
        inputs.setCriteriaIds(xmcda.criteria.getActiveCriteria().stream().filter(a -> "criteria".equals(a.getMarker())).map(
                Criterion::id).collect(Collectors.toList()));

        checkAndExtractCriteriaPreferencesDirection(inputs, xmcda, errors);
    }

    protected static void checkAndExtractCriteriaPreferencesDirection(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (inputs.getCriteriaIds() == null || inputs.getCriteriaIds().isEmpty()) {
            return;
        }

        if (xmcda.criteriaScalesList.size() != 1) {
            errors.addError("You need to provide one not empty criteria scales list.");
            return;
        }

        inputs.setCriteriaPreferencesDirection(new HashMap<>());

        CriteriaScales criteriaDirection = xmcda.criteriaScalesList.get(0);
        for (Map.Entry<Criterion, CriterionScales> criterionEntry : criteriaDirection.entrySet()) {
            try {
                @SuppressWarnings("unchecked")
                QuantitativeScale<String> scale = (QuantitativeScale<String>) criterionEntry.getValue().get(0);
                String scaleDirection = scale.getPreferenceDirection().name();
                if (!"min".equalsIgnoreCase(scaleDirection) && !"max".equalsIgnoreCase(scaleDirection)) {
                    errors.addError("Each criterion scale must be a label \"min\" or \"max\".");
                }
                inputs.getCriteriaPreferencesDirection().put(criterionEntry.getKey().id(), scaleDirection);
            } catch (Exception e) {
                errors.addError("Each criterion scale must be a label \"min\" or \"max\".");
                return;
            }
        }
    }

    protected static void checkAndExtractProfilesPerformance(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (inputs.getProfilesIds() == null || inputs.getProfilesIds().isEmpty() || inputs.getProfilesIds().get(0).isEmpty()) {
            return;
        }
        if (xmcda.performanceTablesList.size() != 1) {
            errors.addError("You need to provide exactly 1 profile performances lists.");
            return;
        }

        inputs.setProfilesPerformance(new HashMap<>());

        @SuppressWarnings("rawtypes")
        PerformanceTable p = xmcda.performanceTablesList.get(0);

        if (p.hasMissingValues()) {
            errors.addError("The performance table has missing values.");
            return;
        }
        if (!p.isNumeric()) {
            errors.addError("The performance table must contain numeric values only");
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            PerformanceTable<Double> perfTable = p.asDouble();
            xmcda.performanceTablesList.set(0, perfTable);
        } catch (Exception e) {
            final String msg = "Error when converting the performance table's value to Double, reason:";
            errors.addError(Utils.getMessage(msg, e));
            return;
        }

        @SuppressWarnings("unchecked")
        PerformanceTable<Double> profilesPerformance = (PerformanceTable<Double>) xmcda.performanceTablesList.get(0);
        for (Alternative alternative : profilesPerformance.getAlternatives()) {
            if (!inputs.getProfilesIds().contains(alternative.id())) {
                continue;
            }
            for (Criterion criterion : profilesPerformance.getCriteria()) {
                if (!inputs.getCriteriaIds().contains(criterion.id())) {
                    continue;
                }

                Double value = profilesPerformance.getValue(alternative, criterion);
                inputs.getProfilesPerformance().putIfAbsent(alternative.id(), new LinkedHashMap<>());
                inputs.getProfilesPerformance().get(alternative.id()).put(criterion.id(), value);
            }
        }
    }

    protected static void checkDominanceProperty(Inputs inputs, ProgramExecutionResult errors) {

        for (String criterionId : inputs.getCriteriaIds()) {
            for (int i = 0; i < inputs.getProfilesIds().size() - 1; i++) {
                String profileId = inputs.getProfilesIds().get(i);
                String nextProfileId = inputs.getProfilesIds().get(i + 1);

                Map<String, Double> firstProfilePerformance = inputs.getProfilesPerformance().get(profileId);
                Map<String, Double> secondProfilePerformance = inputs.getProfilesPerformance().get(nextProfileId);
                String preferenceDirection = inputs.getCriteriaPreferencesDirection().get(criterionId);
                if (firstProfilePerformance == null || secondProfilePerformance == null || preferenceDirection == null) {
                    errors.addError("There was a problem when checking profiles preferences. Profiles need to fulfill the dominance condition on each criterion.");
                    return;
                }
                Double firstProfileCriterionPerformance = firstProfilePerformance.get(criterionId);
                Double secondProfileCriterionPerformance = secondProfilePerformance.get(criterionId);
                if (firstProfileCriterionPerformance == null || secondProfileCriterionPerformance == null) {
                    errors.addError("There was a problem when checking profiles preferences. Profiles need to fulfill the dominance condition on each criterion.");
                    return;
                }
                if (("max".equalsIgnoreCase(preferenceDirection) && firstProfileCriterionPerformance.compareTo(secondProfileCriterionPerformance) > 0) ||
                        ("min".equalsIgnoreCase(preferenceDirection) && firstProfileCriterionPerformance.compareTo(secondProfileCriterionPerformance) < 0)) {
                    errors.addError("Profiles need to fulfill the dominance condition on each criterion.");
                    return;
                }
            }
        }
    }



}
