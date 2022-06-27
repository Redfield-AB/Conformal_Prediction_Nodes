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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.nodes.CompactConformalClassificationNodeModel;
import se.redfield.cp.nodes.ConformalPredictorLoopEndNodeModel;
import se.redfield.cp.nodes.ConformalPredictorNodeModel;

public class CompactClassificationNodeSettigns implements CalibratorSettings, PredictorSettings {
	private static final String KEY_KEEP_ALL_COLUMNS = "keepAllColumns";
	private static final String KEY_KEEP_ID_COLUMN = "keepIdColumn";
	private static final String KEY_ID_COLUMN = "idColumn";
	private static final String KEY_INCLUDE_RANK_COLUMN = "includeRankColumn";
	private static final String KEY_ERROR_RATE = "errorRate";
	private static final String KEY_CLASSES_AS_STRING = "classesAsString";
	private static final String KEY_STRING_SEPARATOR = "stringSeparator";

	private static final double DEFAULT_ERROR_RATE = 0.2;
	private static final String DEFAULT_SEPARATOR = ";";
	public static final String DEFAULT_CLASSES_COLUMN_NAME = "Classes";

	private final TargetSettings targetSettings;
	private final SettingsModelBoolean keepAllColumns;
	private final SettingsModelBoolean keepIdColumn;
	private final SettingsModelString idColumn;
	private final SettingsModelBoolean includeRank;
	private final SettingsModelDoubleBounded errorRate;
	private final SettingsModelBoolean classesAsString;
	private final SettingsModelString stringSeparator;

	public CompactClassificationNodeSettigns() {
		targetSettings = new TargetSettings(CompactConformalClassificationNodeModel.PORT_CALIBRATION_TABLE,
				CompactConformalClassificationNodeModel.PORT_CALIBRATION_TABLE,
				CompactConformalClassificationNodeModel.PORT_PREDICTION_TABLE);
		keepAllColumns = new SettingsModelBoolean(KEY_KEEP_ALL_COLUMNS, true);
		keepIdColumn = new SettingsModelBoolean(KEY_KEEP_ID_COLUMN, false);
		idColumn = new SettingsModelString(KEY_ID_COLUMN, "");
		includeRank = new SettingsModelBoolean(KEY_INCLUDE_RANK_COLUMN, false);
		errorRate = new SettingsModelDoubleBounded(KEY_ERROR_RATE, DEFAULT_ERROR_RATE, 0, 1);
		classesAsString = new SettingsModelBoolean(KEY_CLASSES_AS_STRING, false);
		stringSeparator = new SettingsModelString(KEY_STRING_SEPARATOR, DEFAULT_SEPARATOR);

		keepAllColumns.addChangeListener(e -> {
			keepIdColumn.setEnabled(!keepAllColumns.getBooleanValue());
			if (!keepIdColumn.isEnabled()) {
				keepIdColumn.setBooleanValue(false);
			}
		});
		keepIdColumn.addChangeListener(e -> idColumn.setEnabled(keepIdColumn.getBooleanValue()));
		classesAsString.addChangeListener(e -> stringSeparator.setEnabled(classesAsString.getBooleanValue()));

		keepIdColumn.setEnabled(false);
		idColumn.setEnabled(false);
		stringSeparator.setEnabled(classesAsString.getBooleanValue());
	}

	public TargetSettings getTargetSettings() {
		return targetSettings;
	}

	public SettingsModelString getTargetColumnModel() {
		return targetSettings.getTargetColumnModel();
	}

	@Override
	public String getTargetColumnName() {
		return targetSettings.getTargetColumn();
	}

	public SettingsModelBoolean getKeepAllColumnsModel() {
		return keepAllColumns;
	}

	@Override
	public boolean getKeepAllColumns() {
		return keepAllColumns.getBooleanValue();
	}

	public SettingsModelBoolean getKeepIdColumnModel() {
		return keepIdColumn;
	}

	public boolean getKeepIdColumn() {
		return keepIdColumn.getBooleanValue();
	}

	public SettingsModelString getIdColumnModel() {
		return idColumn;
	}

	public String getIdColumn() {
		return idColumn.getStringValue();
	}

	public SettingsModelBoolean getIncludeRankModel() {
		return includeRank;
	}

	@Override
	public boolean getIncludeRankColumn() {
		return includeRank.getBooleanValue();
	}

	@Override
	public String getProbabilityColumnName(String value) {
		return targetSettings.getProbabilityColumnName(value);
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

	public String getClassesColumnName() {
		return DEFAULT_CLASSES_COLUMN_NAME;
	}

	public String getScoreColumnPattern() {
		return ConformalPredictorLoopEndNodeModel.P_VALUE_COLUMN_REGEX;
	}

	public void loadSettingFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		targetSettings.loadSettingsFrom(settings);
		keepAllColumns.loadSettingsFrom(settings);
		keepIdColumn.loadSettingsFrom(settings);
		idColumn.loadSettingsFrom(settings);
		includeRank.loadSettingsFrom(settings);
		errorRate.loadSettingsFrom(settings);
		classesAsString.loadSettingsFrom(settings);
		stringSeparator.loadSettingsFrom(settings);
	}

	public void saveSettingsTo(NodeSettingsWO settings) {
		targetSettings.saveSettingsTo(settings);
		keepAllColumns.saveSettingsTo(settings);
		keepIdColumn.saveSettingsTo(settings);
		idColumn.saveSettingsTo(settings);
		includeRank.saveSettingsTo(settings);
		errorRate.saveSettingsTo(settings);
		classesAsString.saveSettingsTo(settings);
		stringSeparator.saveSettingsTo(settings);
	}

	private void validate() throws InvalidSettingsException {
		targetSettings.validate();

		if (!getKeepAllColumns() && getKeepIdColumn() && getIdColumn().isEmpty()) {
			throw new InvalidSettingsException("Id column is not selected");
		}

		if (getClassesAsString() && getStringSeparator().isEmpty()) {
			throw new InvalidSettingsException("String separator is empty");
		}
	}

	public void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		CompactClassificationNodeSettigns temp = new CompactClassificationNodeSettigns();
		temp.loadSettingFrom(settings);
		temp.validate();
	}

	public void validateSettings(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		validate();

		targetSettings.validateSettings(inSpecs);

		if (!getKeepAllColumns() && getKeepIdColumn()
				&& !inSpecs[ConformalPredictorNodeModel.PORT_PREDICTION_TABLE.getIdx()].containsName(getIdColumn())) {
			throw new InvalidSettingsException("Id column not found: " + getIdColumn());
		}
	}

	@Override
	public String getCalibrationProbabilityColumnName() {
		return CalibratorSettings.super.getCalibrationProbabilityColumnName();
	}
}
