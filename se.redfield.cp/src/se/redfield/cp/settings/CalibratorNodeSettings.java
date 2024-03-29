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

import java.util.function.Consumer;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import se.redfield.cp.nodes.ConformalPredictorCalibratorNodeModel;

/**
 * The node settings for the {@link ConformalPredictorCalibratorNodeModel} node.
 * 
 * @author Alexander Bondaletov
 *
 */
public class CalibratorNodeSettings implements CalibratorSettings {

	private final TargetSettings targetSettings;
	private final KeepColumnsSettings keepColumns;

	/**
	 * Creates new instance
	 */
	public CalibratorNodeSettings() {
		targetSettings = new TargetSettings(ConformalPredictorCalibratorNodeModel.PORT_INPUT_TABLE,
				ConformalPredictorCalibratorNodeModel.PORT_INPUT_TABLE);
		keepColumns = new KeepColumnsSettings(ConformalPredictorCalibratorNodeModel.PORT_INPUT_TABLE);
	}

	@Override
	public TargetSettings getTargetSettings() {
		return targetSettings;
	}

	@Override
	public KeepColumnsSettings getKeepColumns() {
		return keepColumns;
	}

	/**
	 * Loads settings from the provided {@link NodeSettingsRO}
	 * 
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void loadSettingFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		targetSettings.loadSettingsFrom(settings);
		keepColumns.loadSettingFrom(settings);
	}

	/**
	 * Saves current settings into the given {@link NodeSettingsWO}.
	 * 
	 * @param settings
	 */
	public void saveSettingsTo(NodeSettingsWO settings) {
		targetSettings.saveSettingsTo(settings);
		keepColumns.saveSettingsTo(settings);
	}

	/**
	 * Validates internal consistency of the current settings
	 * 
	 * @throws InvalidSettingsException
	 */
	private void validate() throws InvalidSettingsException {
		targetSettings.validate();
		keepColumns.validate();
	}

	/**
	 * Validates settings stored in the provided {@link NodeSettingsRO}.
	 * 
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		CalibratorNodeSettings temp = new CalibratorNodeSettings();
		temp.loadSettingFrom(settings);
		temp.validate();
	}

	/**
	 * Validates the settings against input table spec.
	 * 
	 * @param inSpecs     Input specs
	 * @param msgConsumer Warning message consumer
	 * @throws InvalidSettingsException
	 */
	public void validateSettings(DataTableSpec[] inSpecs, Consumer<String> msgConsumer)
			throws InvalidSettingsException {
		targetSettings.validateSettings(inSpecs, msgConsumer);
		keepColumns.validateSettings(inSpecs);

		validate();
	}

}
