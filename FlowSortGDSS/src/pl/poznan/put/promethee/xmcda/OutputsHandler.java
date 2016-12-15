package pl.poznan.put.promethee.xmcda;

import org.xmcda.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-12-10.
 */
public class OutputsHandler {

    public static class Output{
        public Map<String, String> assignments;
        public Map<String, Map<String, String>> firstStepAssignments;
    }

    public static final String xmcdaV3Tag(String outputName)
    {
        switch(outputName)
        {
            case "final_assignments":
                return "alternativesAssignments";
            case "first_step_assignments":
                return "alternativesAssignments";
            case "messages":
                return "programExecutionResult";
            default:
                throw new IllegalArgumentException(String.format("Unknown output name '%s'",outputName));
        }
    }

    public static final String xmcdaV2Tag(String outputName)
    {
        switch(outputName)
        {
            case "final_assignments":
                return "alternativesAffectations";
            case "first_step_assignments":
                return "alternativesAffectations";
            case "messages":
                return "methodMessages";
            default:
                throw new IllegalArgumentException(String.format("Unknown output name '%s'",outputName));
        }
    }

    public static Map<String, XMCDA> convert(Map<String, Map<String, String>> firstStepAssignments, Map<String, String> finalAssignments, ProgramExecutionResult executionResult)
    {
        final HashMap<String, XMCDA> x_results = new HashMap<>();

        XMCDA finalAssignmentsXmcdaObject = new XMCDA();
        XMCDA firstStepAssignmentsXmcdaObject = new XMCDA();
        AlternativesAssignments  finalAlternativeAssignments = new AlternativesAssignments();
        AlternativesAssignments firstStepAlternativeAssignments = new AlternativesAssignments();

        for (String alternativeId : finalAssignments.keySet()) {
            AlternativeAssignment tmpAssignment = new AlternativeAssignment();
            tmpAssignment.setAlternative(new Alternative(alternativeId));
            tmpAssignment.setCategory(new Category(finalAssignments.get(alternativeId)));
            finalAlternativeAssignments.add(tmpAssignment);
        }

        for (String alternativeId : firstStepAssignments.keySet()) {
            AlternativeAssignment tmpAssignment = new AlternativeAssignment();
            tmpAssignment.setAlternative(new Alternative(alternativeId));

            CategoriesInterval tmpInterval = new CategoriesInterval();
            tmpInterval.setLowerBound(new Category(firstStepAssignments.get(alternativeId).get("LOWER")));
            tmpInterval.setUpperBound(new Category(firstStepAssignments.get(alternativeId).get("UPPER")));

            tmpAssignment.setCategoryInterval(tmpInterval);

            firstStepAlternativeAssignments.add(tmpAssignment);
        }


        finalAssignmentsXmcdaObject.alternativesAssignmentsList.add(finalAlternativeAssignments);
        firstStepAssignmentsXmcdaObject.alternativesAssignmentsList.add(firstStepAlternativeAssignments);

        x_results.put("final_assignments", finalAssignmentsXmcdaObject);
        x_results.put("first_step_assignments", firstStepAssignmentsXmcdaObject);

        return x_results;
    }
}
