/** 
 * Copyright QDB Platform to Present
 * All rights Reserved.
 * 
 * Contributors
 * 11-12-2020 PerumalRaja - Settings Class
 *
 */
package com.atp.commonfiles;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.google.gson.JsonObject;

// TODO: Auto-generated Javadoc
/**
 * The Class Settings.
 */
public class Settings {

	/** The prop. */
	static Properties prop;

	/**
	 * Gets the.
	 *
	 * @param prop_name the prop name
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static String get(String prop_name) throws IOException {
		if (prop == null)
			prop = readPropertiesFile("application.properties");
		return prop.getProperty(prop_name);
	}

	/**
	 * Gets the group properities.
	 *
	 * @param prop_name the prop name
	 * @return the group
	 */
	public static JsonObject getgroup(String prop_name) {
		JsonObject result = null;

		if (prop.get(prop_name + "EnableProperty") != null & (boolean) prop.get(prop_name + "EnableProperty")) {
			result = new JsonObject();
			for (Object key : prop.stringPropertyNames()) {

				if (key.toString().indexOf(prop_name) != -1)
					result.addProperty(key.toString().replace(prop_name, ""), prop.get(key).toString());

			}
		}
		return result;
	}

	/**
	 * Read properties file.
	 *
	 * @param fileName the file name
	 * @return the properties
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static Properties readPropertiesFile(String fileName) throws IOException {
		FileInputStream fis = null;
		Properties prop = null;
		try {
			fis = new FileInputStream(fileName);
			prop = new Properties();
			prop.load(fis);
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			fis.close();
			fis = null;
		}
		return prop;
	}

}
