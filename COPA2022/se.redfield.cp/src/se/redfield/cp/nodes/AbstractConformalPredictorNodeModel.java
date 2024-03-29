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
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Base {@link NodeModel} implementation for Predictor and Calibrator nodes
 *
 */
public abstract class AbstractConformalPredictorNodeModel extends NodeModel {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractConformalPredictorNodeModel.class);

	private static final String KEY_COLUMN_NAME = "targetColumn";
	private static final String KEY_KEEP_ALL_COLUMNS = "keepAllColumns";
	private static final String KEY_KEEP_ID_COLUMN = "keepIdColumn";
	private static final String KEY_ID_COLUMN = "idColumn";

	private static final String CALIBRATION_P_COLUMN_DEFAULT_NAME = "P";
	private static final String CALIBRATION_RANK_COLUMN_DEFAULT_NAME = "Rank";

	private static final String PREDICTION_RANK_COLUMN_DEFAULT_FORMAT = "Rank (%s)";
	private static final String PREDICTION_SCORE_COLUMN_DEFAULT_FORMAT = "p-value (%s)";

	private static final String KEY_INCLUDE_RANK_COLUMN = "includeRankColumn";

	protected final SettingsModelString targetColumnSettings = createTargetColumnSettings();
	protected final SettingsModelBoolean keepAllColumnsSettings = createKeepAllColumnsSettingsModel();
	protected final SettingsModelBoolean keepIdColumnSettings = createKeepIdColumnSettings();
	protected final SettingsModelString idColumnSettings = createIdColumnSettings();
	private final SettingsModelBoolean includeRankSettings = createIncludeRankSettings();

	static SettingsModelBoolean createIncludeRankSettings() {
		return new SettingsModelBoolean(KEY_INCLUDE_RANK_COLUMN, false);
	}

	static SettingsModelString createTargetColumnSettings() {
		return new SettingsModelString(KEY_COLUMN_NAME, "");
	}

	static SettingsModelBoolean createKeepAllColumnsSettingsModel() {
		return new SettingsModelBoolean(KEY_KEEP_ALL_COLUMNS, true);
	}

	static SettingsModelBoolean createKeepIdColumnSettings() {
		return new SettingsModelBoolean(KEY_KEEP_ID_COLUMN, false);
	}

	static SettingsModelString createIdColumnSettings() {
		return new SettingsModelString(KEY_ID_COLUMN, "");
	}

	protected AbstractConformalPredictorNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}

	/**
	 * Returns probability column name for a given value for a selected target
	 * column.
	 * 
	 * @param val target's value
	 * @return probability column name
	 */
	public String getProbabilityColumnName(String val) {
		return getProbabilityColumnName(getTargetColumnName(), val);
	}

	/**
	 * Returns probability column name which format is
	 * 
	 * <pre>
	 * P ([column]=[value])
	 * </pre>
	 * 
	 * @param column target column name
	 * @param val    target column value
	 * @return probability column name
	 */
	public String getProbabilityColumnName(String column, String val) {
		return String.format(String.format("P (%s=%s)", column, val));
	}

	public String getTargetColumnName() {
		return targetColumnSettings.getStringValue();
	}

	public boolean getKeepAllColumns() {
		return keepAllColumnsSettings.getBooleanValue();
	}

	public boolean getKeepIdColumn() {
		return keepIdColumnSettings.getBooleanValue();
	}

	public String getIdColumn() {
		return idColumnSettings.getStringValue();
	}

	public String getCalibrationRankColumnName() {
		return CALIBRATION_RANK_COLUMN_DEFAULT_NAME;
	}

	public String getCalibrationProbabilityColumnName() {
		return CALIBRATION_P_COLUMN_DEFAULT_NAME;
	}

	public boolean getIncludeRankColumn() {
		return includeRankSettings.getBooleanValue();
	}

	public String getPredictionRankColumnFormat() {
		return PREDICTION_RANK_COLUMN_DEFAULT_FORMAT;
	}

	public String getPredictionScoreColumnFormat() {
		return PREDICTION_SCORE_COLUMN_DEFAULT_FORMAT;
	}

	/**
	 * Validates input table spec. Attempts to perform autoconfig if node is not
	 * configured
	 * 
	 * @param spec
	 * @throws InvalidSettingsException
	 */
	protected void validateSettings(DataTableSpec spec) throws InvalidSettingsException {
		if (getTargetColumnName().isEmpty()) {
			attemptAutoconfig(spec);
		}

		if (getTargetColumnName().isEmpty()) {
			throw new InvalidSettingsException("Class column is not selected");
		}

		if (!getKeepAllColumns() && getKeepIdColumn() && getIdColumn().isEmpty()) {
			throw new InvalidSettingsException("Id column is not selected");
		}

		validateTableSpecs(getTargetColumnName(), spec);
	}

	/**
	 * Attempts autoconfig by searching for a target column that would pass spec
	 * validation.
	 * 
	 * @param spec Input table spec
	 */
	protected void attemptAutoconfig(DataTableSpec spec) {
		String[] columnNames = spec.getColumnNames();
		for (String column : columnNames) {
			try {
				validateTableSpecs(column, spec);
				targetColumnSettings.setStringValue(column);
				setWarningMessage(String.format("Node autoconfigured with '%s' column", column));
			} catch (InvalidSettingsException e) {
				// ignore
			}
		}
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
	protected void validateTableSpecs(String selectedColumn, DataTableSpec spec) throws InvalidSettingsException {
		DataColumnSpec columnSpec = spec.getColumnSpec(selectedColumn);
		if (!columnSpec.getDomain().hasValues() || columnSpec.getDomain().getValues().isEmpty()) {
			throw new InvalidSettingsException("Insufficient domain information for column: " + selectedColumn);
		}

		Set<DataCell> values = columnSpec.getDomain().getValues();
		for (DataCell cell : values) {
			String value = cell.toString();
			String pColumnName = getProbabilityColumnName(selectedColumn, value);
			if (!spec.containsName(pColumnName)) {
				throw new InvalidSettingsException("Probability column not found: " + pColumnName);
			}
		}

//		DataColumnSpec columnSpec = spec.getColumnSpec(selectedColumn);
//		if(!(columnSpec.getType().getCellClass().equals((IntCell.class)) 
//				|| columnSpec.getType().getCellClass().equals((DoubleCell.class)) 
//				|| columnSpec.getType().getCellClass().equals((LongCell.class)))) 
//		{
//			throw new InvalidSettingsException("Target column " + selectedColumn + " must be numeric.");
//		}
//		
//		if (!spec.containsName(getPredictionColumnName(selectedColumn)))
//		{
//			throw new InvalidSettingsException("Prediction column '" + getPredictionColumnName(selectedColumn) + "' must exist.");
//		}
//		columnSpec = spec.getColumnSpec(getPredictionColumnName(selectedColumn));
//		if(!(columnSpec.getType().getCellClass().equals((IntCell.class)) 
//				|| columnSpec.getType().getCellClass().equals((DoubleCell.class)) 
//				|| columnSpec.getType().getCellClass().equals((LongCell.class)))) 
//		{
//			throw new InvalidSettingsException("Prediction column " + getPredictionColumnName(selectedColumn) + " must be numeric.");
//		}

		if (!getKeepAllColumns() && getKeepIdColumn() && !spec.containsName(getIdColumn())) {
			throw new InvalidSettingsException("Id column not found: " + getIdColumn());
		}
	}

	/**
	 * Returns this list of column from input table required to include in output
	 * table. List includes target column, all the probability columns and ID column
	 * (if option enabled).
	 * 
	 * @param spec Input table spec
	 * @return
	 */
	public String[] getRequiredColumnNames(DataTableSpec spec) {
		List<String> columns = spec.getColumnSpec(getTargetColumnName()).getDomain().getValues().stream()
				.map(c -> getProbabilityColumnName(c.toString())).collect(Collectors.toList());
		columns.add(getTargetColumnName());
		if (!getKeepAllColumns() && getKeepIdColumn()) {
			columns.add(getIdColumn());
		}
		return columns.toArray(new String[] {});
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		targetColumnSettings.saveSettingsTo(settings);
		keepAllColumnsSettings.saveSettingsTo(settings);
		keepIdColumnSettings.saveSettingsTo(settings);
		idColumnSettings.saveSettingsTo(settings);
		includeRankSettings.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		targetColumnSettings.validateSettings(settings);
		keepAllColumnsSettings.validateSettings(settings);
		keepIdColumnSettings.validateSettings(settings);
		idColumnSettings.validateSettings(settings);
		includeRankSettings.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		targetColumnSettings.loadSettingsFrom(settings);
		keepAllColumnsSettings.loadSettingsFrom(settings);
		keepIdColumnSettings.loadSettingsFrom(settings);
		idColumnSettings.loadSettingsFrom(settings);
		includeRankSettings.loadSettingsFrom(settings);
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

	@Override
	protected void reset() {
		// nothing to reset
	}
}
