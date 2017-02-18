package pl.poznan.put.promethee.xmcda;

import org.xmcda.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-11-01.
 */
public class OutputsHandler {

    private static final String ASSIGNMENTS = "assignments";
    private static final String MESSAGES = "messages";

    private OutputsHandler() {

    }

    public static class Output{
        private Map<String, Map<String, String>> assignments;

        public Map<String, Map<String, String>> getAssignments() {
            return assignments;
        }

        public void setAssignments(Map<String, Map<String, String>> assignments) {
            this.assignments = assignments;
        }
    }

    public static final String xmcdaV3Tag(String outputName)
    {
        switch(outputName)
        {
            case ASSIGNMENTS:
                return "alternativesAssignments";
            case MESSAGES:
                return "programExecutionResult";
            default:
                throw new IllegalArgumentException(String.format("Unknown output name '%s'",outputName));
        }
    }

    public static final String xmcdaV2Tag(String outputName)
    {
        switch(outputName)
        {
            case ASSIGNMENTS:
                return "alternativesAffectations";
            case MESSAGES:
                return "methodMessages";
            default:
                throw new IllegalArgumentException(String.format("Unknown output name '%s'",outputName));
        }
    }

    public static Map<String, XMCDA> convert(Map<String, Map<String, String>> assignments)
    {
        final HashMap<String, XMCDA> xResults = new HashMap<>();

        XMCDA assignmentsXmcdaObject = new XMCDA();
        AlternativesAssignments alternativeAssignments = new AlternativesAssignments();


        for (Map.Entry<String, Map<String, String>> alternativeEntry : assignments.entrySet()) {
            AlternativeAssignment tmpAssignment = new AlternativeAssignment();
            tmpAssignment.setAlternative(new Alternative(alternativeEntry.getKey()));

            CategoriesInterval tmpInterval = new CategoriesInterval();
            tmpInterval.setLowerBound(new Category(alternativeEntry.getValue().get("LOWER")));
            tmpInterval.setUpperBound(new Category(alternativeEntry.getValue().get("UPPER")));

            tmpAssignment.setCategoryInterval(tmpInterval);

            alternativeAssignments.add(tmpAssignment);
        }


        assignmentsXmcdaObject.alternativesAssignmentsList.add(alternativeAssignments);

        xResults.put(ASSIGNMENTS, assignmentsXmcdaObject);

        return xResults;
    }
}
