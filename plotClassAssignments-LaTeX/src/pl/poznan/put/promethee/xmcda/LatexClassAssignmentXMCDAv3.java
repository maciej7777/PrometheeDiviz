package pl.poznan.put.promethee.xmcda;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.Referenceable;
import org.xmcda.XMCDA;
import pl.poznan.put.promethee.LatexClassAssignment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Created by Maciej Uniejewski on 2016-12-26.
 */
public class LatexClassAssignmentXMCDAv3 {

    private LatexClassAssignmentXMCDAv3() {

    }

    private static void readFiles(XMCDA xmcda, String indir, ProgramExecutionResult executionResult) {
        Referenceable.DefaultCreationObserver.currentMarker = "alternatives";
        Utils.loadXMCDAv3(xmcda, new File(indir, "alternatives.xml"), true, executionResult, "alternatives");
        Referenceable.DefaultCreationObserver.currentMarker = "categories";
        Utils.loadXMCDAv3(xmcda, new File(indir, "categories.xml"), true, executionResult, "categories");
        Referenceable.DefaultCreationObserver.currentMarker = "categoriesValues";
        Utils.loadXMCDAv3(xmcda, new File(indir, "categories.xml"), true, executionResult, "categoriesValues");
        Referenceable.DefaultCreationObserver.currentMarker = "alternativesAssignments";
        Utils.loadXMCDAv3(xmcda, new File(indir, "assignments.xml"), true, executionResult, "alternativesAssignments");
    }

    public static void main(String[] args) throws Utils.InvalidCommandLineException {
        final Utils.Arguments params = Utils.parseCmdLineArguments(args);

        final String indir = params.inputDirectory;
        final String outdir = params.outputDirectory;

        final File prgExecResults = new File(outdir, "messages.xml");

        final ProgramExecutionResult executionResult = new ProgramExecutionResult();

        final XMCDA xmcda = new XMCDA();

        readFiles(xmcda, indir, executionResult);

        if ( ! (executionResult.isOk() || executionResult.isWarning() ) ) {
            Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
        }

        final InputsHandler.Inputs inputs = InputsHandler.checkAndExtractInputs(xmcda, executionResult);

        if ( ! ( executionResult.isOk() || executionResult.isWarning() ) || inputs == null ) {
            Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
        }

        final OutputsHandler.Output results;
        try
        {
            results = LatexClassAssignment.execute(inputs);
        }
        catch (Exception e) {
            executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", e));
            Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
            return;
        }

        try(  PrintWriter out = new PrintWriter( outdir+"/classAssignments.tex" )  ){
            out.println( results.getLatexTable() );
        } catch (FileNotFoundException e) {
            executionResult.addError("Output file cannot be created. Reason: " + e);
        }

        Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
    }
}
