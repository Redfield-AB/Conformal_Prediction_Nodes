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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Base {@link NodeModel} implementation for Predictor and Calibrator nodes
 *
 */
public abstract class AbstractConformalPredictorRegressionNodeModel extends AbstractConformalPredictorNodeModel {
	@SuppressWarnings("unused")
	private static final String KEY_SIGMA_COLUMN_NAME = "sigma";
	private static final String CALIBRATION_ALHPA_COLUMN_DEFAULT_NAME = "Alpha";
	private static final String NORMALIZED_COLUMN_NAME = "normalized";
	protected final SettingsModelBoolean normalizedSettings = createNormalizedSettingsModel();
	protected final SettingsModelString sigmaSettings = createSigmaColumnNameSettingsModel();
	
	private final SettingsModelDoubleBounded betaSettings = createBetaSettings();
	
	protected static final String KEY_BETA = "beta";

	private static final double DEFAULT_BETA = 0.25;

	static SettingsModelDoubleBounded createBetaSettings() {
		return new SettingsModelDoubleBounded(KEY_BETA, DEFAULT_BETA, 0, 1);
	}
	
	public double getBeta() {
		return betaSettings.getDoubleValue();
	}
	
	protected AbstractConformalPredictorRegressionNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}

	static SettingsModelBoolean createNormalizedSettingsModel() {
		return new SettingsModelBoolean(NORMALIZED_COLUMN_NAME, false);
	}

	public boolean getNormalized() {
		return normalizedSettings.getBooleanValue();
	}


	static SettingsModelString createSigmaColumnNameSettingsModel() {
		return new SettingsModelString(KEY_SIGMA_COLUMN_NAME, "");
	}

	public String getSigmaColumnName() {
		return sigmaSettings.getStringValue();
	}

	/**
	 * Returns prediction column name for a given value for a selected target
	 * column.
	 * 
	 * @param val target's value
	 * @return prediction column name
	 */
	public String getPredictionColumnName() {
		return getPredictionColumnName(getTargetColumnName());
	}

	/**
	 * Returns prediction column name which format is
	 * 
	 * <pre>
	 * P ([column]=[value])
	 * </pre>
	 * 
	 * @param column target column name
	 * @param val    target column value
	 * @return prediction column name
	 */
	public String getPredictionColumnName(String val) {
		return String.format(String.format("Prediction (%s)", val));
	}

	public String getCalibrationAlphaColumnName() {
		return CALIBRATION_ALHPA_COLUMN_DEFAULT_NAME;
	}

	/**
	 * Validates input table spec against specified target column. Makes sure table
	 * contains probability columns for every target column's value.
	 * 
	 * @param selectedColumn Target column.
	 * @param spec           Input table spec.
	 * @throws InvalidSettingsException If target column domain doesn't have
	 *                                  possible values filled. If one of
	 *                                  probability columns is missing from input
	 *                                  table. If keepIDColumn is selected and ID
	 *                                  column is missing from input table.
	 */
	@Override
	protected void validateTableSpecs(String selectedColumn, DataTableSpec spec) throws InvalidSettingsException {
		
		DataColumnSpec columnSpec = spec.getColumnSpec(selectedColumn);
		if(!(columnSpec.getType().getCellClass().equals((IntCell.class)) 
				|| columnSpec.getType().getCellClass().equals((DoubleCell.class)) 
				|| columnSpec.getType().getCellClass().equals((LongCell.class)))) 
		{
			throw new InvalidSettingsException("Target column " + selectedColumn + " must be numeric.");
		}
		
		if (!spec.containsName(getPredictionColumnName(selectedColumn)))
		{
			throw new InvalidSettingsException("Prediction column '" + getPredictionColumnName(selectedColumn) + "' must exist.");
		}
		columnSpec = spec.getColumnSpec(getPredictionColumnName(selectedColumn));
		if(!(columnSpec.getType().getCellClass().equals((IntCell.class)) 
				|| columnSpec.getType().getCellClass().equals((DoubleCell.class)) 
				|| columnSpec.getType().getCellClass().equals((LongCell.class)))) 
		{
			throw new InvalidSettingsException("Prediction column " + getPredictionColumnName(selectedColumn) + " must be numeric.");
		}

		if (!getKeepAllColumns() && getKeepIdColumn() && !spec.containsName(getIdColumn())) {
			throw new InvalidSettingsException("Id column not found: " + getIdColumn());
		}
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		super.saveSettingsTo(settings);
		normalizedSettings.saveSettingsTo(settings);
		sigmaSettings.saveSettingsTo(settings);
		betaSettings.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		super.validateSettings(settings);
		normalizedSettings.validateSettings(settings);
		sigmaSettings.validateSettings(settings);
		betaSettings.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		super.loadValidatedSettingsFrom(settings);
		normalizedSettings.loadSettingsFrom(settings);
		sigmaSettings.loadSettingsFrom(settings);
		betaSettings.loadSettingsFrom(settings);
	}

}
