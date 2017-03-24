package pl.poznan.put.promethee.xmcda;

import org.xmcda.*;
import org.xmcda.Alternative;
import org.xmcda.AlternativesValues;
import org.xmcda.CategoriesProfiles;
import org.xmcda.CategoriesValues;
import org.xmcda.Category;
import org.xmcda.CategoryProfile;
import org.xmcda.XMCDA;
import pl.poznan.put.promethee.exceptions.InputDataException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Maciej Uniejewski on 2016-11-01.
 */
public class InputsHandler {

    private InputsHandler() {

    }

    public static class Inputs {
        private List<String> alternativesIds;
        private List<String> categoriesIds;
        private List<String> profilesIds;
        private Map<String, BigDecimal> positiveFlows;
        private Map<String, BigDecimal> negativeFlows;
        private BigDecimal cutPoint;
        private Boolean assignToABetterClass;
        private Map<String, Integer> categoriesRanking;
        private List<CategoryProfile> categoryProfiles;
        private List<String> criteriaIds;
        private Map<String, String> criteriaPreferencesDirection;
        private Map<String, BigDecimal> criteriaPreferenceThresholds;
        private Map<String, Map<String, BigDecimal>> profilesPerformance;

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

        public Map<String, BigDecimal> getPositiveFlows() {
            return positiveFlows;
        }

        public void setPositiveFlows(Map<String, BigDecimal> positiveFlows) {
            this.positiveFlows = positiveFlows;
        }

        public Map<String, BigDecimal> getNegativeFlows() {
            return negativeFlows;
        }

        public void setNegativeFlows(Map<String, BigDecimal> negativeFlows) {
            this.negativeFlows = negativeFlows;
        }

        public BigDecimal getCutPoint() {
            return cutPoint;
        }

        public void setCutPoint(BigDecimal cutPoint) {
            this.cutPoint = cutPoint;
        }

        public Boolean getAssignToABetterClass() {
            return assignToABetterClass;
        }

        public void setAssignToABetterClass(Boolean assignToABetterClass) {
            this.assignToABetterClass = assignToABetterClass;
        }

        public List<CategoryProfile> getCategoryProfiles() {
            return categoryProfiles;
        }

        public void setCategoryProfiles(List<CategoryProfile> categoryProfiles) {
            this.categoryProfiles = categoryProfiles;
        }

        private Map<String, Integer> getCategoriesRanking() {
            return categoriesRanking;
        }

