package pl.poznan.put.promethee.xmcda;

import org.xmcda.*;
import org.xmcda.Alternative;
import org.xmcda.AlternativesMatrix;
import org.xmcda.AlternativesValues;
import org.xmcda.CategoriesProfiles;
import org.xmcda.CategoriesValues;
import org.xmcda.Category;
import org.xmcda.CategoryProfile;
import org.xmcda.Criterion;
import org.xmcda.PerformanceTable;
import org.xmcda.XMCDA;
import org.xmcda.utils.Coord;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Maciej Uniejewski on 2016-12-10.
 */
public class InputsHandler {

    public static class Inputs {
        public List<String> alternativesIds;
        public List<String> categoriesIds;
        public List<String> criteriaIds;
        public Map<String, Integer> categoriesRanking;
        public List<List<String>> profilesIds;
        public List<Map<String, Double>> alternativesFlows;
        public Map<String, Double> alternativesFlowsAverage;
        public List<List<CategoryProfile>> categoryProfiles;
        public ComparisonWithProfiles profilesType;
        public List<Double> decisionMakersWages;
        public Boolean assignToABetterClass;
        public Integer decisionMakers;
        public Map<String, String> criteriaPreferencesDirection;
        public List<Map<String, Map<String, Double>>> profilesPerformance;
        public Map<String, Double> profilesFlows;
        public List<Map<String, Map<String, Double>>> preferences;
        public Map<String, Map<String, Double>> profilesSummaryFlows;
    }

    public enum ComparisonWithProfiles {
        CENTRAL("central"),
        BOUNDING("bounding");
        private String label;

