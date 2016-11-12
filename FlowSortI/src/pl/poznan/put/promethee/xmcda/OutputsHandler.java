package pl.poznan.put.promethee.xmcda;

import org.xmcda.*;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-11-01.
 */
public class OutputsHandler {
    public static class Output{
        public Map<String, Map<String, String>> assignments;
    }

    public static final String xmcdaV3Tag(String outputName)
    {
        switch(outputName)
        {
            case "assignments":
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
            case "assignments":
                return "alternativesAssignments";
            case "messages":
                return "methodMessages";
            default:
                throw new IllegalArgumentException(String.format("Unknown output name '%s'",outputName));
        }
    }

    public static Map<String, XMCDA> convert(Map<String, Map<String, String>> assignments, ProgramExecutionResult executionResult)
    {
        final HashMap<String, XMCDA> x_results = new HashMap<>();

		/* alternativesValues */
        XMCDA assignmentsXmcdaObject = new XMCDA();
        AlternativesAssignments alternativeAssignments = new AlternativesAssignments();


        for (String alternativeId : assignments.keySet()) {
            AlternativeAssignment tmpAssignment = new AlternativeAssignment();
            tmpAssignment.setAlternative(new Alternative(alternativeId));

            CategoriesInterval tmpInterval = new CategoriesInterval();
            tmpInterval.setLowerBound(new Category(assignments.get(alternativeId).get("LOWER")));
            tmpInterval.setUpperBound(new Category(assignments.get(alternativeId).get("UPPER")));

            tmpAssignment.setCategoryInterval(tmpInterval);

            alternativeAssignments.add(tmpAssignment);
        }


        assignmentsXmcdaObject.alternativesAssignmentsList.add(alternativeAssignments);

        x_results.put("assignments", assignmentsXmcdaObject);

        return x_results;
    }
}
