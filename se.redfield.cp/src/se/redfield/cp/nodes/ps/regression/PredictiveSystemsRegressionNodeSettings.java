/*
 * Copyright (c) 2022 Redfield AB.
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
package se.redfield.cp.nodes.ps.regression;

import static se.redfield.cp.nodes.ps.regression.PredictiveSystemsRegressionNodeModel.PORT_CALIBRATION_TABLE;
import static se.redfield.cp.nodes.ps.regression.PredictiveSystemsRegressionNodeModel.PORT_PREDICTION_TABLE;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.settings.KeepColumnsSettings;
import se.redfield.cp.settings.PredictiveSystemsRegressionSettings;
import se.redfield.cp.settings.RegressionSettings;
import se.redfield.cp.utils.KnimeUtils;

/**
 * 
 * The node settings for the {@link PredictiveSystemsRegressionNodeModel} node.
 * 
 * @author Alexander Bondaletov
 *
 */
public class PredictiveSystemsRegressionNodeSettings implements PredictiveSystemsRegressionSettings {
	private static final String KEY_PREDICTION_COLUMN_NAME = "predictionColumn";

	private final SettingsModelString predictionColumn;
	private final RegressionSettings regressionSettings;
	private final KeepColumnsSettings keepColumns;

	/**
	 * Creates new instance.
	 */
	public PredictiveSystemsRegressionNodeSettings() {
		predictionColumn = new SettingsModelString(KEY_PREDICTION_COLUMN_NAME, "");
		regressionSettings = new RegressionSettings(PORT_PREDICTION_TABLE);
		keepColumns = new KeepColumnsSettings(PORT_PREDICTION_TABLE);
	}

	/**
	 * @return The prediction column model.
	 */
	public SettingsModelString getPredictionColumnModel() {
		return predictionColumn;
	}

	@Override
	public String getPredictionColumnName() {
		return predictionColumn.getStringValue();
	}

	@Override
	public RegressionSettings getRegressionSettings() {
		return regressionSettings;
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
		predictionColumn.loadSettingsFrom(settings);
		regressionSettings.loadSettingFrom(settings);
		keepColumns.loadSettingFrom(settings);
	}

	/**
	 * Saves current settings into the given {@link NodeSettingsWO}.
	 * 
	 * @param settings
	 */
	public void saveSettingsTo(NodeSettingsWO settings) {
		predictionColumn.saveSettingsTo(settings);
		regressionSettings.saveSettingsTo(settings);
		keepColumns.saveSettingsTo(settings);
	}

	private void validate() throws InvalidSettingsException {
		if (getPredictionColumnName().isEmpty()) {
			throw new InvalidSettingsException("Prediction column is not selected");
		}
		regressionSettings.validate();
		keepColumns.validate();
	}

	/**
	 * Validates settings stored in the provided {@link NodeSettingsRO}.
	 * 
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		PredictiveSystemsRegressionNodeSettings temp = new PredictiveSystemsRegressionNodeSettings();
		temp.loadSettingFrom(settings);
		temp.validate();
	}

	/**
	 * Validates the settings against input table spec.
	 * 
	 * @param inSpecs Input specs
	 * @throws InvalidSettingsException
	 */
	public void validateSettings(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		KnimeUtils.validateDoubleColumn(PORT_PREDICTION_TABLE, inSpecs, getPredictionColumnName(), "Prediction");
		KnimeUtils.validateDoubleColumn(PORT_CALIBRATION_TABLE, inSpecs, getCalibrationAlphaColumnName(),
				"Alpha (Nonconformity)");

		regressionSettings.validateSettings(inSpecs);
		keepColumns.validateSettings(inSpecs);

		validate();
	}
}
