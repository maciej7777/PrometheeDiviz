package pl.poznan.put.promethee.xmcda;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.Referenceable;
import org.xmcda.XMCDA;
import pl.poznan.put.promethee.FlowSortGDSS;

import java.io.File;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-12-10.
 */
public class FlowSortGDSSv3 {
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
        Referenceable.DefaultCreationObserver.currentMarker="methodParameters";
        Utils.loadXMCDAv3(xmcda, new File(indir, "method_parameters.xml"), true, executionResult, "programParameters");
        Referenceable.DefaultCreationObserver.currentMarker="criteria";
        Utils.loadXMCDAv3(xmcda, new File(indir, "criteria.xml"), true, executionResult, "criteria");
        Referenceable.DefaultCreationObserver.currentMarker="criteriaScales";
        Utils.loadXMCDAv3(xmcda, new File(indir, "criteria.xml"), true, executionResult, "criteriaScales");

        Referenceable.DefaultCreationObserver.currentMarker="profilesFlows";
        Utils.loadXMCDAv3(xmcda, new File(indir, "profilesFlows.xml"), true, executionResult, "alternativesValues");


        Referenceable.DefaultCreationObserver.currentMarker="categoriesProfiles1";
        Utils.loadXMCDAv3(xmcda, new File(indir, "categories_profiles1.xml"), true, executionResult, "categoriesProfiles");
        Referenceable.DefaultCreationObserver.currentMarker="categoriesProfiles2";
        Utils.loadXMCDAv3(xmcda, new File(indir, "categories_profiles2.xml"), true, executionResult, "categoriesProfiles");

        Referenceable.DefaultCreationObserver.currentMarker="performanceTable1";
        Utils.loadXMCDAv3(xmcda, new File(indir, "performance_table1.xml"), true, executionResult, "performanceTable");
        Referenceable.DefaultCreationObserver.currentMarker="performanceTable2";
        Utils.loadXMCDAv3(xmcda, new File(indir, "performance_table2.xml"), true, executionResult, "performanceTable");

        Referenceable.DefaultCreationObserver.currentMarker="flows1";
        Utils.loadXMCDAv3(xmcda, new File(indir, "flows1.xml"), true, executionResult, "alternativesValues");
        Referenceable.DefaultCreationObserver.currentMarker="flows2";
        Utils.loadXMCDAv3(xmcda, new File(indir, "flows2.xml"), true, executionResult, "alternativesValues");

        Referenceable.DefaultCreationObserver.currentMarker="preferences1";
        Utils.loadXMCDAv3(xmcda, new File(indir, "preferences1.xml"), true, executionResult, "alternativesMatrix");
        Referenceable.DefaultCreationObserver.currentMarker="preferences2";
        Utils.loadXMCDAv3(xmcda, new File(indir, "preferences2.xml"), true, executionResult, "alternativesMatrix");

        for (int i = 3; i <= 10; i++) {
            Referenceable.DefaultCreationObserver.currentMarker="categoriesProfiles" + i;
            Utils.loadXMCDAv3(xmcda, new File(indir, "categories_profiles" + i + ".xml"), false, executionResult, "categoriesProfiles");
            Referenceable.DefaultCreationObserver.currentMarker="performanceTable" + i;
            Utils.loadXMCDAv3(xmcda, new File(indir, "performance_table" + i + ".xml"), false, executionResult, "performanceTable");
            Referenceable.DefaultCreationObserver.currentMarker="flows" + i;
            Utils.loadXMCDAv3(xmcda, new File(indir, "flows" + i + ".xml"), false, executionResult, "alternativesValues");
            Referenceable.DefaultCreationObserver.currentMarker="preferences" + i;
            Utils.loadXMCDAv3(xmcda, new File(indir, "preferences" + i + ".xml"), false, executionResult, "alternativesMatrix");
        }


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
            results = FlowSortGDSS.sort(inputs);
        }
        catch (Throwable t) {
            executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", t));
            Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
            return;
        }

        //convert results
        Map<String, XMCDA> x_results = OutputsHandler.convert(results.firstStepAssignments, results.assignments, executionResult);

        //write results
        final org.xmcda.parsers.xml.xmcda_v3.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_v3.XMCDAParser();

        for ( String key : x_results.keySet() )
        {
            File outputFile = new File(outdir, String.format("%s.xml", key));
            try
            {
                parser.writeXMCDA(x_results.get(key), outputFile, OutputsHandler.xmcdaV3Tag(key));
            }
            catch (Throwable throwable)
            {
                final String err = String.format("Error while writing %s.xml, reason: ", key);
                executionResult.addError(Utils.getMessage(err, throwable));
                // Whatever the error is, clean up the file: we do not want to leave an empty or partially-written file
                outputFile.delete();
            }
        }

        Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
    }
}
