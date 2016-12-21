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

    public static class Inputs {
        public List<String> alternativesIds;
        public List<String> categoriesIds;
        public List<String> profilesIds;
        public Map<String, Double> positiveFlows;
        public Map<String, Double> negativeFlows;
        public Double cutPoint;
        public Boolean assignToABetterClass;
        public Map<String, Integer> categoriesRanking;
        public List<CategoryProfile> categoryProfiles;
        public List<String> criteriaIds;
        public Map<String, String> criteriaPreferencesDirection;
        public Map<String, Double> criteriaPreferenceThresholds;
        public Map<String, Map<String, Double>> profilesPerformance;
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
        checkAndExtractParameters(inputs, xmcda, errors);
        checkCategoriesRanking(inputs, xmcda, errors);
        checkAndExtractProfilesIds(inputs, xmcda, errors);
        checkAndExtractAlternativesFlows(inputs, xmcda, errors);
        checkAndExtractCriteria(inputs, xmcda, errors);
        checkAndExtractProfilesPerformance(inputs, xmcda, errors);

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
            inputs.categoriesIds = categories;
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
            if (max != inputs.categoriesIds.size()) {
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

            inputs.categoriesRanking = categoriesValues;
        } catch (Exception e) {
            errors.addError("An error occurred: " + e + ". Remember that each rank has to be integer.");
        }
    }

    protected static void checkAndExtractParameters(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {

        if (xmcda.programParametersList.size() > 1) {
            errors.addError("Only one programParameter is expected");
            return;
        }
        if (xmcda.programParametersList.isEmpty()) {
            errors.addError("No programParameter found");
            return;
        }
        if (xmcda.programParametersList.get(0).size() != 2) {
            errors.addError("Parameter's list must contain exactly two elements");
            return;
        }

        checkAndExtractCutPoint(inputs, xmcda, errors);
        checkAndExtractAssignToABetterClass(inputs, xmcda, errors);
    }

    protected static void checkAndExtractCutPoint(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        Double cutPoint;

        final ProgramParameter<?> prgParam1 = xmcda.programParametersList.get(0).get(0);
        if (!"cutPoint".equalsIgnoreCase(prgParam1.id())) {
            errors.addError(String.format("Invalid parameter w/ id '%s'", prgParam1.id()));
            return;
        }
        if (prgParam1.getValues() == null || (prgParam1.getValues() != null && prgParam1.getValues().size() != 1)) {
            errors.addError("Parameter cutPoint must have a single (real) value only");
            return;
        }
        if (!prgParam1.getValues().get(0).isNumeric()) {
            errors.addError("Invalid value for parameter cutPoint, it must be a numeric value");
            return;
        }
        try {
            cutPoint = (Double) prgParam1.getValues().get(0).getValue();
            if (cutPoint > 1 || cutPoint < -1) {
                errors.addError("Invalid value for parameter cutPoint, it must be a numeric value greater or equal to -1 and lower or equal to 1");
                return;
            }
            inputs.cutPoint = cutPoint;
        } catch (Exception exception) {
            String err = "Invalid value for parameter cut point, it must be a real number.";
            errors.addError(err);
            return;
        }
    }

    protected static void checkAndExtractAssignToABetterClass(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        Boolean assignToABetterClass;

        final ProgramParameter<?> prgParam2 = xmcda.programParametersList.get(0).get(1);
        if (!"assignToABetterClass".equalsIgnoreCase(prgParam2.id())) {
            errors.addError(String.format("Invalid parameter w/ id '%s'", prgParam2.id()));
            return;
        }
        if (prgParam2.getValues() == null || (prgParam2.getValues() != null && prgParam2.getValues().size() != 1)) {
            errors.addError("Parameter assignToABetterClass must have a single (boolean) value only");
            return;
        }
        try {
            assignToABetterClass = (Boolean) prgParam2.getValues().get(0).getValue();
            if (assignToABetterClass == null) {
                errors.addError("Invalid value for parameter assignToABetterClass, it must be true or false.");
                return;
            }
            inputs.assignToABetterClass = assignToABetterClass;
        } catch (Exception exception) {
            String err = "Invalid value for parameter assignToABetterClass, it must be true or false.";
            errors.addError(err);
        }
    }

    protected static void checkAndExtractProfilesIds(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        inputs.profilesIds = new ArrayList<>();

        if (xmcda.categoriesProfilesList.isEmpty()) {
            errors.addError("No categories profiles list has been supplied");
        }
        if (xmcda.categoriesProfilesList.size() > 1) {
            errors.addError("You can not supply more then 1 categories profiles list");
        }

        inputs.categoryProfiles = new ArrayList<>();

        CategoriesProfiles categoriesProfiles = xmcda.categoriesProfilesList.get(0);
        if (inputs.categoriesRanking.size() != categoriesProfiles.size()) {
            errors.addError("There is a problem with categories rank list or categories profiles list. Each category has to be added to categories profiles list.");
            return;
        }

        for (Object profile : categoriesProfiles) {
            CategoryProfile tmpProfile = (CategoryProfile) profile;
            if (!"bounding".equalsIgnoreCase(tmpProfile.getType().name())) {
                errors.addError("There is a problem with categories rank list or categories profiles list. You need to provide boundary profiles for categories.");
                return;
            } else {
                inputs.categoryProfiles.add(tmpProfile);
            }
        }

        Collections.sort(inputs.categoryProfiles, (left, right) -> Integer.compare(inputs.categoriesRanking.get(left.getCategory().id()), inputs.categoriesRanking.get(right.getCategory().id())));


        inputs.profilesIds = new ArrayList<>();
        checkAndExtractBoundaryProfilesIds(errors, inputs);
    }

    protected static void checkAndExtractBoundaryProfilesIds(ProgramExecutionResult errors, Inputs inputs) {
        for (int j = 0; j < inputs.categoryProfiles.size() - 1; j++) {
            if (inputs.categoryProfiles.get(j).getUpperBound() != null && inputs.categoryProfiles.get(j + 1).getLowerBound() != null) {
                inputs.profilesIds.add(inputs.categoryProfiles.get(j).getUpperBound().getAlternative().id());
                if (!inputs.categoryProfiles.get(j).getUpperBound().getAlternative().id().equals(inputs.categoryProfiles.get(j + 1).getLowerBound().getAlternative().id())) {
                    errors.addError("Each two closest categories have to be separated by same boundary profile.");
                    return;
                }
            } else {
                errors.addError("There is a problem with categories profiles. You need to provide boundary profiles for categories");
                return;
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

    }

    protected static void checkAndExtractPositiveFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {

        inputs.positiveFlows = new LinkedHashMap<>();

        AlternativesValues positiveFlows = xmcda.alternativesValuesList.get(0);
        if (!positiveFlows.isNumeric()) {
            errors.addError("Each flow must have numeric type.");
            return;
        }

        try {
            Map<Alternative, LabelledQValues<Double>> positiveFlowsMap = positiveFlows;
            for (Map.Entry<Alternative, LabelledQValues<Double>> flow : positiveFlowsMap.entrySet()) {
                Double tmpValue = flow.getValue().get(0).convertToDouble().getValue();
                inputs.positiveFlows.put(flow.getKey().id(), tmpValue);
            }
        } catch (Exception exception) {
            errors.addError("An error occurred: " + exception + ". Each flow must have numeric type.");
            return;
        }

        checkMissingValuesInPositiveFlows(inputs, errors, positiveFlows);
    }

    protected static void checkMissingValuesInPositiveFlows(Inputs inputs, ProgramExecutionResult errors, AlternativesValues positiveFlows) {
        for (int j = 0; j < inputs.alternativesIds.size(); j++) {
            boolean found = false;
            for (Object alt : positiveFlows.getAlternatives()) {
                if (((Alternative) alt).id().equals(inputs.alternativesIds.get(j))) {
                    found = true;
                }
            }
            if (!found) {
                errors.addError("There are some missing values in positive flows.");
                return;
            }
        }

        for (int i = 0; i < inputs.profilesIds.size(); i++) {
            boolean found = false;
            for (Object alt : positiveFlows.getAlternatives()) {
                if (((Alternative) alt).id().equals(inputs.alternativesIds.get(i))) {
                    found = true;
                }
            }
            if (!found) {
                errors.addError("There are some missing values in positive flows.");
                return;
            }
        }
    }

    protected static void checkAndExtractNegativeFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {

        inputs.negativeFlows = new LinkedHashMap<>();

        AlternativesValues negativeFlows = xmcda.alternativesValuesList.get(1);
        if (!negativeFlows.isNumeric()) {
            errors.addError("Each flow must have numeric type.");
            return;
        }

        try {
            Map<Alternative, LabelledQValues<Double>> negativeFlowsMap = negativeFlows;
            for (Map.Entry<Alternative, LabelledQValues<Double>> flow : negativeFlowsMap.entrySet()) {
                Double tmpValue = flow.getValue().get(0).convertToDouble().getValue();
                inputs.negativeFlows.put(flow.getKey().id(), tmpValue);
            }
        } catch (Exception exception) {
            errors.addError("An error occurred: " + exception + ". Each flow must have numeric type.");
            return;
        }

        checkMissingValuesInNegativeFlows(inputs, errors, negativeFlows);
    }

    protected static void checkMissingValuesInNegativeFlows(Inputs inputs, ProgramExecutionResult errors, AlternativesValues negativeFlows) {
        for (int j = 0; j < inputs.alternativesIds.size(); j++) {
            boolean found = false;
            for (Object alt : negativeFlows.getAlternatives()) {
                if (((Alternative) alt).id().equals(inputs.alternativesIds.get(j))) {
                    found = true;
                }
            }
            if (!found) {
                errors.addError("There are some missing values in negative flows.");
                return;
            }
        }

        for (int i = 0; i < inputs.profilesIds.size(); i++) {
            boolean found = false;
            for (Object alt : negativeFlows.getAlternatives()) {
                if (((Alternative) alt).id().equals(inputs.alternativesIds.get(i))) {
                    found = true;
                }
            }
            if (!found) {
                errors.addError("There are some missing values in negative flows.");
                return;
            }
        }
    }

    protected static void checkAndExtractCriteria(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.criteria.getActiveCriteria().isEmpty()) {
            errors.addError("You need to provide a not empty criteria list.");
            return;
        }
        inputs.criteriaIds = xmcda.criteria.getActiveCriteria().stream().filter(a -> "criteria".equals(a.getMarker())).map(
                Criterion::id).collect(Collectors.toList());

        checkAndExtractCriteriaPreferencesDirection(inputs, xmcda, errors);
        checkAndExtractCriteriaPreferenceThresholds(inputs, xmcda, errors);
    }

    protected static void checkAndExtractCriteriaPreferencesDirection(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (inputs.criteriaIds == null || inputs.criteriaIds.isEmpty()) {
            return;
        }

        if (xmcda.criteriaScalesList.size() != 1) {
            errors.addError("You need to provide one not empty criteria scales list.");
            return;
        }

        inputs.criteriaPreferencesDirection = new HashMap<>();

        CriteriaScales criteriaDirection = xmcda.criteriaScalesList.get(0);
        for (Map.Entry<Criterion, CriterionScales> criterionEntry : criteriaDirection.entrySet()) {
            try {
                @SuppressWarnings("unchecked")
                QuantitativeScale<String> scale = (QuantitativeScale<String>) criterionEntry.getValue().get(0);
                inputs.criteriaPreferencesDirection.put(criterionEntry.getKey().id(), scale.getPreferenceDirection().name());
            } catch (Exception e) {
                errors.addError("Each criterion scale must be a label \"min\" or \"max\".");
                return;
            }
        }
    }

    protected static void checkAndExtractCriteriaPreferenceThresholds(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (inputs.criteriaIds == null || inputs.criteriaIds.isEmpty()) {
            return;
        }

        if (xmcda.criteriaThresholdsList.size() != 1) {
            errors.addError("You need to provide one not empty criteria thresholds list.");
            return;
        }

        inputs.criteriaPreferenceThresholds = new HashMap<>();
        CriteriaThresholds criteriaThresholds = xmcda.criteriaThresholdsList.get(0);
        for (Map.Entry<Criterion, CriterionThresholds> criterionEntry : criteriaThresholds.entrySet()) {
            for (int i = 0; i < criterionEntry.getValue().size(); i++) {
                if ("preference".equalsIgnoreCase(criterionEntry.getValue().get(i).mcdaConcept())) {
                    try {
                        Threshold<Double> threshold = (Threshold<Double>) criterionEntry.getValue().get(i);
                        inputs.criteriaPreferenceThresholds.put(criterionEntry.getKey().id(), threshold.getConstant().getValue());
                    } catch (Exception e) {
                        errors.addError("Each threshold must be a real value");
                        return;
                    }
                }
            }
        }
    }

    protected static void checkAndExtractProfilesPerformance(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (inputs.profilesIds == null || inputs.profilesIds.isEmpty() || inputs.profilesIds.get(0).isEmpty()) {
            return;
        }
        if (xmcda.performanceTablesList.size() != 1) {
            errors.addError("You need to provide exactly 1 profile performances lists.");
            return;
        }

        inputs.profilesPerformance = new HashMap<>();

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
            if (!inputs.profilesIds.contains(alternative.id())) {
                continue;
            }
            for (Criterion criterion : profilesPerformance.getCriteria()) {
                if (!inputs.criteriaIds.contains(criterion.id())) {
                    continue;
                }

                Double value = profilesPerformance.getValue(alternative, criterion);
                inputs.profilesPerformance.putIfAbsent(alternative.id(), new LinkedHashMap<>());
                inputs.profilesPerformance.get(alternative.id()).put(criterion.id(), value);
            }
        }
    }
}
