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

import static se.redfield.cp.nodes.ConformalPredictorNodeModel.PORT_CALIBRATION_TABLE;
import static se.redfield.cp.nodes.ConformalPredictorNodeModel.PORT_PREDICTION_TABLE;

import java.util.function.Consumer;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

import se.redfield.cp.nodes.ConformalPredictorNodeModel;

/**
 * The node settings for the {@link ConformalPredictorNodeModel} node.
 * 
 * @author Alexander Bondaletov
 *
 */
public class PredictorNodeSettings implements PredictorSettings {

	private static final String KEY_INCLUDE_RANK_COLUMN = "includeRankColumn";

	private final TargetSettings targetSettings;
	private final KeepColumnsSettings keepColumns;
	private final SettingsModelBoolean includeRank;

	/**
	 * Creates new instance.
	 */
	public PredictorNodeSettings() {
		targetSettings = new TargetSettings(PORT_CALIBRATION_TABLE, PORT_PREDICTION_TABLE);
		keepColumns = new KeepColumnsSettings(PORT_PREDICTION_TABLE);
		includeRank = new SettingsModelBoolean(KEY_INCLUDE_RANK_COLUMN, false);
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
	 * @return The include rank model.
	 */
	public SettingsModelBoolean getIncludeRankModel() {
		return includeRank;
	}

	@Override
	public boolean getIncludeRankColumn() {
		return includeRank.getBooleanValue();
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
		includeRank.loadSettingsFrom(settings);
	}

	/**
	 * Saves current settings into the given {@link NodeSettingsWO}.
	 * 
	 * @param settings
	 */
	public void saveSettingsTo(NodeSettingsWO settings) {
		targetSettings.saveSettingsTo(settings);
		keepColumns.saveSettingsTo(settings);
		includeRank.saveSettingsTo(settings);
	}

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
		PredictorNodeSettings temp = new PredictorNodeSettings();
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
