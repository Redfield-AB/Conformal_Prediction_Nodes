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

import se.redfield.cp.nodes.ConformalPredictorLoopEndNodeModel;
import se.redfield.cp.utils.ColumnPatternExtractor;

public class ClassifierSettings {
	public static final String KEY_ERROR_RATE = "errorRate";
	private static final String KEY_CLASSES_AS_STRING = "classesAsString";
	private static final String KEY_STRING_SEPARATOR = "stringSeparator";

	private static final double DEFAULT_ERROR_RATE = 0.2;
	public static final String DEFAULT_SEPARATOR = ";";
	public static final String DEFAULT_CLASSES_COLUMN_NAME = "Classes";

	private final SettingsModelDoubleBounded errorRate;
	private final SettingsModelBoolean classesAsString;
	private final SettingsModelString stringSeparator;

	private Map<String, Integer> scoreColumns;

	public ClassifierSettings() {
		errorRate = new SettingsModelDoubleBounded(KEY_ERROR_RATE, DEFAULT_ERROR_RATE, 0, 1);
		classesAsString = new SettingsModelBoolean(KEY_CLASSES_AS_STRING, false);
		stringSeparator = new SettingsModelString(KEY_STRING_SEPARATOR, DEFAULT_SEPARATOR);

		classesAsString.addChangeListener(e -> stringSeparator.setEnabled(classesAsString.getBooleanValue()));
		stringSeparator.setEnabled(classesAsString.getBooleanValue());
	}

	public SettingsModelDoubleBounded getErrorRateModel() {
		return errorRate;
	}

	public double getErrorRate() {
		return errorRate.getDoubleValue();
	}

	public SettingsModelBoolean getClassesAsStringModel() {
		return classesAsString;
	}

	public boolean getClassesAsString() {
		return classesAsString.getBooleanValue();
	}

	public SettingsModelString getStringSeparatorModel() {
		return stringSeparator;
	}

	public String getStringSeparator() {
		return stringSeparator.getStringValue();
	}

	public Map<String, Integer> getScoreColumns() {
		if (scoreColumns == null) {
			throw new IllegalStateException("The settings object is not configured");
		}
		return scoreColumns;
	}

	public String getClassesColumnName() {
		return DEFAULT_CLASSES_COLUMN_NAME;
	}

	public void loadSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		errorRate.loadSettingsFrom(settings);
		classesAsString.loadSettingsFrom(settings);
		stringSeparator.loadSettingsFrom(settings);
	}

	public void saveSettingsTo(NodeSettingsWO settings) {
		errorRate.saveSettingsTo(settings);
		classesAsString.saveSettingsTo(settings);
		stringSeparator.saveSettingsTo(settings);
	}

	public void validate() throws InvalidSettingsException {
		if (getClassesAsString() && getStringSeparator().isEmpty()) {
			throw new InvalidSettingsException("String separator is empty");
		}
	}

	public void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		ClassifierSettings temp = new ClassifierSettings();
		temp.loadSettingsFrom(settings);
		temp.validate();
	}

	public void configure(DataTableSpec inSpecs) throws InvalidSettingsException {
		scoreColumns = new ColumnPatternExtractor(getScoreColumnPattern()).match(inSpecs);

		if (scoreColumns.isEmpty()) {
			throw new InvalidSettingsException("No p-values columns found in provided table");
		}

	}

	private String getScoreColumnPattern() {
		return ConformalPredictorLoopEndNodeModel.P_VALUE_COLUMN_REGEX;
	}
}