        ComparisonWithProfiles(String operatorLabel) {
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
                throw new NullPointerException("operatorLabel is null");
            for (ComparisonWithProfiles op : ComparisonWithProfiles.values()) {
                if (op.toString().equals(operatorLabel))
                    return op;
            }
            throw new IllegalArgumentException("No enum comparisonWithProfiles with label " + operatorLabel);
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
        checkAndExtractParameters(inputs, xmcda, errors);
        checkAndExtractNumberOfDecisionMakers(inputs, xmcda, errors);
        checkAndExtractProfilesIds(inputs, xmcda, errors);
        checkAndExtractCriteria(inputs, xmcda, errors);
        checkAndExtractCriteriaPreferencesDirection(inputs, xmcda, errors);
        checkAndExtractProfilesPerformance(inputs, xmcda, errors);
        checkDominanceCondition(inputs, errors);
        checkAndExtractAlternativesFlows(inputs, xmcda, errors);
        extractFlowsAverage(inputs);
        checkAndExtractProfilesFlows(inputs, xmcda, errors);
        checkAndExtractPreferences(inputs, xmcda, errors);

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
        }
        else {
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

            int min = 100;
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
        if (xmcda.programParametersList.get(0).size() != 12) {
            errors.addError("Parameters' list must contain exactly twelve elements");
            return;
        }

        checkAndExtractProfilesType(inputs, xmcda, errors);
        checkAndExtractAssignToABetterClass(inputs, xmcda, errors);
        checkAndExtractDecisionMakersWeights(inputs, xmcda, errors);
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
        inputs.profilesType = profilesType;
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

    protected static void checkAndExtractDecisionMakersWeights(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {

        inputs.decisionMakersWages = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Double weight;

            final ProgramParameter<?> tmpPrgParam = xmcda.programParametersList.get(0).get(i + 1);
            if (!("decisionMaker" + i).equalsIgnoreCase(tmpPrgParam.id())) {
                errors.addError(String.format("Invalid parameter w/ id '%s'", tmpPrgParam.id()));
                return;
            }
            if (tmpPrgParam.getValues() == null || (tmpPrgParam.getValues() != null && tmpPrgParam.getValues().size() != 1)) {
                errors.addError("Parameter decisionMaker" + i + " must have a single (real) value only");
                return;
            }

            try {
                weight = (Double) tmpPrgParam.getValues().get(0).getValue();
                if (weight == null) {
                    errors.addError("Invalid value for parameter decisionMaker" + i + ", it must be a real number.");
                    return;
                }
                inputs.decisionMakersWages.add(weight);
            } catch (Exception exception) {
                String err = "Invalid value for parameter decisionMaker" + i + ", it must be a real number.";
                errors.addError(err);
            }
        }
    }

    protected static void checkAndExtractNumberOfDecisionMakers(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        int perfTables = xmcda.performanceTablesList.size();
        int categProfiles = xmcda.categoriesProfilesList.size();
        int flows = xmcda.alternativesValuesList.size() - 1;
        int preferences = xmcda.alternativesMatricesList.size();
        int decisionMakers = 0;

        if (perfTables != categProfiles || perfTables != flows || perfTables != preferences) {
            String err = "Invalid number of files for some of decision makers. Each decision maker need to provide his own categories profiles, performance table, flows and preferences lists.";
            errors.addError(err);
        } else {
            decisionMakers = perfTables;
        }

        if (decisionMakers < 2 || decisionMakers > 10) {
            String err = "Invalid number of decision makers. You have to provide files for 2 - 10 decision makers.";
            errors.addError(err);
            return;
        }

        inputs.decisionMakers = decisionMakers;
    }

    protected static void checkAndExtractProfilesIds(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        inputs.profilesIds = new ArrayList<>();

        if (xmcda.categoriesProfilesList.isEmpty()) {
            errors.addError("No categories profiles list has been supplied");
        }
        if (xmcda.categoriesProfilesList.size() > 10) {
            errors.addError("You can not supply more then 10 categories profiles list");
        }

        if (inputs.decisionMakers == null) {
            return;
        }

        inputs.categoryProfiles = new ArrayList<>();
        for (int i = 0; i < inputs.decisionMakers; i++) {
            List<CategoryProfile> categoriesProfilesList = new ArrayList<>();
            CategoriesProfiles categoriesProfiles = xmcda.categoriesProfilesList.get(i);
            if (inputs.categoriesRanking.size() != categoriesProfiles.size()) {
                errors.addError("There is a problem with categories rank list or categories profiles list for decision maker" + (i + 1) + ". Each category has to be added to categories profiles list or each decision maker and to global categories ranks list.");
                return;
            }

            for (Object profile : categoriesProfiles) {
                CategoryProfile tmpProfile = (CategoryProfile) profile;
                if (!tmpProfile.getType().name().equalsIgnoreCase(inputs.profilesType.toString())) {
                    errors.addError("There is a problem with categories rank list or categories profiles list for decision maker" + (i + 1) + ". Every decision maker need to provide profiles for categories witch are boundary or central. Profiles type need to be same for all decision makers and equal to setting in program parameters input.");
                    return;
                } else {
                    categoriesProfilesList.add(tmpProfile);
                }
            }

            Collections.sort(categoriesProfilesList, (left, right) -> Integer.compare(inputs.categoriesRanking.get(left.getCategory().id()), inputs.categoriesRanking.get(right.getCategory().id())));

            inputs.categoryProfiles.add(categoriesProfilesList);

            List<String> profilesIds = new ArrayList<>();
            if ("BOUNDING".equalsIgnoreCase(inputs.profilesType.toString())) {
                checkAndExtractBoundaryProfilesIds(errors, categoriesProfilesList, profilesIds, i);
            } else if ("CENTRAL".equalsIgnoreCase(inputs.profilesType.toString())) {
                checkAndExtractCentralProfilesIds(errors, categoriesProfilesList, profilesIds, i);
            }
            inputs.profilesIds.add(profilesIds);
        }
        checkForProfilesDuplicates(inputs, errors);
    }

    protected static void checkAndExtractCentralProfilesIds(ProgramExecutionResult errors, List<CategoryProfile> categoriesProfilesList, List<String> profilesIds, int i) {
        for (int j = 0; j < categoriesProfilesList.size(); j++) {
            if (categoriesProfilesList.get(j).getCentralProfile() != null) {
                profilesIds.add(categoriesProfilesList.get(j).getCentralProfile().getAlternative().id());
            } else {
                errors.addError("There is a problem with categories profiles for decision maker" + (i + 1) + ". Every decision maker need to provide profiles for categories witch are boundary or central.");
                break;
            }
        }
    }

    protected static void checkAndExtractBoundaryProfilesIds(ProgramExecutionResult errors, List<CategoryProfile> categoriesProfilesList, List<String> profilesIds, int i) {
        for (int j = 0; j < categoriesProfilesList.size() - 1; j++) {
            if (categoriesProfilesList.get(j).getUpperBound() != null && categoriesProfilesList.get(j + 1).getLowerBound() != null) {
                profilesIds.add(categoriesProfilesList.get(j).getUpperBound().getAlternative().id());
                if (!categoriesProfilesList.get(j).getUpperBound().getAlternative().id().equals(categoriesProfilesList.get(j + 1).getLowerBound().getAlternative().id())) {
                    errors.addError("Each two closest categories have to be separated by same boundary profile.");
                    return;
                }
            } else {
                errors.addError("There is a problem with categories profiles for decision maker" + (i + 1) + ". Every decision maker need to provide profiles for categories witch are boundary or central.");
                return;
            }
        }
    }

    protected static void checkForProfilesDuplicates(Inputs inputs, ProgramExecutionResult errors) {
        HashSet<String> testDuplicates = new HashSet<>();
        for (int i = 0; i < inputs.profilesIds.size(); i++) {
            testDuplicates.addAll(inputs.profilesIds.get(i));
        }
        if (inputs.profilesIds.get(0) != null && testDuplicates.size() != inputs.profilesIds.size() * inputs.profilesIds.get(0).size()) {
            errors.addError("There are some duplicates in decision makers profiles id's.");
        }

    }

    protected static void checkAndExtractCriteria(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.criteria.getActiveCriteria().isEmpty()) {
            errors.addError("You need to provide a not empty criteria list.");
            return;
        }
        inputs.criteriaIds = xmcda.criteria.getActiveCriteria().stream().filter(a -> "criteria".equals(a.getMarker())).map(
                Criterion::id).collect(Collectors.toList());
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
            @SuppressWarnings("unchecked")
            QuantitativeScale<String> scale = (QuantitativeScale<String>) criterionEntry.getValue().get(0);
            inputs.criteriaPreferencesDirection.put(criterionEntry.getKey().id(), scale.getPreferenceDirection().name());
        }
    }

