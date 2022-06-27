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
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.nodes.ConformalPredictorCalibratorNodeModel;

public class CalibratorNodeSettings implements CalibratorSettings {

	private static final String KEY_KEEP_ALL_COLUMNS = "keepAllColumns";
	private static final String KEY_KEEP_ID_COLUMN = "keepIdColumn";
	private static final String KEY_ID_COLUMN = "idColumn";

	private final TargetSettings targetSettings;
	protected final SettingsModelBoolean keepAllColumns;
	protected final SettingsModelBoolean keepIdColumn;
	protected final SettingsModelString idColumn;

	public CalibratorNodeSettings() {
		targetSettings = new TargetSettings(ConformalPredictorCalibratorNodeModel.PORT_INPUT_TABLE,
				ConformalPredictorCalibratorNodeModel.PORT_INPUT_TABLE);
		keepAllColumns = new SettingsModelBoolean(KEY_KEEP_ALL_COLUMNS, true);
		keepIdColumn = new SettingsModelBoolean(KEY_KEEP_ID_COLUMN, false);
		idColumn = new SettingsModelString(KEY_ID_COLUMN, "");

		keepAllColumns.addChangeListener(e -> {
			keepIdColumn.setEnabled(!keepAllColumns.getBooleanValue());
			if (!keepIdColumn.isEnabled()) {
				keepIdColumn.setBooleanValue(false);
			}
		});
		keepIdColumn.addChangeListener(e -> idColumn.setEnabled(keepIdColumn.getBooleanValue()));

		keepIdColumn.setEnabled(false);
		idColumn.setEnabled(false);
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

	@Override
	public String getProbabilityColumnName(String value) {
		return targetSettings.getProbabilityColumnName(value);
	}

	public void loadSettingFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		targetSettings.loadSettingsFrom(settings);
		keepAllColumns.loadSettingsFrom(settings);
		keepIdColumn.loadSettingsFrom(settings);
		idColumn.loadSettingsFrom(settings);
	}

	public void saveSettingsTo(NodeSettingsWO settings) {
		targetSettings.saveSettingsTo(settings);
		keepAllColumns.saveSettingsTo(settings);
		keepIdColumn.saveSettingsTo(settings);
		idColumn.saveSettingsTo(settings);
	}

	private void validate() throws InvalidSettingsException {
		targetSettings.validate();

		if (!getKeepAllColumns() && getKeepIdColumn() && getIdColumn().isEmpty()) {
			throw new InvalidSettingsException("Id column is not selected");
		}
	}

	public void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		CalibratorNodeSettings temp = new CalibratorNodeSettings();
		temp.loadSettingFrom(settings);
		temp.validate();
	}

	public void validateSettings(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		validate();

		targetSettings.validateSettings(inSpecs);

		if (!getKeepAllColumns() && getKeepIdColumn() && !inSpecs[0].containsName(getIdColumn())) {
			throw new InvalidSettingsException("Id column not found: " + getIdColumn());
		}
	}

}
