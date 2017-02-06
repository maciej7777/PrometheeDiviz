package pl.poznan.put.promethee.xmcda;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.Referenceable;
import org.xmcda.XMCDA;
import org.xmcda.converters.v2_v3.XMCDAConverter;
import org.xmcda.parsers.xml.xmcda_v2.XMCDAParser;
import pl.poznan.put.promethee.PrometheeTri;

import java.io.File;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-12-03.
 */
public class PrometheeTriXMCDAv2 {
    private static final ProgramExecutionResult executionResult = new ProgramExecutionResult();

    private PrometheeTriXMCDAv2() {

    }

    /**
     * Loads, converts and inserts the content of the XMCDA v2 {@code file} into {@code xmcdaV3}.
     * Updates {@link #executionResult} if an error occurs.
     *
     * @param file         the XMCDA v2 file to be loaded
     * @param marker       the marker to use, see {@link Referenceable.DefaultCreationObserver#currentMarker}
     * @param xmcdaV3     the object into which the content of {@file} is inserted
     * @param v2TagsOnly the list of XMCDA v2 tags to be loaded
     */
    private static void convertToV3AndMark(File file, String marker, org.xmcda.XMCDA xmcdaV3,
                                           String... v2TagsOnly) {
        final org.xmcda.v2.XMCDA xmcdaV2 = new org.xmcda.v2.XMCDA();
        Referenceable.DefaultCreationObserver.currentMarker = marker;
        Utils.loadXMCDAv2(xmcdaV2, file, true, executionResult, v2TagsOnly);
        try {
            XMCDAConverter.convertTo_v3(xmcdaV2, xmcdaV3);
        } catch (Exception e) {
            executionResult.addError(Utils.getMessage("Could not convert " + file.getPath() + " to XMCDA v3, reason: ", e));
        }
    }

    private static void readFiles(XMCDA xmcda, String indir) {
        convertToV3AndMark(new File(indir,  "alternatives.xml"), "alternatives", xmcda, "alternatives");
        convertToV3AndMark(new File(indir,  "categories.xml"), "categories", xmcda, "categories");
        convertToV3AndMark(new File(indir,  "categories.xml"), "categoriesValues", xmcda, "categoriesValues");
        convertToV3AndMark(new File(indir,  "categories_profiles.xml"), "categoriesProfiles", xmcda, "categoriesProfiles");
        convertToV3AndMark(new File(indir,  "criteria.xml"), "criteria", xmcda, "criteria");
        convertToV3AndMark(new File(indir,  "weights.xml"), "weights", xmcda, "criteriaValues");
        convertToV3AndMark(new File(indir,  "preferences.xml"), "preferences", xmcda, "alternativesComparisons");
        convertToV3AndMark(new File(indir,  "method_parameters.xml"), "methodParameters", xmcda, "methodParameters");
    }

    private static void handleResults(String outdir, Map<String, XMCDA> xResults) {
        org.xmcda.v2.XMCDA resultsV2;
        for (Map.Entry<String, XMCDA> outputNameEntry : xResults.entrySet()) {
            File outputFile = new File(outdir, String.format("%s.xml", outputNameEntry.getKey()));
            try {
                resultsV2 = XMCDAConverter.convertTo_v2(outputNameEntry.getValue());
                if (resultsV2 == null)
                    throw new IllegalStateException("Conversion from v3 to v2 returned a null value");
            } catch (Exception e) {
                final String err = String.format("Could not convert %s into XMCDA_v2, reason: ", outputNameEntry.getKey());
                executionResult.addError(Utils.getMessage(err, e));
                continue;
            }
            try {
                XMCDAParser.writeXMCDA(resultsV2, outputFile, OutputsHandler.xmcdaV2Tag(outputNameEntry.getKey()));
            } catch (Exception e) {
                final String err = String.format("Error while writing %s.xml, reason: ", outputNameEntry.getKey());
                executionResult.addError(Utils.getMessage(err, e));
                outputFile.delete();
            }
        }
    }

    public static void main(String[] args) throws Utils.InvalidCommandLineException {
        final Utils.Arguments params = Utils.parseCmdLineArguments(args);
        final String indir = params.inputDirectory;
        final String outdir = params.outputDirectory;
        final File prgExecResultsFile = new File(outdir, "messages.xml");
        final ProgramExecutionResult executionResult = new ProgramExecutionResult();

        final org.xmcda.XMCDA xmcda = new org.xmcda.XMCDA();

        readFiles(xmcda, indir);


        if (!(executionResult.isOk() || executionResult.isWarning())) {
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
        }

        final InputsHandler.Inputs inputs = InputsHandler.checkAndExtractInputs(xmcda, executionResult);
        if (!(executionResult.isOk() || executionResult.isWarning()) || inputs == null) {
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
        }
        final OutputsHandler.Output results;
        try {
            results = PrometheeTri.sort(inputs);
        } catch (Exception e) {
            executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", e));
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
            return;
        }

        final Map<String, XMCDA> xResults = OutputsHandler.convert(results.getAssignments());

        handleResults(outdir, xResults);
        if (!executionResult.isError()) {
            executionResult.addDebug("Success");
        }
        Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
    }

}