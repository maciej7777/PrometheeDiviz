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
    private static void convertToV3AndMark(org.xmcda.XMCDA xmcda_v3, File file, boolean isMandatory, String marker,
                                           String... v2_tags_only) {
        final org.xmcda.v2.XMCDA xmcda_v2 = new org.xmcda.v2.XMCDA();
        Referenceable.DefaultCreationObserver.currentMarker = marker;
        Utils.loadXMCDAv2(xmcda_v2, file, isMandatory, executionResult, v2_tags_only);
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

        final org.xmcda.XMCDA xmcda = new org.xmcda.XMCDA();

        convertToV3AndMark(xmcda, new File(indir, "alternatives.xml"), true, "alternatives", "alternatives");
        convertToV3AndMark(xmcda, new File(indir, "categories.xml"), true, "categories", "categories");
        convertToV3AndMark(xmcda, new File(indir, "categories.xml"), true, "categoriesValues", "categoriesValues");
        convertToV3AndMark(xmcda, new File(indir, "method_parameters.xml"), true, "methodParameters", "methodParameters");
        convertToV3AndMark(xmcda, new File(indir, "criteria.xml"), true, "criteria", "criteria");
        convertToV3AndMark(xmcda, new File(indir, "criteria.xml"), true, "criteriaScales", "criteriaScales");
        convertToV3AndMark(xmcda, new File(indir, "profiles_flows.xml"), true, "profiles_flows", "alternativesValues");

        convertToV3AndMark(xmcda, new File(indir, "categories_profiles1.xml"), true, "categoriesProfiles1", "categoriesProfiles");
        convertToV3AndMark(xmcda, new File(indir, "categories_profiles2.xml"), true, "categoriesProfiles2", "categoriesProfiles");

        convertToV3AndMark(xmcda, new File(indir, "performance_table1.xml"), true, "performanceTable1", "performanceTable");
        convertToV3AndMark(xmcda, new File(indir, "performance_table2.xml"), true, "performanceTable2", "performanceTable");

        convertToV3AndMark(xmcda, new File(indir, "flows1.xml"), true, "flows1", "alternativesValues");
        convertToV3AndMark(xmcda, new File(indir, "flows2.xml"), true, "flows2", "alternativesValues");

        convertToV3AndMark(xmcda, new File(indir, "preferences1.xml"), true, "preferences1", "alternativesComparisons");
        convertToV3AndMark(xmcda, new File(indir, "preferences2.xml"), true, "preferences2", "alternativesComparisons");

        for (int i = 3; i <= 10; i++) {
            convertToV3AndMark(xmcda, new File(indir, "categories_profiles" + i + ".xml"), false, "categoriesProfiles" + i, "categoriesProfiles");
            convertToV3AndMark(xmcda, new File(indir, "performance_table" + i + ".xml"), false, "performanceTable" + i, "performanceTable");
            convertToV3AndMark(xmcda, new File(indir, "flows" + i + ".xml"), false, "flows" + i, "alternativesValues");
            convertToV3AndMark(xmcda, new File(indir, "preferences" + i + ".xml"), false, "preferences" + i, "alternativesComparisons");
        }

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
        catch (Throwable t)
        {
            executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", t));
            Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
            return;
        }

        final Map<String, XMCDA> x_results = OutputsHandler.convert(results.firstStepAssignments, results.assignments, executionResult);

        org.xmcda.v2.XMCDA results_v2;
        for ( String outputName : x_results.keySet() )
        {
            File outputFile = new File(outdir, String.format("%s.xml", outputName));
            try
            {
                results_v2 = XMCDAConverter.convertTo_v2(x_results.get(outputName));
                if ( results_v2 == null )
                    throw new IllegalStateException("Conversion from v3 to v2 returned a null value");
            }
            catch (Throwable t)
            {
                final String err = String.format("Could not convert %s into XMCDA_v2, reason: ", outputName);
                executionResult.addError(Utils.getMessage(err, t));
                continue;
            }
            try
            {
                XMCDAParser.writeXMCDA(results_v2, outputFile, OutputsHandler.xmcdaV2Tag(outputName));
            }
            catch (Throwable t)
            {
                final String err = String.format("Error while writing %s.xml, reason: ", outputName);
                executionResult.addError(Utils.getMessage(err, t));
                outputFile.delete();
            }
        }
        Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v2);
    }
}
