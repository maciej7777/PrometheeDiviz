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

    //Tags (+ some markers which are also tags)
    private static final String ALTERNATIVES = "alternatives";
    private static final String ALTERNATIVES_VALUES = "alternativesValues";
    private static final String ALTERNATIVES_MATRIX = "alternativesMatrix";
    private static final String CATEGORIES = "categories";
    private static final String CATEGORIES_PROFILES = "categoriesProfiles";
    private static final String CATEGORIES_VALUES = "categoriesValues";
    private static final String CRITERIA = "criteria";
    private static final String CRITERIA_SCALES = "criteriaScales";
    private static final String PERFORMANCE_TABLE = "performanceTable";
    private static final String PROGRAM_PARAMETERS = "programParameters";

    //Markers
    private static final String FLOWS = "flows";
    private static final String PREFERENCES = "preferences";
    private static final String PROFILES_FLOWS = "profiles_flows";
    private static final String METHOD_PARAMETERS = "methodParameters";

    private FlowSortGDSSv3() {

    }

    private static void readFiles(XMCDA xmcda, String indir, ProgramExecutionResult executionResult) {
        Referenceable.DefaultCreationObserver.currentMarker = ALTERNATIVES;
        Utils.loadXMCDAv3(xmcda, new File(indir, "alternatives.xml"), true, executionResult, ALTERNATIVES);
        Referenceable.DefaultCreationObserver.currentMarker = CATEGORIES;
        Utils.loadXMCDAv3(xmcda, new File(indir, "categories.xml"), true, executionResult, CATEGORIES);
        Referenceable.DefaultCreationObserver.currentMarker = CATEGORIES_VALUES;
        Utils.loadXMCDAv3(xmcda, new File(indir, "categories.xml"), true, executionResult, CATEGORIES_VALUES);
        Referenceable.DefaultCreationObserver.currentMarker = METHOD_PARAMETERS;
        Utils.loadXMCDAv3(xmcda, new File(indir, "method_parameters.xml"), true, executionResult, PROGRAM_PARAMETERS);
        Referenceable.DefaultCreationObserver.currentMarker = CRITERIA;
        Utils.loadXMCDAv3(xmcda, new File(indir, "criteria.xml"), true, executionResult, CRITERIA);
        Referenceable.DefaultCreationObserver.currentMarker = CRITERIA_SCALES;
        Utils.loadXMCDAv3(xmcda, new File(indir, "criteria.xml"), true, executionResult, CRITERIA_SCALES);
        Referenceable.DefaultCreationObserver.currentMarker = PROFILES_FLOWS;
        Utils.loadXMCDAv3(xmcda, new File(indir, "profiles_flows.xml"), true, executionResult, ALTERNATIVES_VALUES);

        Referenceable.DefaultCreationObserver.currentMarker = CATEGORIES_PROFILES + 1;
        Utils.loadXMCDAv3(xmcda, new File(indir, "categories_profiles1.xml"), true, executionResult, CATEGORIES_PROFILES);
        Referenceable.DefaultCreationObserver.currentMarker = CATEGORIES_PROFILES + 2;
        Utils.loadXMCDAv3(xmcda, new File(indir, "categories_profiles2.xml"), true, executionResult, CATEGORIES_PROFILES);

        Referenceable.DefaultCreationObserver.currentMarker = PERFORMANCE_TABLE + 1;
        Utils.loadXMCDAv3(xmcda, new File(indir, "performance_table1.xml"), true, executionResult, PERFORMANCE_TABLE);
        Referenceable.DefaultCreationObserver.currentMarker = PERFORMANCE_TABLE + 2;
        Utils.loadXMCDAv3(xmcda, new File(indir, "performance_table2.xml"), true, executionResult, PERFORMANCE_TABLE);

        Referenceable.DefaultCreationObserver.currentMarker = FLOWS + 1;
        Utils.loadXMCDAv3(xmcda, new File(indir, "flows1.xml"), true, executionResult, ALTERNATIVES_VALUES);
        Referenceable.DefaultCreationObserver.currentMarker = FLOWS + 2;
        Utils.loadXMCDAv3(xmcda, new File(indir, "flows2.xml"), true, executionResult, ALTERNATIVES_VALUES);

        Referenceable.DefaultCreationObserver.currentMarker = PREFERENCES + 1;
        Utils.loadXMCDAv3(xmcda, new File(indir, "preferences1.xml"), true, executionResult, ALTERNATIVES_MATRIX);
        Referenceable.DefaultCreationObserver.currentMarker = PREFERENCES + 2;
        Utils.loadXMCDAv3(xmcda, new File(indir, "preferences2.xml"), true, executionResult, ALTERNATIVES_MATRIX);

        for (int i = 3; i <= 10; i++) {
            Referenceable.DefaultCreationObserver.currentMarker = CATEGORIES_PROFILES + i;
            Utils.loadXMCDAv3(xmcda, new File(indir, "categories_profiles" + i + ".xml"), false, executionResult, CATEGORIES_PROFILES);
            Referenceable.DefaultCreationObserver.currentMarker = PERFORMANCE_TABLE + i;
            Utils.loadXMCDAv3(xmcda, new File(indir, "performance_table" + i + ".xml"), false, executionResult, PERFORMANCE_TABLE);
            Referenceable.DefaultCreationObserver.currentMarker = FLOWS + i;
            Utils.loadXMCDAv3(xmcda, new File(indir, "flows" + i + ".xml"), false, executionResult, ALTERNATIVES_VALUES);
            Referenceable.DefaultCreationObserver.currentMarker = PREFERENCES + i;
            Utils.loadXMCDAv3(xmcda, new File(indir, "preferences" + i + ".xml"), false, executionResult, ALTERNATIVES_MATRIX);
        }
    }

    private static void handleResults(String outdir, Map<String, XMCDA> resultsMap, ProgramExecutionResult executionResult) {
        //write results
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
                // Whatever the error is, clean up the file: we do not want to leave an empty or partially-written file
                outputFile.delete();
            }
        }
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
            results = FlowSortGDSS.sort(inputs);
        }
        catch (Exception e) {
            executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", e));
            Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
            return;
        }

        //convert results
        Map<String, XMCDA> resultsMap = OutputsHandler.convert(results.getFirstStepAssignments(), results.getAssignments());

        handleResults(outdir, resultsMap, executionResult);
        Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
    }
}