    protected static void checkAndExtractProfilesPerformance(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (inputs.profilesIds == null || inputs.profilesIds.isEmpty() || inputs.profilesIds.get(0).isEmpty()) {
            return;
        }
        if (xmcda.performanceTablesList.size() < 2 || xmcda.performanceTablesList.size() > 10) {
            errors.addError("You need to provide 2 - 10 profile performances lists.");
            return;
        }

        inputs.profilesPerformance = new ArrayList<>();
        for (int i = 0; i < xmcda.performanceTablesList.size(); i++) {
            @SuppressWarnings("rawtypes")
            PerformanceTable p = xmcda.performanceTablesList.get(i);

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
            PerformanceTable<Double> profilesPerformance = (PerformanceTable<Double>) xmcda.performanceTablesList.get(i);
            Map<String, Map<String, Double>> profilesPerformanceMap = new LinkedHashMap<>();
            for (Alternative alternative : profilesPerformance.getAlternatives()) {
                if (!inputs.profilesIds.get(i).contains(alternative.id())) {
                    continue;
                }
                for (Criterion criterion : profilesPerformance.getCriteria()) {
                    if (!inputs.criteriaIds.contains(criterion.id())) {
                        continue;
                    }

                    Double value = profilesPerformance.getValue(alternative, criterion);
                    profilesPerformanceMap.putIfAbsent(alternative.id(), new LinkedHashMap<>());
                    profilesPerformanceMap.get(alternative.id()).put(criterion.id(), value);
                }
            }
            inputs.profilesPerformance.add(profilesPerformanceMap);
        }

    }

