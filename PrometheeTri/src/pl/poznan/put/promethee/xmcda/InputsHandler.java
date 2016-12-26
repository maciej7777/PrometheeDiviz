package pl.poznan.put.promethee.xmcda;

import org.xmcda.*;

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
        public List<String> alternativesIds;
        public List<String> categoriesIds;
        public List<String> profilesIds;
        public Map<String, BigDecimal> flows;
        public Boolean assignToABetterClass;
        public Map<String, Integer> categoriesRanking;
        public List<CategoryProfile> categoryProfiles;
    }

    static public Inputs checkAndExtractInputs(XMCDA xmcda, ProgramExecutionResult xmcdaExecResults) {
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

        return inputs;
    }

    protected static void checkAndExtractAlternatives(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        if (xmcda.alternatives.isEmpty()) {
            errors.addError("No alternatives list has been supplied.");
        } else {
            List<String> alternativesIds = xmcda.alternatives.getActiveAlternatives().stream().filter(a -> "alternatives".equals(a.getMarker())).map(
                    Alternative::id).collect(Collectors.toList());
            if (alternativesIds.isEmpty())
                errors.addError("The alternatives list can not be empty.");

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

        checkAndExtractAssignToABetterClass(inputs, xmcda, errors);
    }

    protected static void checkAndExtractAssignToABetterClass(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
        Boolean assignToABetterClass;

        final ProgramParameter<?> prgParam1 = xmcda.programParametersList.get(0).get(0);
        if (!"assignToABetterClass".equalsIgnoreCase(prgParam1.id())) {
            errors.addError(String.format("Invalid parameter w/ id '%s'", prgParam1.id()));
            return;
        }
        if (prgParam1.getValues() == null || (prgParam1.getValues() != null && prgParam1.getValues().size() != 1)) {
            errors.addError("Parameter assignToABetterClass must have a single (boolean) value only");
            return;
        }
        try {
            assignToABetterClass = (Boolean) prgParam1.getValues().get(0).getValue();
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
            if (!"central".equalsIgnoreCase(tmpProfile.getType().name())) {
                errors.addError("There is a problem with categories rank list or categories profiles list. You need to provide central profiles for categories.");
                return;
            } else {
                inputs.categoryProfiles.add(tmpProfile);
            }
        }

        Collections.sort(inputs.categoryProfiles, (left, right) -> Integer.compare(inputs.categoriesRanking.get(left.getCategory().id()), inputs.categoriesRanking.get(right.getCategory().id())));


        inputs.profilesIds = new ArrayList<>();
        checkAndExtractCentralProfilesIds(errors, inputs);
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

        AlternativesValues positiveFlows = xmcda.alternativesValuesList.get(0);
        if (!positiveFlows.isNumeric()) {
            errors.addError("Each flow must have numeric type.");
            return;
        }

        try {
            Map<Alternative, LabelledQValues<Double>> positiveFlowsMap = positiveFlows;
            for (Map.Entry<Alternative, LabelledQValues<Double>> flow : positiveFlowsMap.entrySet()) {
                Double tmpValue = flow.getValue().get(0).convertToDouble().getValue();
                BigDecimal bigDecimalValue = BigDecimal.valueOf(tmpValue);
                inputs.flows.put(flow.getKey().id(), bigDecimalValue);
            }
        } catch (Exception exception) {
            errors.addError("An error occurred: " + exception + ". Each flow must have numeric type.");
            return;
        }

        checkMissingValuesInNetFlows(inputs, errors, positiveFlows);
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
