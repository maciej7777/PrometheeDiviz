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
 * Created by Maciej Uniejewski on 2016-11-12.
 */
public class InputsHandler {

    private InputsHandler() {

    }

    public enum ComparisonWithProfiles
    {
        CENTRAL("central"),
        BOUNDING("bounding");
        private String label;
        private ComparisonWithProfiles(String operatorLabel)
        {
            label = operatorLabel;
        }

        public final String getLabel()
        {
            return label;
        }

        @Override
        public String toString()
        {
            return label;
        }

        public static ComparisonWithProfiles fromString(String operatorLabel)
        {
            if ( operatorLabel == null )
                throw new NullPointerException("operatorLabel is null");
            for ( ComparisonWithProfiles op : ComparisonWithProfiles.values() )
            {
                if ( op.toString().equals(operatorLabel) )
                    return op;
            }
            throw new IllegalArgumentException("No enum ComparisonWithProfiles with label " + operatorLabel);
        }
    }

    public static class Inputs {
        public List<String> alternativesIds;
        public List<String> categoriesIds;
        public List<String> profilesIds;
        public Map<String, Double> flows;
        public Map<String, Integer> categoriesRanking;
        public List<CategoryProfile> categoryProfiles;
        public ComparisonWithProfiles profilesType;
    }

    public static Inputs checkAndExtractInputs(XMCDA xmcda, ProgramExecutionResult xmcdaExecResults)
    {
        Inputs inputsDict = checkInputs(xmcda, xmcdaExecResults);
        if ( xmcdaExecResults.isError() )
            return null;
        return inputsDict;
    }

    protected static Inputs checkInputs(XMCDA xmcda, ProgramExecutionResult errors)
    {
        Inputs inputs = new Inputs();

        checkAndExtractAlternatives(inputs, xmcda, errors);
        checkAndExtractParameters(inputs, xmcda, errors);
        checkAndExtractCategories(inputs, xmcda, errors);
        checkCategoriesRanking(inputs, xmcda, errors);
        checkAndExtractProfilesIds(inputs, xmcda, errors);
        checkAndExtractAlternativesFlows(inputs, xmcda, errors);

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
        inputs.profilesType = profilesType;
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

    protected static void checkAndExtractProfilesIds(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        inputs.profilesIds = new ArrayList<>();

        if (xmcda.categoriesProfilesList.isEmpty()) {
            errors.addError("No categories profiles list has been supplied.");
        }
        if (xmcda.categoriesProfilesList.size() > 1) {
            errors.addError("You can not supply more then 1 categories profiles list.");
        }

        inputs.categoryProfiles = new ArrayList<>();

        CategoriesProfiles categoriesProfiles = xmcda.categoriesProfilesList.get(0);
        if (inputs.categoriesRanking.size() != categoriesProfiles.size()) {
            errors.addError("There is a problem with categories rank list or categories profiles list. Each category has to be added to categories profiles list.");
            return;
        }

        for (Object profile : categoriesProfiles) {
            CategoryProfile tmpProfile = (CategoryProfile) profile;
            if (!tmpProfile.getType().name().equalsIgnoreCase(inputs.profilesType.toString())) {
                errors.addError("There is a problem with categories rank list or categories profiles list. Each category has to be added to categories profiles list.");
                return;
            } else {
                inputs.categoryProfiles.add(tmpProfile);
            }
        }

        Collections.sort(inputs.categoryProfiles, (left, right) -> Integer.compare(
                inputs.categoriesRanking.get(left.getCategory().id()), inputs.categoriesRanking.get(right.getCategory().id())));

        if ("BOUNDING".equalsIgnoreCase(inputs.profilesType.toString())) {
            checkAndExtractBoundaryProfilesIds(errors, inputs);
        } else if ("CENTRAL".equalsIgnoreCase(inputs.profilesType.toString())) {
            checkAndExtractCentralProfilesIds(errors, inputs);
        }
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

    protected static void checkAndExtractCentralProfilesIds(ProgramExecutionResult errors, InputsHandler.Inputs inputs) {
        for (int j = 0; j < inputs.categoryProfiles.size(); j++) {
            if (inputs.categoryProfiles.get(j).getCentralProfile() != null) {
                inputs.profilesIds.add(inputs.categoryProfiles.get(j).getCentralProfile().getAlternative().id());
            } else {
                errors.addError("There is a problem with categories profiles. You need to provide central for categories.");
                break;
            }
        }
    }

    protected static void checkAndExtractAlternativesFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.alternativesValuesList.size() != 1) {
            errors.addError("You need to provide 1 alternatives values list for net flows.");
            return;
        }
        checkAndExtractNetFlows(inputs, xmcda, errors);

    }

    protected static void checkAndExtractNetFlows(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {

        inputs.flows = new LinkedHashMap<>();

        AlternativesValues netFlows = xmcda.alternativesValuesList.get(0);
        if (!netFlows.isNumeric()) {
            errors.addError("Each flow must have numeric type.");
            return;
        }

        try {
            Map<Alternative, LabelledQValues<Double>> netFlowsMap = netFlows;
            for (Map.Entry<Alternative, LabelledQValues<Double>> flow : netFlowsMap.entrySet()) {
                Double tmpValue = flow.getValue().get(0).convertToDouble().getValue();
                inputs.flows.put(flow.getKey().id(), tmpValue);
            }
        } catch (Exception exception) {
            errors.addError("An error occurred: " + exception + ". Each flow must have numeric type.");
            return;
        }

        checkMissingValuesInNetFlows(inputs, errors, netFlows);
    }

    protected static void checkMissingValuesInNetFlows(Inputs inputs, ProgramExecutionResult errors, AlternativesValues flows) {
        for (int j = 0; j < inputs.alternativesIds.size(); j++) {
            boolean found = false;
            for (Object alt : flows.getAlternatives()) {
                if (((Alternative) alt).id().equals(inputs.alternativesIds.get(j))) {
                    found = true;
                }
            }
            if (!found) {
                errors.addError("There are some missing values in flows.");
                return;
            }
        }

        for (int i = 0; i < inputs.profilesIds.size(); i++) {
            boolean found = false;
            for (Object alt : flows.getAlternatives()) {
                if (((Alternative) alt).id().equals(inputs.alternativesIds.get(i))) {
                    found = true;
                }
            }
            if (!found) {
                errors.addError("There are some missing values in flows.");
                return;
            }
        }
    }
}
