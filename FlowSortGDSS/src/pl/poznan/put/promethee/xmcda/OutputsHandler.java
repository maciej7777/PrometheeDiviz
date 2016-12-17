package pl.poznan.put.promethee.xmcda;

import org.xmcda.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-12-10.
 */
public class OutputsHandler {

    private static final String FINAL_ASSIGNMENTS = "final_assignments";
    private static final String FIRST_STEP_ASSIGNMENTS = "first_step_assignments";
    private static final String MESSAGES = "messages";

    private OutputsHandler() {
        throw new IllegalAccessError("Utility class");
    }

    public static class Output{
        private Map<String, String> assignments;
        private Map<String, Map<String, String>> firstStepAssignments;

        public Map<String, String> getAssignments() {
            return assignments;
        }

        public void setAssignments(Map<String, String> assignments) {
            this.assignments = assignments;
        }

        public Map<String, Map<String, String>> getFirstStepAssignments() {
            return firstStepAssignments;
        }

        public void setFirstStepAssignments(Map<String, Map<String, String>> firstStepAssignments) {
            this.firstStepAssignments = firstStepAssignments;
        }
    }

    public static final String xmcdaV3Tag(String outputName)
    {
        switch(outputName)
        {
            case FINAL_ASSIGNMENTS:
            case FIRST_STEP_ASSIGNMENTS:
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
            case FINAL_ASSIGNMENTS:
            case FIRST_STEP_ASSIGNMENTS:
                return "alternativesAffectations";
            case MESSAGES:
                return "methodMessages";
            default:
                throw new IllegalArgumentException(String.format("Unknown output name '%s'",outputName));
        }
    }

    public static Map<String, XMCDA> convert(Map<String, Map<String, String>> firstStepAssignments, Map<String, String> finalAssignments)
    {
        final HashMap<String, XMCDA> results = new HashMap<>();

        XMCDA finalAssignmentsXmcdaObject = new XMCDA();
        XMCDA firstStepAssignmentsXmcdaObject = new XMCDA();
        AlternativesAssignments  finalAlternativeAssignments = new AlternativesAssignments();
        AlternativesAssignments firstStepAlternativeAssignments = new AlternativesAssignments();

        for (Map.Entry<String, String> alternativeEntry : finalAssignments.entrySet()) {
            AlternativeAssignment tmpAssignment = new AlternativeAssignment();
            tmpAssignment.setAlternative(new Alternative(alternativeEntry.getKey()));
            tmpAssignment.setCategory(new Category(alternativeEntry.getValue()));
            finalAlternativeAssignments.add(tmpAssignment);
        }

        for (Map.Entry<String, Map<String, String>> alternativeEntry : firstStepAssignments.entrySet()) {
            AlternativeAssignment tmpAssignment = new AlternativeAssignment();
            tmpAssignment.setAlternative(new Alternative(alternativeEntry.getKey()));

            CategoriesInterval tmpInterval = new CategoriesInterval();
            tmpInterval.setLowerBound(new Category(alternativeEntry.getValue().get("LOWER")));
            tmpInterval.setUpperBound(new Category(alternativeEntry.getValue().get("UPPER")));

            tmpAssignment.setCategoryInterval(tmpInterval);

            firstStepAlternativeAssignments.add(tmpAssignment);
        }

        finalAssignmentsXmcdaObject.alternativesAssignmentsList.add(finalAlternativeAssignments);
        firstStepAssignmentsXmcdaObject.alternativesAssignmentsList.add(firstStepAlternativeAssignments);

        results.put("final_assignments", finalAssignmentsXmcdaObject);
        results.put("first_step_assignments", firstStepAssignmentsXmcdaObject);

        return results;
    }
}