    protected static void checkDominanceCondition(Inputs inputs, ProgramExecutionResult errors) {
        for (int i = 0; i < inputs.profilesIds.size(); i++) {
            for (int j = 0; j < inputs.profilesIds.get(i).size() - 1; j++) {
                for (int criterionIterator = 0; criterionIterator < inputs.criteriaIds.size(); criterionIterator++) {
                    int multiplier = 1;
                    if ("MIN".equalsIgnoreCase(inputs.criteriaPreferencesDirection.get(inputs.criteriaIds.get(criterionIterator)))) {
                        multiplier = -1;
                    }

                    Double currentPerformance = inputs.profilesPerformance.get(i).get(inputs.profilesIds.get(i).get(j)).get(inputs.criteriaIds.get(criterionIterator));

                    for (int z = 0; z < inputs.profilesIds.size(); z++) {
                        if (z != i) {
                            Double tempPerformance = inputs.profilesPerformance.get(z).get(inputs.profilesIds.get(z).get(j + 1)).get(inputs.criteriaIds.get(criterionIterator));

                            if (currentPerformance * multiplier >= tempPerformance * multiplier) {
                                errors.addError("Dominance condition is not respected.");
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    protected static void checkAndExtractAlternativesFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.alternativesValuesList.size() < 3 || xmcda.alternativesValuesList.size() > 11) {
            errors.addError("You need to provide 2 - 10 alternatives flows lists.");
        }

        inputs.alternativesFlows = new ArrayList<>();

        if (inputs.decisionMakers == null) {
            return;
        }

        for (int i = 0; i < inputs.decisionMakers; i++) {
            AlternativesValues flows = xmcda.alternativesValuesList.get(i + 1);
            if (!flows.isNumeric()) {
                errors.addError("Each flow must have numeric type");
            }

            Map<String, Double> tmpFlows = new LinkedHashMap<>();

            try {
                Map<Alternative, LabelledQValues<Double>> flowsMap = flows;
                for (Map.Entry<Alternative, LabelledQValues<Double>> flow : flowsMap.entrySet()) {
                    Double tmpValue = flow.getValue().get(0).convertToDouble().getValue();
                    tmpFlows.put(flow.getKey().id(), tmpValue);
                }
            } catch (Exception exception) {
                errors.addError("An error occurred: " + exception.getMessage() + ". Each flow must have numeric type.");
            }

            for (int j = 0; j < inputs.alternativesIds.size(); j++) {
                boolean found = false;
                for (Object alt : flows.getAlternatives()) {
                    if (((Alternative) alt).id().equals(inputs.alternativesIds.get(j))) {
                        found = true;
                    }
                }
                if (!found) {
                    errors.addError("There are some missing values in alternativesFlows.");
                    return;
                }
            }
            inputs.alternativesFlows.add(tmpFlows);
        }
    }

    protected static void extractFlowsAverage(Inputs inputs) {

        inputs.alternativesFlowsAverage = new LinkedHashMap<>();

        for (int i = 0; i < inputs.alternativesIds.size(); i++) {
            double sum = 0.0;

            for (int j = 0; j < inputs.alternativesFlows.size(); j++) {
                sum += inputs.alternativesFlows.get(j).get(inputs.alternativesIds.get(i));
            }

            sum = sum / inputs.alternativesFlows.size();
            inputs.alternativesFlowsAverage.put(inputs.alternativesIds.get(i), sum);
        }
    }

    protected static void checkAndExtractProfilesFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.alternativesValuesList.isEmpty() || xmcda.alternativesValuesList.size() > 11) {
            errors.addError("You need to provide 1 profiles flows lists.");
        }

        AlternativesValues flows = xmcda.alternativesValuesList.get(0);

        if (!flows.isNumeric()) {
            errors.addError("Each flow must have numeric type");
        }

        Map<String, Double> tmpFlows = new LinkedHashMap<>();

        try {
            Map<Alternative, LabelledQValues<Double>> flowsMap = flows;
            for (Map.Entry<Alternative, LabelledQValues<Double>> flow : flowsMap.entrySet()) {
                Double tmpValue = flow.getValue().get(0).convertToDouble().getValue();
                tmpFlows.put(flow.getKey().id(), tmpValue);
            }
        } catch (Exception exception) {
            errors.addError("An error occurred: " + exception.getMessage() + ". Each flow must have numeric type.");
        }

        for (int j = 0; j < inputs.profilesIds.size(); j++) {
            for (int k = 0; k < inputs.profilesIds.get(j).size(); k++) {
                boolean found = false;
                for (Object alt : flows.getAlternatives()) {
                    if (((Alternative) alt).id().equals(inputs.profilesIds.get(j).get(k))) {
                        found = true;
                    }
                }
                if (!found) {
                    errors.addError("There are some missing values in profiles flows.");
                    return;
                }
            }
        }
        inputs.profilesFlows = tmpFlows;
    }

    protected static void checkAndExtractPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.alternativesMatricesList.size() < 2 || xmcda.alternativesMatricesList.size() > 10) {
            errors.addError("You need to provide 2 to 10 preferences lists.");
            return;
        }

        inputs.preferences = new ArrayList<>();

        for (int i = 0; i < xmcda.alternativesMatricesList.size(); i++) {

            if (xmcda.alternativesMatricesList.get(i).isEmpty()) {
                errors.addError("Preferences list number " + (i + 1) + " is empty.");
                return;
            }

            try {
                AlternativesMatrix<Double> matrix = (AlternativesMatrix<Double>) xmcda.alternativesMatricesList.get(i);
                Map<String, Map<String, Double>> tmpPreferences = new LinkedHashMap<>();

                for (Map.Entry<Coord<Alternative, Alternative>, QualifiedValues<Double>> coordEntry : matrix.entrySet()) {
                    String x = coordEntry.getKey().x.id();
                    String y = coordEntry.getKey().y.id();
                    double value = coordEntry.getValue().get(0).getValue().doubleValue();
                    tmpPreferences.putIfAbsent(x, new HashMap<>());
                    tmpPreferences.get(x).put(y, value);
                }

                for (String alternativeId : inputs.alternativesIds) {
                    for (String profileId : inputs.profilesIds.get(i)) {
                        if (tmpPreferences.get(alternativeId) == null || tmpPreferences.get(alternativeId).get(profileId) == null || tmpPreferences.get(profileId) == null || tmpPreferences.get(profileId).get(alternativeId) == null) {
                            errors.addError("Preference between " + alternativeId + " and " + profileId + " is missing for decision maker " + (i + 1) + ".");
                        }
                    }
                }

                inputs.preferences.add(tmpPreferences);
            } catch (Exception e) {
                errors.addError("There was en exception during processing preferences: " + e.getMessage() + ". Remember that each preference need to be a double number.");
                return;
            }
        }

    }
}
