package pl.poznan.put.promethee.xmcda;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.Referenceable;
import org.xmcda.XMCDA;
import org.xmcda.converters.v2_v3.XMCDAConverter;
import org.xmcda.parsers.xml.xmcda_v2.XMCDAParser;
import pl.poznan.put.promethee.FlowSortI;

import java.io.File;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-12-04.
 */
public class FlowSortIXMCDAv2 {

    private static final ProgramExecutionResult executionResult = new ProgramExecutionResult();

    /**
     * Loads, converts and inserts the content of the XMCDA v2 {@code file} into {@code xmcda_v3}.
     * Updates {@link #executionResult} if an error occurs.
     *
     * @param file         the XMCDA v2 file to be loaded
     * @param marker       the marker to use, see {@link Referenceable.DefaultCreationObserver#currentMarker}
     * @param xmcda_v3     the object into which the content of {@file} is inserted
     * @param v2_tags_only the list of XMCDA v2 tags to be loaded
     */
    private static void convertToV3AndMark(File file, String marker, org.xmcda.XMCDA xmcda_v3,
                                           String... v2_tags_only) {
        final org.xmcda.v2.XMCDA xmcda_v2 = new org.xmcda.v2.XMCDA();
        Referenceable.DefaultCreationObserver.currentMarker = marker;
        Utils.loadXMCDAv2(xmcda_v2, file, true, executionResult, v2_tags_only);
        try {
            XMCDAConverter.convertTo_v3(xmcda_v2, xmcda_v3);
        } catch (Throwable t) {
            executionResult.addError(Utils.getMessage("Could not convert " + file.getPath() + " to XMCDA v3, reason: ", t));
        }
    }

    public static void main(String[] args) throws Utils.InvalidCommandLineException {
        final Utils.Arguments params = Utils.parseCmdLineArguments(args);
        final String indir = params.inputDirectory;
        final String outdir = params.outputDirectory;
        final File prgExecResultsFile = new File(outdir, "messages.xml");
        final ProgramExecutionResult executionResult = new ProgramExecutionResult();

        final org.xmcda.XMCDA xmcda = new org.xmcda.XMCDA();

        convertToV3AndMark(new File(indir, "alternatives.xml"), "alternatives", xmcda, "alternatives");
        convertToV3AndMark(new File(indir, "categories.xml"), "categories", xmcda, "categories");
        convertToV3AndMark(new File(indir, "categories.xml"), "categoriesValues", xmcda, "categoriesValues");
        convertToV3AndMark(new File(indir, "categories_profiles.xml"), "categoriesProfiles", xmcda, "categoriesProfiles");
        convertToV3AndMark(new File(indir, "positive_flows.xml"), "positiveFlows", xmcda, "alternativesValues");
        convertToV3AndMark(new File(indir, "negative_flows.xml"), "negativeFlows", xmcda, "alternativesValues");
        convertToV3AndMark(new File(indir, "method_parameters.xml"), "methodParameters", xmcda, "methodParameters");

        if (!(executionResult.isOk() || executionResult.isWarning())) {
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
        }

        final InputsHandler.Inputs inputs = InputsHandler.checkAndExtractInputs(xmcda, executionResult);
        if (!(executionResult.isOk() || executionResult.isWarning()) || inputs == null) {
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
        }
        final OutputsHandler.Output results;
        try {
            results = FlowSortI.sort(inputs);
        } catch (Throwable t) {
            executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", t));
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
            return;
        }

        final Map<String, XMCDA> x_results = OutputsHandler.convert(results.assignments, executionResult);

        org.xmcda.v2.XMCDA results_v2;
        for (String outputName : x_results.keySet()) {
            File outputFile = new File(outdir, String.format("%s.xml", outputName));
            try {
                results_v2 = XMCDAConverter.convertTo_v2(x_results.get(outputName));
                if (results_v2 == null)
                    throw new IllegalStateException("Conversion from v3 to v2 returned a null value");
            } catch (Throwable t) {
                final String err = String.format("Could not convert %s into XMCDA_v2, reason: ", outputName);
                executionResult.addError(Utils.getMessage(err, t));
                continue;
            }
            try {
                XMCDAParser.writeXMCDA(results_v2, outputFile, OutputsHandler.xmcdaV2Tag(outputName));
            } catch (Throwable t) {
                final String err = String.format("Error while writing %s.xml, reason: ", outputName);
                executionResult.addError(Utils.getMessage(err, t));
                outputFile.delete();
            }
        }
        Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
    }

}
