package pl.poznan.put.promethee.xmcda;

import org.xmcda.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-12-26.
 */
public class OutputsHandler {

    private static final String MESSAGES = "messages";

    private OutputsHandler() {
    }

    public static class Output {
        private String latexTable;

        public String getLatexTable() {
            return latexTable;
        }

        public void setLatexTable(String latexTable) {
            this.latexTable = latexTable;
        }
    }

    public static final String xmcdaV3Tag(String outputName) {
        switch (outputName) {
            case MESSAGES:
                return "programExecutionResult";
            default:
                throw new IllegalArgumentException(String.format("Unknown output name '%s'", outputName));
        }
    }

    public static final String xmcdaV2Tag(String outputName) {
        switch (outputName) {
            case MESSAGES:
                return "methodMessages";
            default:
                throw new IllegalArgumentException(String.format("Unknown output name '%s'", outputName));
        }
    }
}
