package pl.poznan.put.promethee.xmcda;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.Referenceable;
import org.xmcda.XMCDA;
import pl.poznan.put.promethee.DrawClassAssignment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Created by Maciej Uniejewski on 2016-12-26.
 */
public class DrawClassAssignmentXMCDAv3 {

    private DrawClassAssignmentXMCDAv3() {

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
        Referenceable.DefaultCreationObserver.currentMarker="methodParameters";
        Utils.loadXMCDAv3(xmcda, new File(indir, "method_parameters.xml"), true, executionResult, "programParameters");
    }

/*    private static void handleResults(String outdir, Map<String, XMCDA> resultsMap, ProgramExecutionResult executionResult) {

        final org.xmcda.parsers.xml.xmcda_v3.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_v3.XMCDAParser();

        for ( Map.Entry<String, XMCDA> entry : resultsMap.entrySet() )
        {
            File outputFile = new File(outdir, String.format("%s.xml", entry.getKey()));
            try
            {
                parser.writeXMCDA(entry.getValue(), outputFile, OutputsHandler.xmcdaV3Tag(entry.getKey()));
            }
            catch (Exception exception)
            {
                final String err = String.format("Error while writing %s.xml, reason: ", entry.getKey());
                executionResult.addError(Utils.getMessage(err, exception));
                outputFile.delete();
            }
        }
    }*/

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
            results = DrawClassAssignment.execute(inputs);
        }
        catch (Exception e) {
            executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", e));
            Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
            return;
        }

        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(results.getAssignmentsImage()));
            ImageIO.write(img,"PNG",new File(outdir + "/assignments.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }


/*
        Map<String, XMCDA> resultsMap = OutputsHandler.convert(results.getFirstStepAssignments(), results.getAssignments());

        handleResults(outdir, resultsMap, executionResult);*/
        Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
    }
}
