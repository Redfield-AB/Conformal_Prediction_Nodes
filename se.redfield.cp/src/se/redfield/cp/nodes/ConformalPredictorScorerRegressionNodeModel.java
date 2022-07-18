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
package se.redfield.cp.nodes;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.core.scoring.ScorerRegression;
import se.redfield.cp.settings.PredictorRegressionSettings;
import se.redfield.cp.utils.KnimeUtils;
import se.redfield.cp.utils.PortDef;


/**
 * Conformal Scorer node for regerssion. Used to evaluate data generated by
 * Predictor(Regression) node.
 *
 */
public class ConformalPredictorScorerRegressionNodeModel extends NodeModel {

	private static final PortDef PORT_INPUT_TABLE = new PortDef(0, "Input table");

	private static final String KEY_TARGET_COLUMN = "targetColumn";
	private static final String KEY_ADDITIONAL_INFO = "additionalInfo";	
	private static final String KEY_UPPERBOUND_COLUMN = "upperBound";
	private static final String KEY_LOWERBOUND_COLUMN = "lowerBound";
	
	private final SettingsModelString targetColumnSettings = createTargetColumnSettings();
	private final SettingsModelString upperboundColumnSettings = createUpperBoundColumnSettings();
	private final SettingsModelString lowerboundColumnSettings = createLowerBoundColumnSettings();
	private final SettingsModelBoolean additionalInforSettings = createAdditionalInfoSettings();

	private final ScorerRegression scorer = new ScorerRegression(this);

	static SettingsModelString createTargetColumnSettings() {
		return new SettingsModelString(KEY_TARGET_COLUMN, "");
	}
	static SettingsModelString createUpperBoundColumnSettings() {
		return new SettingsModelString(KEY_UPPERBOUND_COLUMN,
				PredictorRegressionSettings.PREDICTION_UPPER_COLUMN_DEFAULT_NAME);
	}
	static SettingsModelString createLowerBoundColumnSettings() {
		return new SettingsModelString(KEY_LOWERBOUND_COLUMN,
				PredictorRegressionSettings.PREDICTION_LOWER_COLUMN_DEFAULT_NAME);
	}

	static SettingsModelBoolean createAdditionalInfoSettings() {
		return new SettingsModelBoolean(KEY_ADDITIONAL_INFO, true);
	}

	protected ConformalPredictorScorerRegressionNodeModel() {
		super(1, 1);
	}

	/**
	 * @return The target column
	 */
	public String getTargetColumn() {
		return targetColumnSettings.getStringValue();
	}

	/**
	 * @return The upper bound column
	 */
	public String getUpperBoundColumnName() {
		return upperboundColumnSettings.getStringValue();
	}

	/**
	 * @return The lower bound column
	 */
	public String getLowerBoundColumnName() {
		return lowerboundColumnSettings.getStringValue();
	}

	/**
	 * @return Whether additional info mode is enabled
	 */
	public boolean isAdditionalInfoMode() {
		return additionalInforSettings.getBooleanValue();
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		validataSettings(inSpecs);
		return new DataTableSpec[] { scorer.createOutputSpec() };
	}

	/**
	 * Validates settings against input table spec.
	 * 
	 * @param spec Input table spec.
	 * @throws InvalidSettingsException
	 */
	private void validataSettings(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		if (getTargetColumn().isEmpty()) {
			throw new InvalidSettingsException("Target column is not selected.");
		}
		if (getUpperBoundColumnName().isEmpty()) {
			throw new InvalidSettingsException("Upper bound column is not selected");
		}
		if (getLowerBoundColumnName().isEmpty()) {
			throw new InvalidSettingsException("Lower bound column is not selected");
		}

		KnimeUtils.validateDoubleColumn(PORT_INPUT_TABLE, inSpecs, getTargetColumn(), "Target");
		KnimeUtils.validateDoubleColumn(PORT_INPUT_TABLE, inSpecs, getLowerBoundColumnName(), "Lower bound");
		KnimeUtils.validateDoubleColumn(PORT_INPUT_TABLE, inSpecs, getUpperBoundColumnName(), "Upper bound");
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		return new BufferedDataTable[] { scorer.process(inData[0], exec) };
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		targetColumnSettings.saveSettingsTo(settings);
		upperboundColumnSettings.saveSettingsTo(settings);
		lowerboundColumnSettings.saveSettingsTo(settings);
		additionalInforSettings.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		targetColumnSettings.validateSettings(settings);
		upperboundColumnSettings.validateSettings(settings);
		lowerboundColumnSettings.validateSettings(settings);
		additionalInforSettings.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		targetColumnSettings.loadSettingsFrom(settings);
		upperboundColumnSettings.loadSettingsFrom(settings);
		lowerboundColumnSettings.loadSettingsFrom(settings);
		additionalInforSettings.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
		// nothing to reset
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no internals
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no internals
	}
}
