/*
 * Copyright (c) 2020 Redfield AB.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, Version 3, as
 * published by the Free Software Foundation.
 *  
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package se.redfield.cp.settings;

import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.utils.ColumnPatternExtractor;

/**
 * Classification settings.
 * 
 * @author Alexander Bondaletov
 *
 */
public class ClassifierSettings {
	/**
	 * Error rate settings key
	 */
	public static final String KEY_ERROR_RATE = "errorRate";
	private static final String KEY_CLASSES_AS_STRING = "classesAsString";
	private static final String KEY_STRING_SEPARATOR = "stringSeparator";

	private static final double DEFAULT_ERROR_RATE = 0.2;
	/**
	 * Default classes separator value
	 */
	public static final String DEFAULT_SEPARATOR = ";";
	/**
	 * Default classes column name
	 */
	public static final String DEFAULT_CLASSES_COLUMN_NAME = "Classes";

	/**
	 * Regular expression to match p-value columns
	 */
	public static final String P_VALUE_COLUMN_REGEX = "^p-value \\((?<value>.+)\\)$";

	private final SettingsModelDoubleBounded errorRate;
	private final SettingsModelBoolean classesAsString;
	private final SettingsModelString stringSeparator;

	private Map<String, Integer> scoreColumns;

	/**
	 * Creates new instance
	 */
	public ClassifierSettings() {
		errorRate = new SettingsModelDoubleBounded(KEY_ERROR_RATE, DEFAULT_ERROR_RATE, 0, 1);
		classesAsString = new SettingsModelBoolean(KEY_CLASSES_AS_STRING, false);
		stringSeparator = new SettingsModelString(KEY_STRING_SEPARATOR, DEFAULT_SEPARATOR);

		classesAsString.addChangeListener(e -> stringSeparator.setEnabled(classesAsString.getBooleanValue()));
		stringSeparator.setEnabled(classesAsString.getBooleanValue());
	}

	/**
	 * @return The error rate model.
	 */
	public SettingsModelDoubleBounded getErrorRateModel() {
		return errorRate;
	}

	/**
	 * @return The error rate.
	 */
	public double getErrorRate() {
		return errorRate.getDoubleValue();
	}

	/**
	 * @return The classesAsString model.
	 */
	public SettingsModelBoolean getClassesAsStringModel() {
		return classesAsString;
	}

	/**
	 * @return Whether to output classes as a string column instead of a collection
	 *         column.
	 */
	public boolean getClassesAsString() {
		return classesAsString.getBooleanValue();
	}

	/**
	 * @return The string separator model
	 */
	public SettingsModelString getStringSeparatorModel() {
		return stringSeparator;
	}

	/**
	 * @return The value of a classes separator
	 */
	public String getStringSeparator() {
		return stringSeparator.getStringValue();
	}

	/**
	 * @return The score columns for each class value.
	 */
	public Map<String, Integer> getScoreColumns() {
		if (scoreColumns == null) {
			throw new IllegalStateException("The settings object is not configured");
		}
		return scoreColumns;
	}

	/**
	 * @return The classes column name.
	 */
	public String getClassesColumnName() {
		return DEFAULT_CLASSES_COLUMN_NAME;
	}

	/**
	 * Loads settings from the provided {@link NodeSettingsRO}
	 * 
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void loadSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		errorRate.loadSettingsFrom(settings);
		classesAsString.loadSettingsFrom(settings);
		stringSeparator.loadSettingsFrom(settings);
	}

	/**
	 * Saves current settings into the given {@link NodeSettingsWO}.
	 * 
	 * @param settings
	 */
	public void saveSettingsTo(NodeSettingsWO settings) {
		errorRate.saveSettingsTo(settings);
		classesAsString.saveSettingsTo(settings);
		stringSeparator.saveSettingsTo(settings);
	}

	/**
	 * Validates internal consistency of the current settings
	 * 
	 * @throws InvalidSettingsException
	 */
	public void validate() throws InvalidSettingsException {
		if (getClassesAsString() && getStringSeparator().isEmpty()) {
			throw new InvalidSettingsException("String separator is empty");
		}
	}

	/**
	 * Validates settings stored in the provided {@link NodeSettingsRO}.
	 * 
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		ClassifierSettings temp = new ClassifierSettings();
		temp.loadSettingsFrom(settings);
		temp.validate();
	}

	/**
	 * Configures and validates the settings against input table spec. Score columns
	 * are extracted as a part of configure process.
	 * 
	 * @param inSpecs Input table spec.
	 * @throws InvalidSettingsException
	 */
	public void configure(DataTableSpec inSpecs) throws InvalidSettingsException {
		scoreColumns = new ColumnPatternExtractor(getScoreColumnPattern()).match(inSpecs);

		if (scoreColumns.isEmpty()) {
			throw new InvalidSettingsException("No p-values columns found in provided table");
		}

	}

	private static String getScoreColumnPattern() {
		return P_VALUE_COLUMN_REGEX;
	}
}
