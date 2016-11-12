package pl.poznan.put.promethee.xmcda;

import org.xmcda.*;
import org.xmcda.Alternative;
import org.xmcda.AlternativesValues;
import org.xmcda.Categories;
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

    static public Inputs checkAndExtractInputs(XMCDA xmcda, ProgramExecutionResult xmcda_exec_results)
    {
        Inputs inputsDict = checkInputs(xmcda, xmcda_exec_results);
        if ( xmcda_exec_results.isError() )
            return null;
        return extractInputs(inputsDict, xmcda, xmcda_exec_results);
    }

    protected static Inputs checkInputs(XMCDA xmcda, ProgramExecutionResult errors)
    {
        Inputs inputs = new Inputs();

        if ( xmcda.alternatives.size() == 0 )
        {
            errors.addError("No alternatives list has been supplied");
        }
        else
        {
            List<Alternative> alternatives = xmcda.alternatives.getActiveAlternatives();
            if ( alternatives.isEmpty() )
                errors.addError("The alternatives list can not be empty");
        }

        check_parameters:
        {
            ComparisonWithProfiles operator = null;
            if ( xmcda.programParametersList.size() > 1 )
            {
                errors.addError("Only one programParameter is expected");
                break check_parameters;
            }
            if ( xmcda.programParametersList.size() == 0 )
            {
                errors.addError("No programParameter found");
                break check_parameters;
            }
            if ( xmcda.programParametersList.get(0).size() != 1 )
            {
                errors.addError("Parameters' list must contain exactly one element");
                break check_parameters;
            }
            final ProgramParameter<?> prgParam = xmcda.programParametersList.get(0).get(0);
            if ( ! "ComparisonWithProfiles".toUpperCase().equals(prgParam.id().toUpperCase()) )
            {
                errors.addError(String.format("Invalid parameter w/ id '%s'", prgParam.id()));
                break check_parameters;
            }
            if ( prgParam.getValues() == null || (prgParam.getValues() != null && prgParam.getValues().size() != 1) )
            {
                errors.addError("Parameter operator must have a single (label) value only");
                break check_parameters;
            }
            try
            {
                final String operatorValue = (String) prgParam.getValues().get(0).getValue();
                operator = ComparisonWithProfiles.fromString((String) operatorValue);
            }
            catch (Throwable throwable)
            {
                StringBuffer valid_values = new StringBuffer();
                for ( ComparisonWithProfiles op : ComparisonWithProfiles.values() )
                {
                    valid_values.append(op.getLabel()).append(", ");
                }
                String err = "Invalid value for parameter ComparisonWithProfiles, it must be a label, ";
                err += "possible values are: " + valid_values.substring(0, valid_values.length() - 2);
                errors.addError(err);
                operator = null;
            }
            inputs.profilesType = operator;
        }



        //Get and check categories
        Categories categoriesList = xmcda.categories;
        if ( xmcda.categories.size() == 0 )
        {
            errors.addError("No categories list has been supplied");
        }
/*        else if ( xmcda.categories.size() > 1 )
        {
            errors.addError("More than one categories list has been supplied");
        }*/
        else
        {
            @SuppressWarnings("rawtypes")
            List<Category> categories = xmcda.categories.getActiveCategories();
            inputs.categoriesIds = xmcda.categories.getIDs();

            if ( categories.isEmpty() )
                errors.addError("The category list can not be empty");
        }

        //Get and check categories and profiles
        if ( xmcda.categoriesValuesList.size() == 0 )
        {
            errors.addError("No categories values list has been supplied");
        }
        else if ( xmcda.categoriesValuesList.size() > 1 )
        {
            errors.addError("More than one categories values list has been supplied");
        }
        CategoriesValues categoriesValuesList = xmcda.categoriesValuesList.get(0);
        if (!categoriesValuesList.isNumeric()) {
            errors.addError("Each of the categories ranks must be integer");
        }
        
        Map<String, Integer> categoriesValues = new LinkedHashMap<String, Integer>();

        try {
            CategoriesValues<Integer> categoriesValuesClass = categoriesValuesList.convertTo(Integer.class);
            xmcda.categoriesValuesList.set(0, categoriesValuesClass);

            int min = 100;
            int max = -1;

            for (Map.Entry<Category, LabelledQValues<Integer>> a: categoriesValuesClass.entrySet()) {
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
            }
            if (max != categoriesList.size()){
                errors.addError("Maximal rank should be equal to number of categories.");
            }

            boolean broken = false;
            for (Map.Entry<String, Integer> categoryA: categoriesValues.entrySet()) {
                for (Map.Entry<String, Integer> categoryB: categoriesValues.entrySet()) {
                    if (categoryA.getValue() == categoryB.getValue() && categoryA.getKey() != categoryB.getKey()) {
                        errors.addError("There can not be two categories with the same rank.");
                        broken = true;
                        break;
                    }
                }
                if (broken) {
                    break;
                }
            }

        } catch (Throwable throwable) {
            errors.addError("An error oceured: " + throwable.getMessage() + ". Remember that each rank has to be integer.");
        }


        if ( xmcda.categoriesProfilesList.size() == 0 )
        {
            errors.addError("No categories profiles list has been supplied");
        }
        else if ( xmcda.categoriesProfilesList.size() > 1 )
        {
            errors.addError("More than one categories profiles list has been supplied");
        }


        List<String> alternativesProfiles;
        List<String> profilesIds;

        if (inputs.profilesType.toString().toUpperCase().equals("BOUNDING")) {
            //Check profiles
            List<CategoryProfile> categoriesProfilesList = new ArrayList<>();
            CategoriesProfiles categoriesProfiles = xmcda.categoriesProfilesList.get(0);
            if (categoriesValues.size() != categoriesProfiles.size()) {
                errors.addError("Each category has to be added to file categories_profiles.xml.");
            }
            for (Object profile : categoriesProfiles) {
                CategoryProfile tmpProfile = (CategoryProfile) profile;
                if (!tmpProfile.getType().name().equals("BOUNDING")) {
                    errors.addError("In Pormsort each of categories need to have boundary profiles.");
                } else {
                    categoriesProfilesList.add(tmpProfile);
                }
            }


            Collections.sort(categoriesProfilesList, new Comparator<CategoryProfile>() {
                public int compare(CategoryProfile left, CategoryProfile right) {
                    return Integer.compare(categoriesValues.get(left.getCategory().id()), categoriesValues.get(right.getCategory().id()));
                }
            });
            inputs.categoryProfiles = categoriesProfilesList;

            profilesIds = new ArrayList<>();
            for (int i = 0; i < categoriesProfilesList.size() - 1; i++) {
                profilesIds.add(categoriesProfilesList.get(i).getUpperBound().getAlternative().id());
                if (!categoriesProfilesList.get(i).getUpperBound().getAlternative().id().equals(categoriesProfilesList.get(i + 1).getLowerBound().getAlternative().id())) {
                    errors.addError("Each two closest categories have to be separated by same boundary profile.");
                    break;
                }
            }

            alternativesProfiles = xmcda.alternatives.getActiveAlternatives().stream().filter(
                    alt -> !alt.id().equals(categoriesProfilesList.get(0).getLowerBound().getAlternative().id()) &&
                            !alt.id().equals(categoriesProfilesList.get(categoriesProfilesList.size() - 1).getUpperBound().getAlternative().id())).map(
                    Alternative::id).collect(Collectors.toList());

        } else if (inputs.profilesType.toString().toUpperCase().equals("CENTRAL")) {
            List<CategoryProfile> categoriesProfilesList = new ArrayList<>();
            CategoriesProfiles categoriesProfiles = xmcda.categoriesProfilesList.get(0);
            if (categoriesValues.size() != categoriesProfiles.size()) {
                errors.addError("Each category has to be added to file categories_profiles.xml.");
            }
            for (Object profile : categoriesProfiles) {
                CategoryProfile tmpProfile = (CategoryProfile) profile;
                if (!tmpProfile.getType().name().toUpperCase().equals("CENTRAL")) {
                    errors.addError("In PrometheeTri each of categories need to have boundary profiles.");
                } else {
                    categoriesProfilesList.add(tmpProfile);
                }
            }


            Collections.sort(categoriesProfilesList, new Comparator<CategoryProfile>() {
                public int compare(CategoryProfile left, CategoryProfile right) {
                    return Integer.compare(categoriesValues.get(left.getCategory().id()), categoriesValues.get(right.getCategory().id()));
                }
            });

            inputs.categoryProfiles = categoriesProfilesList;

            profilesIds = new ArrayList<>();
            for (int i = 0; i < categoriesProfilesList.size(); i++) {
                profilesIds.add(categoriesProfilesList.get(i).getCentralProfile().getAlternative().id());
            }
            alternativesProfiles = xmcda.alternatives.getActiveAlternatives().stream().map(
                    Alternative::id).collect(Collectors.toList());
        } else {
            return null;
        }

        //Check flows and categories
        if ( xmcda.alternativesValuesList.size() == 0 )
        {
            errors.addError("None of the flows list has been supplied");
        }
        else if ( xmcda.alternativesValuesList.size() > 2 )
        {
            errors.addError("More than one positive or negative flows list has been supplied");
        }




        AlternativesValues flows = xmcda.alternativesValuesList.get(0);
        if (!flows.isNumeric()) {
            errors.addError("Each flow must have numeric type");
        }

        inputs.flows = new LinkedHashMap<>();
        try {
            Map<Alternative, LabelledQValues<Double>> flowsMap = flows;
            for (Map.Entry<Alternative, LabelledQValues<Double>> flow: flowsMap.entrySet()) {
                Double tmpValue = flow.getValue().get(0).convertToDouble().getValue();
                inputs.flows.put(flow.getKey().id(), tmpValue);
            }
        } catch (Throwable throwable) {
            errors.addError("An error occured: " + throwable.getMessage() + ". Each glow must have numeric type.");
        }

        for (int i = 0; i < alternativesProfiles.size(); i++) {
            boolean found = false;
            for (Object alt : flows.getAlternatives()) {
                if (((Alternative)alt).id().equals(alternativesProfiles.get(i))) {
                    found = true;
                }
            }
            if (!found) {
                errors.addError("There are some missing values in positive flows.");
                break;
            }
        }

        alternativesProfiles.removeAll(profilesIds);
        inputs.alternativesIds = alternativesProfiles;
        inputs.profilesIds = profilesIds;

        return inputs;
    }

    protected static Inputs extractInputs(Inputs inputs, XMCDA xmcda, ProgramExecutionResult xmcda_execution_results)
    {
        return inputs;
    }
}
