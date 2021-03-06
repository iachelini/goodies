package de.mtrail.goodies.internal.workspacesupport.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.mtrail.goodies.internal.workspacesupport.model.BundleConfig;
import de.mtrail.goodies.internal.workspacesupport.model.State;

public class WorkspacePropertiesReader {

	/**
	 * Reads the configuration file from the parameter path and maps all entries to
	 * the unique bundle name and an instance of {@link BundleConfig}.
	 *
	 * @param pathToWorkspaceConfig absolute path to the configuration file
	 * @return Map with bundlename and BundleConfig
	 * @throws IOException {@link Properties#load(java.io.InputStream)}
	 */
	public static Map<String, BundleConfig> createConfiguration(final String pathToWorkspaceConfig) throws IOException {
		final Properties workspaceProperties = readProperties(pathToWorkspaceConfig);
		final Map<String, BundleConfig> bundleConfiguration = new HashMap<>();
		workspaceProperties.forEach((k, v) -> addToMap(bundleConfiguration, (String) k, (String) v));
		return bundleConfiguration;
	}

	private static Properties readProperties(final String pathToWorkspaceConfig)
			throws IOException, FileNotFoundException {
		final Properties workspaceProperties = new Properties();
		try (InputStream is = new FileInputStream(pathToWorkspaceConfig)) {
			workspaceProperties.load(is);
		}
		return workspaceProperties;
	}

	private static void addToMap(final Map<String, BundleConfig> bundleConfiguration, final String k, final String v) {
		final String bundleName = extractBundleName(k);
		final BundleConfig bundleConfig = bundleConfiguration.computeIfAbsent(bundleName,
				b -> new BundleConfig(bundleName));

		final String valueKey = valueKey(k);
		switch (valueKey) {
		case WorkspacePropertiesConstants.WORKINGSET:
			bundleConfig.setWorkingSetName(v);
			break;
		case WorkspacePropertiesConstants.STATE:
			bundleConfig.setState(State.valueOf(v));
			break;
		}
	}

	private static String extractBundleName(final String k) {
		return k.substring(0, k.lastIndexOf('.'));
	}

	private static String valueKey(final String k) {
		return k.substring(k.lastIndexOf('.') + 1);
	}
}