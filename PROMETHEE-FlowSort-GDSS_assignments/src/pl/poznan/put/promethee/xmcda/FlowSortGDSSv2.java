package pl.poznan.put.promethee.xmcda;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.Referenceable;
import org.xmcda.XMCDA;
import org.xmcda.converters.v2_v3.XMCDAConverter;
import org.xmcda.parsers.xml.xmcda_v2.XMCDAParser;
import pl.poznan.put.promethee.FlowSortGDSS;

import java.io.File;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-12-10.
 */
public class FlowSortGDSSv2 {

    //Tags (+ some markers which are also tags)
    private static final String ALTERNATIVES = "alternatives";
    private static final String ALTERNATIVES_COMPARISONS = "alternativesComparisons";
    private static final String ALTERNATIVES_VALUES = "alternativesValues";
    private static final String CATEGORIES = "categories";
    private static final String CATEGORIES_PROFILES = "categoriesProfiles";
    private static final String CATEGORIES_VALUES = "categoriesValues";
    private static final String CRITERIA = "criteria";
    private static final String CRITERIA_SCALES = "criteriaScales";
    private static final String METHOD_PARAMETERS = "methodParameters";
    private static final String PERFORMANCE_TABLE = "performanceTable";

    //Markers
    private static final String FLOWS = "flows";
    private static final String PREFERENCES = "preferences";
    private static final String PROFILES_FLOWS = "profilesFlows";


    private static final ProgramExecutionResult executionResult = new ProgramExecutionResult();

    private FlowSortGDSSv2() {
        
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
        convertToV3AndMark(xmcda, new File(indir, "alternatives.xml"), true, ALTERNATIVES, ALTERNATIVES);
        convertToV3AndMark(xmcda, new File(indir, "categories.xml"), true, CATEGORIES, CATEGORIES);
        convertToV3AndMark(xmcda, new File(indir, "categories.xml"), true, CATEGORIES_VALUES, CATEGORIES_VALUES);
        convertToV3AndMark(xmcda, new File(indir, "method_parameters.xml"), true, METHOD_PARAMETERS, METHOD_PARAMETERS);
        convertToV3AndMark(xmcda, new File(indir, "criteria.xml"), true, CRITERIA, CRITERIA);
        convertToV3AndMark(xmcda, new File(indir, "criteria.xml"), true, CRITERIA_SCALES, CRITERIA_SCALES);
        convertToV3AndMark(xmcda, new File(indir, "profiles_flows.xml"), true, PROFILES_FLOWS, ALTERNATIVES_VALUES);

        convertToV3AndMark(xmcda, new File(indir, "categories_profiles1.xml"), true, CATEGORIES_PROFILES + 1, CATEGORIES_PROFILES);
        convertToV3AndMark(xmcda, new File(indir, "categories_profiles2.xml"), true, CATEGORIES_PROFILES + 2, CATEGORIES_PROFILES);

        convertToV3AndMark(xmcda, new File(indir, "performance_table1.xml"), true, PERFORMANCE_TABLE + 1, PERFORMANCE_TABLE);
        convertToV3AndMark(xmcda, new File(indir, "performance_table2.xml"), true, PERFORMANCE_TABLE + 2, PERFORMANCE_TABLE);

        convertToV3AndMark(xmcda, new File(indir, "flows1.xml"), true, FLOWS + 1, ALTERNATIVES_VALUES);
        convertToV3AndMark(xmcda, new File(indir, "flows2.xml"), true, FLOWS + 2, ALTERNATIVES_VALUES);

        convertToV3AndMark(xmcda, new File(indir, "preferences1.xml"), true, PREFERENCES + 1, ALTERNATIVES_COMPARISONS);
        convertToV3AndMark(xmcda, new File(indir, "preferences2.xml"), true, PREFERENCES + 2, ALTERNATIVES_COMPARISONS);

        for (int i = 3; i <= 10; i++) {
            convertToV3AndMark(xmcda, new File(indir, "categories_profiles" + i + ".xml"), false, CATEGORIES_PROFILES + i, CATEGORIES_PROFILES);
            convertToV3AndMark(xmcda, new File(indir, "performance_table" + i + ".xml"), false, PERFORMANCE_TABLE + i, PERFORMANCE_TABLE);
            convertToV3AndMark(xmcda, new File(indir, "flows" + i + ".xml"), false, FLOWS + i, ALTERNATIVES_VALUES);
            convertToV3AndMark(xmcda, new File(indir, "preferences" + i + ".xml"), false, PREFERENCES + i, ALTERNATIVES_COMPARISONS);
        }
    }

    private static void handleResults(String outdir, Map<String, XMCDA> resultsMap) {
        org.xmcda.v2.XMCDA resultsV2;
        for ( Map.Entry<String, XMCDA> outputNameEntry : resultsMap.entrySet() )
        {
            File outputFile = new File(outdir, String.format("%s.xml", outputNameEntry.getKey()));
            try
            {
                resultsV2 = XMCDAConverter.convertTo_v2(outputNameEntry.getValue());
                if ( resultsV2 == null )
                    throw new IllegalStateException("Conversion from v3 to v2 returned a null value");
            }
            catch (Exception e)
            {
                final String err = String.format("Could not convert %s into XMCDA_v2, reason: ", outputNameEntry.getKey());
                executionResult.addError(Utils.getMessage(err, e));
                continue;
            }
            try
            {
                XMCDAParser.writeXMCDA(resultsV2, outputFile, OutputsHandler.xmcdaV2Tag(outputNameEntry.getKey()));
            }
            catch (Exception e)
            {
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
            results = FlowSortGDSS.sort(inputs);
        }
        catch (Exception e)
        {
            executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", e));
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
            return;
        }

        final Map<String, XMCDA> resultsMap = OutputsHandler.convert(results.getFirstStepAssignments(), results.getAssignments());

        handleResults(outdir, resultsMap);
        if (!executionResult.isError()) {
            executionResult.addDebug("Success");
        }
        Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
    }
}
