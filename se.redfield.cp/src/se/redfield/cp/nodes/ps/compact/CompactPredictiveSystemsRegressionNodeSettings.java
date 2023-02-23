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
package se.redfield.cp.nodes.ps.compact;

import static se.redfield.cp.nodes.ps.compact.CompactPredictiveSystemsRegressionNodeModel.PORT_CALIBRATION_TABLE;
import static se.redfield.cp.nodes.ps.compact.CompactPredictiveSystemsRegressionNodeModel.PORT_PREDICTION_TABLE;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.settings.CalibratorRegressionSettings;
import se.redfield.cp.settings.KeepColumnsSettings;
import se.redfield.cp.settings.PredictiveSystemsClassifierSettings;
import se.redfield.cp.settings.PredictiveSystemsRegressionSettings;
import se.redfield.cp.settings.RegressionSettings;
import se.redfield.cp.utils.KnimeUtils;

/**
 * The node settings for the {@link CompactPredictiveSystemsRegressionNodeModel}
 * node.
 * 
 * @author Alexander Bondaletov, Redfield SE
 *
 */
public class CompactPredictiveSystemsRegressionNodeSettings
		implements CalibratorRegressionSettings, PredictiveSystemsRegressionSettings {
	private static final String KEY_TARGET_COLUMN_NAME = "targetColumn";
	private static final String KEY_PREDICTION_COLUMN_NAME = "predictionColumn";
	/**
	 * The key to store classifier settings.
	 */
	public static final String KEY_CLASSIFIER = "classifier";

	private final SettingsModelString targetColumn;
	private final SettingsModelString predictionColumn;
	private final RegressionSettings regressionSettings;
	private final KeepColumnsSettings keepColumns;
	private final PredictiveSystemsClassifierSettings classifierSettings;

	/**
	 * Creates new instance
	 */
	public CompactPredictiveSystemsRegressionNodeSettings() {
		targetColumn = new SettingsModelString(KEY_TARGET_COLUMN_NAME, "");
		predictionColumn = new SettingsModelString(KEY_PREDICTION_COLUMN_NAME, "");
		regressionSettings = new RegressionSettings(PORT_CALIBRATION_TABLE, PORT_PREDICTION_TABLE);
		keepColumns = new KeepColumnsSettings(PORT_PREDICTION_TABLE);
		classifierSettings = new PredictiveSystemsClassifierSettings(PORT_PREDICTION_TABLE);
	}

	/**
	 * @return The target column model.
	 */
	public SettingsModelString getTargetColumnModel() {
		return targetColumn;
	}

	@Override
	public String getTargetColumnName() {
		return targetColumn.getStringValue();
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
	 * @return the classifier settings.
	 */
	public PredictiveSystemsClassifierSettings getClassifierSettings() {
		return classifierSettings;
	}

	@Override
	public String getCalibrationAlphaColumnName() {
		return CalibratorRegressionSettings.super.getCalibrationAlphaColumnName();
	}

	@Override
	public String getCalibrationRankColumnName() {
		return CalibratorRegressionSettings.super.getCalibrationRankColumnName();
	}

	/**
	 * Loads settings from the provided {@link NodeSettingsRO}
	 * 
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void loadSettingFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		targetColumn.loadSettingsFrom(settings);
		predictionColumn.loadSettingsFrom(settings);
		regressionSettings.loadSettingFrom(settings);
		keepColumns.loadSettingFrom(settings);
		classifierSettings.loadSettingsFrom(settings.getNodeSettings(KEY_CLASSIFIER));
	}

	/**
	 * Saves current settings into the given {@link NodeSettingsWO}.
	 * 
	 * @param settings
	 */
	public void saveSettingsTo(NodeSettingsWO settings) {
		targetColumn.saveSettingsTo(settings);
		predictionColumn.saveSettingsTo(settings);
		regressionSettings.saveSettingsTo(settings);
		keepColumns.saveSettingsTo(settings);
		classifierSettings.saveSettingsTo(settings.addNodeSettings(KEY_CLASSIFIER));
	}

	private void validate() throws InvalidSettingsException {
		if (getTargetColumnName().isEmpty()) {
			throw new InvalidSettingsException("Target column is not selected");
		}
		if (getPredictionColumnName().isEmpty()) {
			throw new InvalidSettingsException("Prediction column is not selected");
		}
		regressionSettings.validate();
		keepColumns.validate();
		classifierSettings.validate();
	}

	/**
	 * Validates settings stored in the provided {@link NodeSettingsRO}.
	 * 
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		CompactPredictiveSystemsRegressionNodeSettings temp = new CompactPredictiveSystemsRegressionNodeSettings();
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
	public void validateSettings(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		KnimeUtils.validateDoubleColumn(PORT_CALIBRATION_TABLE, inSpecs, getTargetColumnName(), "Target");
		KnimeUtils.validateDoubleColumn(PORT_CALIBRATION_TABLE, inSpecs, getPredictionColumnName(), "Prediction");
		KnimeUtils.validateDoubleColumn(PORT_PREDICTION_TABLE, inSpecs, getPredictionColumnName(), "Prediction");

		regressionSettings.validateSettings(inSpecs);
		keepColumns.validateSettings(inSpecs);

		validate();
	}
}
