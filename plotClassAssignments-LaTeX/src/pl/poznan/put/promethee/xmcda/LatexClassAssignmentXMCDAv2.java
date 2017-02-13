package pl.poznan.put.promethee.xmcda;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.Referenceable;
import org.xmcda.XMCDA;
import org.xmcda.converters.v2_v3.XMCDAConverter;
import pl.poznan.put.promethee.LatexClassAssignment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Created by Maciej Uniejewski on 2016-12-26.
 */
public class LatexClassAssignmentXMCDAv2 {

    private static final ProgramExecutionResult executionResult = new ProgramExecutionResult();

    private LatexClassAssignmentXMCDAv2() {

    }

    /**
     * Loads, converts and inserts the content of the XMCDA v2 {@code file} into {@code xmcdaV3}.
     * Updates {@link #executionResult} if an error occurs.
     *
     * @param xmcdaV3     the object into which the content of {@file} is inserted
     * @param file         the XMCDA v2 file to be loaded
     * @param isMandatory   information if file is mandatory
     * @param marker       the marker to use, see {@link Referenceable.DefaultCreationObserver#currentMarker}
     * @param v2TagsOnly the list of XMCDA v2 tags to be loaded
     */
    private static void convertToV3AndMark(org.xmcda.XMCDA xmcdaV3, File file, boolean isMandatory, String marker,
                                           String... v2TagsOnly) {
        final org.xmcda.v2.XMCDA xmcdaV2 = new org.xmcda.v2.XMCDA();
        Referenceable.DefaultCreationObserver.currentMarker = marker;
        Utils.loadXMCDAv2(xmcdaV2, file, isMandatory, executionResult, v2TagsOnly);
        try {
            XMCDAConverter.convertTo_v3(xmcdaV2, xmcdaV3);
        } catch (Exception e) {
            executionResult.addError(Utils.getMessage("Could not convert " + file.getPath() + " to XMCDA v3, reason: ", e));
        }
    }

    private static void readFiles(XMCDA xmcda, String indir) {
        convertToV3AndMark(xmcda, new File(indir, "alternatives.xml"), true, "alternatives", "alternatives");
        convertToV3AndMark(xmcda, new File(indir, "categories.xml"), true, "categories", "categories");
        convertToV3AndMark(xmcda, new File(indir, "categories_values.xml"), true, "categoriesValues", "categoriesValues");
        convertToV3AndMark(xmcda, new File(indir, "assignments.xml"), true, "alternativesAssignments", "alternativesAffectations");
    }

    public static void main(String[] args) throws Utils.InvalidCommandLineException {
        final Utils.Arguments params = Utils.parseCmdLineArguments(args);
        final String indir = params.inputDirectory;
        final String outdir = params.outputDirectory;
        final File prgExecResultsFile = new File(outdir, "messages.xml");

        final org.xmcda.XMCDA xmcda = new org.xmcda.XMCDA();

        readFiles(xmcda, indir);

        if ( ! (executionResult.isOk() || executionResult.isWarning() ) )
        {
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
        }

        final InputsHandler.Inputs inputs = InputsHandler.checkAndExtractInputs(xmcda, executionResult);
        if ( ! ( executionResult.isOk() || executionResult.isWarning() ) || inputs == null )
        {
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
        }
        final OutputsHandler.Output results ;
        try
        {
            results = LatexClassAssignment.execute(inputs);
        }
        catch (Exception e)
        {
            executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", e));
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
            return;
        }
        try(  PrintWriter out = new PrintWriter( outdir+"/classAssignments.tex" )  ){
            out.println( results.getLatexTable() );
        } catch (FileNotFoundException e) {
            executionResult.addError("Output file cannot be created. Reason: " + e);
        }

        if (!executionResult.isError()) {
            executionResult.addDebug("Success");
        }
        Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
    }
}
