package pl.poznan.put.promethee.xmcda;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.Referenceable;
import org.xmcda.XMCDA;
import org.xmcda.converters.v2_v3.XMCDAConverter;
import org.xmcda.parsers.xml.xmcda_v2.XMCDAParser;
import pl.poznan.put.promethee.FlowSortII;

import java.io.File;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-12-04.
 */
public class FlowSortIIXMCDAv2 {
    public static void main(String[] args) throws Utils.InvalidCommandLineException {
        final Utils.Arguments params = Utils.parseCmdLineArguments(args);
        final String indir = params.inputDirectory;
        final String outdir = params.outputDirectory;
        final File prgExecResultsFile = new File(outdir, "messages.xml");
        final ProgramExecutionResult executionResult = new ProgramExecutionResult();

        final org.xmcda.XMCDA xmcda;
        org.xmcda.v2.XMCDA xmcda_v2 = new org.xmcda.v2.XMCDA();

        Referenceable.DefaultCreationObserver.currentMarker = "alternatives";
        Utils.loadXMCDAv2(xmcda_v2, new File(indir, "alternatives.xml"), true, executionResult, "alternatives");
        Referenceable.DefaultCreationObserver.currentMarker = "categories";
        Utils.loadXMCDAv2(xmcda_v2, new File(indir, "categories.xml"), true, executionResult, "categories");
        Referenceable.DefaultCreationObserver.currentMarker = "categoriesValues";
        Utils.loadXMCDAv2(xmcda_v2, new File(indir, "categories.xml"), true, executionResult, "categoriesValues");
        Referenceable.DefaultCreationObserver.currentMarker = "categoriesProfiles";
        Utils.loadXMCDAv2(xmcda_v2, new File(indir, "categories_profiles.xml"), true, executionResult, "categoriesProfiles");
        Referenceable.DefaultCreationObserver.currentMarker = "flows";
        Utils.loadXMCDAv2(xmcda_v2, new File(indir, "flows.xml"), true, executionResult, "alternativesValues");
        Referenceable.DefaultCreationObserver.currentMarker = "methodParameters";
        Utils.loadXMCDAv2(xmcda_v2, new File(indir, "method_parameters.xml"), true, executionResult, "methodParameters");

        if (!(executionResult.isOk() || executionResult.isWarning())) {
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
        }
        try {
            xmcda = XMCDAConverter.convertTo_v3(xmcda_v2);
        } catch (Throwable t) {
            executionResult.addError(Utils.getMessage("Could not convert inputs to XMCDA v3, reason: ", t));
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
            return;
        }
        final InputsHandler.Inputs inputs = InputsHandler.checkAndExtractInputs(xmcda, executionResult);
        if (!(executionResult.isOk() || executionResult.isWarning()) || inputs == null) {
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
        }
        final OutputsHandler.Output results;
        try {
            results = FlowSortII.sort(inputs);
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
