package pl.poznan.put.promethee;

import pl.poznan.put.promethee.components.DrowClassAssignment;
import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

/**
 * Created by Maciej Uniejewski on 2016-12-26.
 */
public class PlotClassAssignment {

    private static final String FIRST_ROWS = "\\begin{table}\n" +
            "\\begin{center}\n";
    private static final String SECOND_ROW_DIRECT = "\\begin{tabular}{c|c}\n" +
            "\\bf Alternative & \\bf Class \\cr\n";
    private static final String SECOND_ROW_INDIRECT = "\\begin{tabular}{c|c|c}\n" +
            "\\bf Alternative & \\bf Lower class & \\bf Upper class \\cr\n";
    private static final String END_ROW = "\\end{tabular}\n" +
            "\\caption{Alternatives Assignments}\n" +
            "\\end{center}\n" +
            "\\end{table}";
    private static final String HLINE = "\\hline\n";
    private static final String SEPARATOR = " & ";
    private static final String NEW_LINE = " \\cr\n";

    private PlotClassAssignment() {

    }

    public static OutputsHandler.Output execute(InputsHandler.Inputs inputs) {
        OutputsHandler.Output output = new OutputsHandler.Output();

        if ("direct".equalsIgnoreCase(inputs.getAssignmentType())) {
            createLatexTableForDirectAssignments(inputs, output);
        } else {
            createLatexTableForIndirectAssignments(inputs, output);
        }

        output.setAsignmentsImage(DrowClassAssignment.drawImage(inputs));

        return output;
    }

    private static void createLatexTableForDirectAssignments(InputsHandler.Inputs inputs, OutputsHandler.Output output){
        StringBuilder table = new StringBuilder();
        table.append(FIRST_ROWS);
        table.append(SECOND_ROW_DIRECT);

        for (int i = 0; i < inputs.getAlternativesIds().size(); i++) {
            table.append(HLINE);
            String alternativeId = inputs.getAlternativesIds().get(i);
            String assignment = inputs.getAssignments().get(alternativeId).get("LOWER");
            table.append(alternativeId + SEPARATOR + assignment + NEW_LINE);
        }

        table.append(END_ROW);

        output.setLatexTable(table.toString());
    }

    private static void createLatexTableForIndirectAssignments(InputsHandler.Inputs inputs, OutputsHandler.Output output){
        StringBuilder table = new StringBuilder();
        table.append(FIRST_ROWS);
        table.append(SECOND_ROW_INDIRECT);

        for (int i = 0; i < inputs.getAlternativesIds().size(); i++) {
            table.append(HLINE);
            String alternativeId = inputs.getAlternativesIds().get(i);
            String lowerBound = inputs.getAssignments().get(alternativeId).get("LOWER");
            String upperBound = inputs.getAssignments().get(alternativeId).get("UPPER");
            table.append(alternativeId + SEPARATOR + lowerBound + SEPARATOR + upperBound + NEW_LINE);
        }

        table.append(END_ROW);

        output.setLatexTable(table.toString());
    }

}
