package pl.poznan.put.promethee.xmcda;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.Referenceable;
import org.xmcda.XMCDA;
import pl.poznan.put.promethee.Promsort;

import java.io.File;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-11-01.
 */
public class PromsortXMCDAv3 {

    private PromsortXMCDAv3() {

    }

    public static void main(String[] args) throws Utils.InvalidCommandLineException {
        final Utils.Arguments params = Utils.parseCmdLineArguments(args);

        final String indir = params.inputDirectory;
        final String outdir = params.outputDirectory;

        final File prgExecResults = new File(outdir, "messages.xml");

        final ProgramExecutionResult executionResult = new ProgramExecutionResult();

        final XMCDA xmcda = new XMCDA();

        Referenceable.DefaultCreationObserver.currentMarker="alternatives";
        Utils.loadXMCDAv3(xmcda, new File(indir, "alternatives.xml"), true, executionResult, "alternatives");
        Referenceable.DefaultCreationObserver.currentMarker="categories";
        Utils.loadXMCDAv3(xmcda, new File(indir, "categories.xml"), true, executionResult, "categories");
        Referenceable.DefaultCreationObserver.currentMarker="categoriesValues";
        Utils.loadXMCDAv3(xmcda, new File(indir, "categories.xml"), true, executionResult, "categoriesValues");
        Referenceable.DefaultCreationObserver.currentMarker="categoriesProfiles";
        Utils.loadXMCDAv3(xmcda, new File(indir, "categories_profiles.xml"), true, executionResult, "categoriesProfiles");
        Referenceable.DefaultCreationObserver.currentMarker="positiveFlows";
        Utils.loadXMCDAv3(xmcda, new File(indir, "positive_flows.xml"), true, executionResult, "alternativesValues");
        Referenceable.DefaultCreationObserver.currentMarker="negativeFlows";
        Utils.loadXMCDAv3(xmcda, new File(indir, "negative_flows.xml"), true, executionResult, "alternativesValues");
        Referenceable.DefaultCreationObserver.currentMarker="methodParameters";
        Utils.loadXMCDAv3(xmcda, new File(indir, "method_parameters.xml"), true, executionResult, "programParameters");

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
            results = Promsort.sort(inputs);
        }
        catch (Exception e) {
            executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", e));
            Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
            return;
        }

        //convert results
        Map<String, XMCDA> xResults = OutputsHandler.convert(results.getFirstStepAssignments(), results.getFinalAssignments());

        //write results
        final org.xmcda.parsers.xml.xmcda_v3.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_v3.XMCDAParser();

        for ( Map.Entry<String, XMCDA> keyEntry : xResults.entrySet() )
        {
            File outputFile = new File(outdir, String.format("%s.xml", keyEntry.getKey()));
            try
            {
                parser.writeXMCDA(keyEntry.getValue(), outputFile, OutputsHandler.xmcdaV3Tag(keyEntry.getKey()));
            }
            catch (Exception exception)
            {
                final String err = String.format("Error while writing %s.xml, reason: ", keyEntry.getKey());
                executionResult.addError(Utils.getMessage(err, exception));
                // Whatever the error is, clean up the file: we do not want to leave an empty or partially-written file
                outputFile.delete();
            }
        }

        Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
    }
}
