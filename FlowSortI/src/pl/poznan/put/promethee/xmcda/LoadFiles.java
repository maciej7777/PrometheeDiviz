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
                new InputFile("methodParameters", "programParameters", "method_parametres.xml", true));
        files.put("categoryProfiles", new InputFile("categoriesProfiles", "categoriesProfiles", "categories_profiles.xml", true));
        files.put("alternatives", new InputFile("alternatives", "alternatives", "alternatives.xml", true));
        files.put("categories", new InputFile("categories", "categories", "categories.xml", true));
        files.put("categories", new InputFile("categories", "categoriesValues", "categories.xml", true));
        files.put("positive_flows", new InputFile("alternativesValues", "alternativesValues", "positive_flows.xml", true));
        files.put("negative_flows", new InputFile("alternativesValues", "alternativesValues", "negative_flows.xml", true));

        return files;
    }
}
