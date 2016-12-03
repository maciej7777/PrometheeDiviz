package pl.poznan.put.promethee.xmcda;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Maciej Uniejewski on 2016-11-11.
 */
public class LoadFiles {
    public static Map<String, InputFile> initFiles() {
        Map<String, InputFile> files = new LinkedHashMap<>();
        files.put("methodParameters",
                new InputFile("methodParameters", "programParameters", "method_parameters.xml", true));
        files.put("categoryProfiles", new InputFile("categoriesProfiles", "categoriesProfiles", "categories_profiles.xml", true));
        files.put("alternatives", new InputFile("alternatives", "alternatives", "alternatives.xml", true));
        files.put("categories", new InputFile("categories", "categories", "categories.xml", true));
        files.put("categories", new InputFile("categories", "categoriesValues", "categories.xml", true));
        files.put("flows", new InputFile("alternativesValues", "alternativesValues", "flows.xml", true));

        return files;
    }
}
