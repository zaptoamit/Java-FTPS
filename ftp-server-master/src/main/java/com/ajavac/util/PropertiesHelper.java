package com.ajavac.util;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Read the configuration file under config
 * Created by wyp0596 on 8/12/2016.
 */
public class PropertiesHelper {

    /**
     * Obtain the Properties object through the Properties file
     *
     * @param inputFilePath Enter the full path of the Properties file
     * @return Properties object
     */
    public static Properties getProperties(String inputFilePath) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(inputFilePath)) {
            properties.load(fileInputStream);
        }
        return properties;
    }

    /**
     * Write the Properties file through the Properties object
     *
     * @param properties     Properties object
     * @param outputFilePath Output the full path of the Properties file
     */
    public static void saveProperties(Properties properties, String outputFilePath) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath)) {
            properties.store(fileOutputStream);
        }
    }
}