        private void setCategoriesRanking(Map<String, Integer> categoriesRanking) {
            this.categoriesRanking = categoriesRanking;
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

        private Map<String, BigDecimal> getCriteriaPreferenceThresholds() {
            return criteriaPreferenceThresholds;
        }

        private void setCriteriaPreferenceThresholds(Map<String, BigDecimal> criteriaPreferenceThresholds) {
            this.criteriaPreferenceThresholds = criteriaPreferenceThresholds;
        }

        private Map<String, Map<String, BigDecimal>> getProfilesPerformance() {
            return profilesPerformance;
        }

        private void setProfilesPerformance(Map<String, Map<String, BigDecimal>> profilesPerformance) {
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

        try {
            checkAndExtractAlternatives(inputs, xmcda, errors);
            checkAndExtractCategories(inputs, xmcda, errors);
            checkAndExtractParameters(inputs, xmcda, errors);
            checkCategoriesRanking(inputs, xmcda, errors);
            checkAndExtractProfilesIds(inputs, xmcda, errors);
            checkAndExtractAlternativesFlows(inputs, xmcda, errors);
            checkAndExtractCriteria(inputs, xmcda, errors);
            checkAndExtractProfilesPerformance(inputs, xmcda, errors);
            checkDominanceProperty(inputs, errors);
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

        inputs.setAlternativesIds(alternativesIds);
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
            String errorMessage = "An error occurred. Remember that each rank has to be integer.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
    }

    protected static void checkAndExtractParameters(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {

        if (xmcda.programParametersList.size() > 1) {
            String errorMessage = "Only one programParameter is expected";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        if (xmcda.programParametersList.isEmpty()) {
            String errorMessage = "No programParameter found";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        if (xmcda.programParametersList.get(0).size() != 2) {
            String errorMessage = "Parameter's list must contain exactly two elements";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        checkAndExtractCutPoint(inputs, xmcda, errors);
        checkAndExtractAssignToABetterClass(inputs, xmcda, errors);
    }

    protected static void checkAndExtractCutPoint(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        Double cutPoint;

        final ProgramParameter<?> prgParam1 = xmcda.programParametersList.get(0).get(0);
        if (!"cutPoint".equalsIgnoreCase(prgParam1.id())) {
            String errorMessage = String.format("Invalid parameter w/ id '%s'", prgParam1.id());
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        if (prgParam1.getValues() == null || (prgParam1.getValues() != null && prgParam1.getValues().size() != 1)) {
            String errorMessage = "Parameter cutPoint must have a single (real) value only";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        if (!prgParam1.getValues().get(0).isNumeric()) {
            String errorMessage = "Invalid value for parameter cutPoint, it must be a numeric value";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        try {
            cutPoint = (Double) prgParam1.getValues().get(0).getValue();
            BigDecimal bigDecimalCutPoint = BigDecimal.valueOf(cutPoint);

            if (cutPoint > 1 || cutPoint < -1) {
                String errorMessage = "Invalid value for parameter cutPoint, it must be a numeric value greater or equal to -1 and lower or equal to 1";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }

            inputs.setCutPoint(bigDecimalCutPoint);

        } catch (InputDataException e) {
            throw e;
        } catch (Exception exception) {
            String errorMessage = "Invalid value for parameter cut point, it must be a real number.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
    }

    protected static void checkAndExtractAssignToABetterClass(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        Boolean assignToABetterClass;

        final ProgramParameter<?> prgParam2 = xmcda.programParametersList.get(0).get(1);
        if (!"assignToABetterClass".equalsIgnoreCase(prgParam2.id())) {
            String errorMessage = String.format("Invalid parameter w/ id '%s'", prgParam2.id());
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        if (prgParam2.getValues() == null || (prgParam2.getValues() != null && prgParam2.getValues().size() != 1)) {
            String errorMessage = "Parameter assignToABetterClass must have a single (boolean) value only";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        try {
            assignToABetterClass = (Boolean) prgParam2.getValues().get(0).getValue();
            if (assignToABetterClass == null) {
                String errorMessage = "Invalid value for parameter assignToABetterClass, it must be true or false.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }

            inputs.setAssignToABetterClass(assignToABetterClass);

        } catch (InputDataException e) {
            throw e;
        } catch (Exception exception) {
            String errorMessage = "Invalid value for parameter assignToABetterClass, it must be true or false.";
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
            String errorMessage = "There is a problem with categories rank list or categories profiles list. Each " +
                    "category has to be added to categories profiles list.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        for (Object profile : categoriesProfiles) {
            CategoryProfile tmpProfile = (CategoryProfile) profile;
            if (!"bounding".equalsIgnoreCase(tmpProfile.getType().name())) {
                String errorMessage = "There is a problem with categories rank list or categories profiles list. " +
                        "You need to provide boundary profiles for categories.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }

            inputs.getCategoryProfiles().add(tmpProfile);
        }

        Collections.sort(inputs.getCategoryProfiles(), (left, right) -> Integer.compare(inputs.getCategoriesRanking().get(left.getCategory().id()), inputs.getCategoriesRanking().get(right.getCategory().id())));

        inputs.setProfilesIds(new ArrayList<>());
        checkAndExtractBoundaryProfilesIds(errors, inputs);
    }

    protected static void checkAndExtractBoundaryProfilesIds(ProgramExecutionResult errors, Inputs inputs) throws InputDataException {
        for (int j = 0; j < inputs.getCategoryProfiles().size() - 1; j++) {
            if (inputs.getCategoryProfiles().get(j).getUpperBound() == null || inputs.getCategoryProfiles().get(j + 1).getLowerBound() == null) {
                String errorMessage = "There is a problem with categories profiles. You need to provide boundary profiles for categories";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
            inputs.getProfilesIds().add(inputs.getCategoryProfiles().get(j).getUpperBound().getAlternative().id());

            if (!inputs.getCategoryProfiles().get(j).getUpperBound().getAlternative().id().
                    equals(inputs.getCategoryProfiles().get(j + 1).getLowerBound().getAlternative().id())) {
                String errorMessage = "Each two closest categories have to be separated by same boundary profile.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
        }
    }

    protected static void checkAndExtractAlternativesFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        if (xmcda.alternativesValuesList.size() != 2) {
            String errorMessage = "You need to provide 2 alternatives values lists - one for positive flows  and one for negative flows.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        checkAndExtractPositiveFlows(inputs, xmcda, errors);
        checkAndExtractNegativeFlows(inputs, xmcda, errors);
        checkProfilesFlows(inputs, errors);
    }

    protected static void checkAndExtractPositiveFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        inputs.setPositiveFlows(new LinkedHashMap<>());

        AlternativesValues positiveFlows = xmcda.alternativesValuesList.get(0);
        if (!positiveFlows.isNumeric()) {
            String errorMessage = "Each flow must have numeric type.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        try {
            Map<Alternative, LabelledQValues<Double>> positiveFlowsMap = positiveFlows;
            for (Map.Entry<Alternative, LabelledQValues<Double>> flow : positiveFlowsMap.entrySet()) {
                Double tmpValue = flow.getValue().get(0).convertToDouble().getValue();
                BigDecimal bigDecimalTmpValue = BigDecimal.valueOf(tmpValue);
                inputs.getPositiveFlows().put(flow.getKey().id(), bigDecimalTmpValue);
            }
        } catch (Exception exception) {
            String errorMessage = "An error occurred. Each flow must have numeric type.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        checkMissingValuesInPositiveFlows(inputs, errors);
    }

    protected static void checkMissingValuesInPositiveFlows(Inputs inputs, ProgramExecutionResult errors) throws InputDataException {
        for (int j = 0; j < inputs.getAlternativesIds().size(); j++) {
            String alternativeId = inputs.getAlternativesIds().get(j);
            if (!inputs.getPositiveFlows().containsKey(alternativeId)) {
                String errorMessage = "There are some missing values in positive flows.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
        }

        for (int i = 0; i < inputs.getProfilesIds().size(); i++) {
            String profileId = inputs.getProfilesIds().get(i);
            if (!inputs.getPositiveFlows().containsKey(profileId)) {
                String errorMessage = "There are some missing values in positive flows.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
        }
    }

    protected static void checkAndExtractNegativeFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        inputs.setNegativeFlows(new LinkedHashMap<>());

        AlternativesValues negativeFlows = xmcda.alternativesValuesList.get(1);
        if (!negativeFlows.isNumeric()) {
            String errorMessage = "Each flow must have numeric type.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        try {
            for (Map.Entry<Alternative, LabelledQValues<Double>> flow : ((Map<Alternative, LabelledQValues<Double>>) negativeFlows).entrySet()) {
                Double tmpValue = flow.getValue().get(0).convertToDouble().getValue();
                BigDecimal bigDecimalTmpValue = BigDecimal.valueOf(tmpValue);
                inputs.getNegativeFlows().put(flow.getKey().id(), bigDecimalTmpValue);
            }
        } catch (Exception exception) {
            String errorMessage = "An error occurred:. Each flow must have numeric type.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        checkMissingValuesInNegativeFlows(inputs, errors);
    }

    protected static void checkMissingValuesInNegativeFlows(Inputs inputs, ProgramExecutionResult errors) throws InputDataException {
        for (int j = 0; j < inputs.getAlternativesIds().size(); j++) {
            String alternativeId = inputs.getAlternativesIds().get(j);
            if (!inputs.getNegativeFlows().containsKey(alternativeId)) {
                String errorMessage = "There are some missing values in negative flows.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
        }

        for (int i = 0; i < inputs.getProfilesIds().size(); i++) {
            String profileId = inputs.getProfilesIds().get(i);
            if (!inputs.getNegativeFlows().containsKey(profileId)) {
                String errorMessage = "There are some missing values in negative flows.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
        }
    }

    protected static void checkProfilesFlows(Inputs inputs, ProgramExecutionResult errors) throws InputDataException {
        for (int i = 1; i < inputs.profilesIds.size(); i++) {
            String profile1 = inputs.profilesIds.get(i - 1);
            String profile2 = inputs.profilesIds.get(i);
            if (!isPreffered(profile2, profile1, inputs)) {
                String errorMessage = "There are some errors in profiles flows. Better profiles need to be preferred to the worse ones.";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
        }

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

    private static boolean isPreffered(String profile1, String profile2, Inputs inputs) {

        BigDecimal positiveFlowProfile1 = inputs.getPositiveFlows().get(profile1);
        BigDecimal positiveFlowProfile2 = inputs.getPositiveFlows().get(profile2);
        BigDecimal negativeFlowProfile1 = inputs.getNegativeFlows().get(profile1);
        BigDecimal negativeFlowProfile2 = inputs.getNegativeFlows().get(profile2);

        if (preferenceCondition1(positiveFlowProfile1, negativeFlowProfile1, positiveFlowProfile2, negativeFlowProfile2) ||
                preferenceCondition2(positiveFlowProfile1, negativeFlowProfile1, positiveFlowProfile2, negativeFlowProfile2) ||
                preferenceCondition3(positiveFlowProfile1, negativeFlowProfile1, positiveFlowProfile2, negativeFlowProfile2)) {
            return true;
        }
        return false;
    }

    protected static void checkAndExtractCriteria(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        if (xmcda.criteria.getActiveCriteria().isEmpty()) {
            String errorMessage = "You need to provide a not empty criteria list.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }
        inputs.setCriteriaIds(xmcda.criteria.getActiveCriteria().stream().filter(a -> "criteria".equals(a.getMarker())).map(
                Criterion::id).collect(Collectors.toList()));

        checkAndExtractCriteriaPreferencesDirection(inputs, xmcda, errors);
        checkAndExtractCriteriaPreferenceThresholds(inputs, xmcda, errors);
    }

    protected static void checkAndExtractCriteriaPreferencesDirection(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        if (xmcda.criteriaScalesList.size() != 1) {
            String errorMessage = "You need to provide one not empty criteria scales list.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        inputs.setCriteriaPreferencesDirection(new HashMap<>());

        CriteriaScales criteriaDirection = xmcda.criteriaScalesList.get(0);
        for (Map.Entry<Criterion, CriterionScales> criterionEntry : criteriaDirection.entrySet()) {
            try {
                @SuppressWarnings("unchecked")
                QuantitativeScale<String> scale = (QuantitativeScale<String>) criterionEntry.getValue().get(0);
                String scaleDirection = scale.getPreferenceDirection().name();
                if (!"min".equalsIgnoreCase(scaleDirection) && !"max".equalsIgnoreCase((scaleDirection))) {
                    String errorMessage = "Each criterion scale must be a label \"min\" or \"max\".";
                    errors.addError(errorMessage);
                    throw new InputDataException(errorMessage);
                }
                inputs.getCriteriaPreferencesDirection().put(criterionEntry.getKey().id(), scaleDirection);
            } catch (InputDataException e) {
                throw e;
            } catch (Exception e) {
                String errorMessage = "Each criterion scale must be a label \"min\" or \"max\".";
                errors.addError(errorMessage);
                throw new InputDataException(errorMessage);
            }
        }
    }

    protected static void checkAndExtractCriteriaPreferenceThresholds(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        if (xmcda.criteriaThresholdsList.size() != 1) {
            String errorMessage = "You need to provide one not empty criteria thresholds list.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        inputs.setCriteriaPreferenceThresholds(new HashMap<>());
        CriteriaThresholds criteriaThresholds = xmcda.criteriaThresholdsList.get(0);
        for (Map.Entry<Criterion, CriterionThresholds> criterionEntry : criteriaThresholds.entrySet()) {
            for (int i = 0; i < criterionEntry.getValue().size(); i++) {
                if ("preference".equalsIgnoreCase(criterionEntry.getValue().get(i).mcdaConcept())) {
                    try {
                        Threshold<Double> threshold = (Threshold<Double>) criterionEntry.getValue().get(i);
                        inputs.getCriteriaPreferenceThresholds().put(criterionEntry.getKey().id(), BigDecimal.valueOf(threshold.getConstant().getValue()));
                    } catch (Exception e) {
                        String errorMessage = "Each threshold must be a real value";
                        errors.addError(errorMessage);
                        throw new InputDataException(errorMessage);
                    }
                }
            }
        }
    }

    protected static void checkAndExtractProfilesPerformance(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) throws InputDataException {
        if (xmcda.performanceTablesList.size() != 1) {
            String errorMessage = "You need to provide exactly 1 profile performances lists.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        inputs.setProfilesPerformance(new HashMap<>());

        @SuppressWarnings("rawtypes")
        PerformanceTable p = xmcda.performanceTablesList.get(0);

        if (p.hasMissingValues()) {
            String errorMessage = "The performance table has missing values.";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        if (!p.isNumeric()) {
            String errorMessage = "The performance table must contain numeric values only";
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
        }

        try {
            @SuppressWarnings("unchecked")
            PerformanceTable<Double> perfTable = p.asDouble();
            xmcda.performanceTablesList.set(0, perfTable);
        } catch (Exception e) {
            final String msg = "Error when converting the performance table's value to Double, reason:";
            String errorMessage = Utils.getMessage(msg, e);
            errors.addError(errorMessage);
            throw new InputDataException(errorMessage);
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
                BigDecimal bigDecimalValue = BigDecimal.valueOf(value);
                inputs.getProfilesPerformance().putIfAbsent(alternative.id(), new LinkedHashMap<>());
                inputs.getProfilesPerformance().get(alternative.id()).put(criterion.id(), bigDecimalValue);
            }
        }
    }

    protected static void checkDominanceProperty(Inputs inputs, ProgramExecutionResult errors) throws InputDataException {

        for (String criterionId : inputs.getCriteriaIds()) {
            for (int i = 0; i < inputs.getProfilesIds().size() - 1; i++) {
                String profileId = inputs.getProfilesIds().get(i);
                String nextProfileId = inputs.getProfilesIds().get(i + 1);

                Map<String, BigDecimal> firstProfilePerformance = inputs.getProfilesPerformance().get(profileId);
                Map<String, BigDecimal> secondProfilePerformance = inputs.getProfilesPerformance().get(nextProfileId);
                BigDecimal preferenceThreshold = inputs.getCriteriaPreferenceThresholds().get(criterionId);
                String preferenceDirection = inputs.getCriteriaPreferencesDirection().get(criterionId);

                if (firstProfilePerformance == null || secondProfilePerformance == null || preferenceThreshold == null || preferenceDirection == null) {
                    String errorMessage = "There was a problem when checking profiles preferences. Profiles need to " +
                            "fulfill the dominance condition on each criterion.";
                    errors.addError(errorMessage);
                    throw new InputDataException(errorMessage);
                }

                BigDecimal firstProfileCriterionPerformance = firstProfilePerformance.get(criterionId);
                BigDecimal secondProfileCriterionPerformance = secondProfilePerformance.get(criterionId);

                if (firstProfileCriterionPerformance == null || secondProfileCriterionPerformance == null) {
                    String errorMessage = "There was a problem when checking profiles preferences. " +
                            "Profiles need to fulfill the dominance condition on each criterion.";
                    errors.addError(errorMessage);
                    throw new InputDataException(errorMessage);
                }

                if (("max".equalsIgnoreCase(preferenceDirection) && firstProfileCriterionPerformance.add(preferenceThreshold).compareTo(secondProfileCriterionPerformance) > 0) ||
                        ("min".equalsIgnoreCase(preferenceDirection) && firstProfileCriterionPerformance.subtract(preferenceThreshold).compareTo(secondProfileCriterionPerformance) < 0)) {
                    String errorMessage = "Profiles need to fulfill the dominance condition on each criterion.";
                    errors.addError(errorMessage);
                    throw new InputDataException(errorMessage);
                }
            }
        }
    }
}
