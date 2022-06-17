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

import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableOperator;

import se.redfield.cp.PredictorRegression;

/**
 * Conformal Predictor Node. Uses calibration data to calculate Rank and P-value
 * for each row of the input prediction table.
 *
 */
public class ConformalPredictorRegressionNodeModel extends AbstractConformalPredictorRegressionNodeModel {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(ConformalPredictorRegressionNodeModel.class);

	public static final int PORT_PREDICTION_TABLE = 1;
	public static final int PORT_CALIBRATION_TABLE = 0;
	
	private static final String DEFAULT_SEPARATOR = ";";
	private static final String KEY_STRING_SEPARATOR = "stringSeparator";

	public static final String PREDICTION_LOWER_COLUMN_DEFAULT_FORMAT = "Lower bound (%s)";
	public static final String PREDICTION_UPPER_COLUMN_DEFAULT_FORMAT = "Upper bound (%s)";
	public static final String PREDICTION_LOWER_COLUMN_EPSILON_FORMAT = "Lower bound (%s) [%0.3g]";
	public static final String PREDICTION_UPPER_COLUMN_EPSILON_FORMAT = "Upper bound (%s) [%0.3g]";

	private static final String PREDICTION_RANK_COLUMN_DEFAULT_FORMAT = "Rank (%s)";
	private static final String PREDICTION_SCORE_COLUMN_DEFAULT_FORMAT = "p-value (%s)";
	
	private final PredictorRegression predictor = new PredictorRegression(this);

	protected ConformalPredictorRegressionNodeModel(boolean visibleTarget) {
		super(2, 1, visibleTarget);
	}
	
	static SettingsModelString createStringSeparatorSettings() {
		return new SettingsModelString(KEY_STRING_SEPARATOR, DEFAULT_SEPARATOR);
	}
	
	protected static final String KEY_ERROR_RATE = "errorRate";

	private static final double DEFAULT_ERROR_RATE = 0.05;

	public static final String DEFAULT_REGRESSION_COLUMN_NAME = "Regression";

	private final SettingsModelDoubleBounded errorRateSettings = createErrorRateSettings();

	static SettingsModelDoubleBounded createErrorRateSettings() {
		return new SettingsModelDoubleBounded(KEY_ERROR_RATE, DEFAULT_ERROR_RATE, 0, 1);
	}

	public double getErrorRate() {
		return errorRateSettings.getDoubleValue();
	}

	public String getPredictionRankColumnFormat() {
		return PREDICTION_RANK_COLUMN_DEFAULT_FORMAT;
	}

	public String getPredictionLowerBoundColumnFormat() {
		return PREDICTION_LOWER_COLUMN_DEFAULT_FORMAT;
	}
		
	public String getLowerBoundColumnName() {
		return getLowerBoundColumnName(getTargetColumnName());
	}
	
	public String getLowerBoundColumnName(String val) {
		return String.format(String.format(getPredictionLowerBoundColumnFormat(), val));
	}

	public String getPredictionUpperBoundColumnFormat() {
		return PREDICTION_UPPER_COLUMN_DEFAULT_FORMAT;
	}
	
	public String getUpperBoundColumnName() {
		return getUpperBoundColumnName(getTargetColumnName());
	}
	
	public String getUpperBoundColumnName(String val) {
		return String.format(String.format(getPredictionUpperBoundColumnFormat(), val));
	}

	public String getPredictionScoreColumnFormat() {
		return PREDICTION_SCORE_COLUMN_DEFAULT_FORMAT;
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		pushFlowVariableDouble(KEY_ERROR_RATE, getErrorRate());
		BufferedDataTable inCalibrationTable = inData[PORT_CALIBRATION_TABLE];
		BufferedDataTable inPredictionTable = inData[PORT_PREDICTION_TABLE];
		ColumnRearranger r = predictor.createRearranger(inPredictionTable.getDataTableSpec(), inCalibrationTable,
				exec.createSubExecutionContext(0.1));

		return new BufferedDataTable[] {
				exec.createColumnRearrangeTable(inPredictionTable, r, exec.createSubProgress(0.9)) };
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		validateSettings(inSpecs[PORT_PREDICTION_TABLE]);
		validateCalibrationTable(inSpecs[PORT_CALIBRATION_TABLE], inSpecs[PORT_PREDICTION_TABLE]);

		return new DataTableSpec[] { predictor.createOuputTableSpec(inSpecs[PORT_CALIBRATION_TABLE], inSpecs[PORT_PREDICTION_TABLE]) };
	}

	/**
	 * Validates calibration table spec.
	 * 
	 * @param calibrationTableSpec Calibration table spec.
	 * @param predictionTableSpec  Input Prediction table spec.
	 * @throws InvalidSettingsException
	 */
	private void validateCalibrationTable(DataTableSpec calibrationTableSpec, DataTableSpec predictionTableSpec)
			throws InvalidSettingsException {		
		if (!calibrationTableSpec.containsName(getCalibrationRankColumnName())) {
			throw new InvalidSettingsException(
					String.format("Rank column '%s' is missing from the calibration table", getTargetColumnName()));
		}

		if (!calibrationTableSpec.containsName(getCalibrationAlphaColumnName())) {
			throw new InvalidSettingsException(
					String.format("Alpha (Nonconformity) column '%s' is missing from the calibration table",
							getCalibrationAlphaColumnName()));
		}

//		DataColumnSpec columnSpec = calibrationTableSpec.getColumnSpec(getSelectedColumnName());
//		if (!columnSpec.getDomain().hasValues() || columnSpec.getDomain().getValues().isEmpty()) {
//			throw new InvalidSettingsException(
//					"Calibration table: insufficient domain information for column: " + getSelectedColumnName());
//		}
//
//		checkAllClassesPresent(calibrationTableSpec, predictionTableSpec);
	}
//
//	/**
//	 * Checks if the calibration table contains data for all classes present in
//	 * prediction table
//	 * 
//	 * @param calibrationTableSpec Calibration table spec.
//	 * @param predictionTableSpec  Input prediction table spec.
//	 * @throws InvalidSettingsException
//	 */
//	private void checkAllClassesPresent(DataTableSpec calibrationTableSpec, DataTableSpec predictionTableSpec)
//			throws InvalidSettingsException {
//		Set<String> calibrationClasses = calibrationTableSpec.getColumnSpec(getSelectedColumnName()).getDomain()
//				.getValues().stream().map(DataCell::toString).collect(Collectors.toSet());
//		Set<String> predictionValues = predictionTableSpec.getColumnSpec(getSelectedColumnName()).getDomain()
//				.getValues().stream().map(DataCell::toString).collect(Collectors.toSet());
//
//		for (String val : predictionValues) {
//			if (!calibrationClasses.contains(val)) {
//				throw new InvalidSettingsException(
//						String.format("Class '%s' is missing in the calibration table", val));
//			}
//		}
//	}

	@Override
	public InputPortRole[] getInputPortRoles() {
		return new InputPortRole[] { InputPortRole.DISTRIBUTED_STREAMABLE,
				InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, };
	}

	@Override
	public OutputPortRole[] getOutputPortRoles() {
		return new OutputPortRole[] { OutputPortRole.DISTRIBUTED };
	}

	@Override
	public StreamableOperator createStreamableOperator(PartitionInfo partitionInfo, PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		return new StreamableOperator() {

			@Override
			public void runFinal(PortInput[] inputs, PortOutput[] outputs, ExecutionContext exec) throws Exception {
				BufferedDataTable inCalibrationTable = (BufferedDataTable) ((PortObjectInput) inputs[PORT_CALIBRATION_TABLE])
						.getPortObject();
				ColumnRearranger rearranger = predictor.createRearranger((DataTableSpec) inSpecs[PORT_PREDICTION_TABLE],
						inCalibrationTable, exec);
				rearranger.createStreamableFunction(PORT_PREDICTION_TABLE, 0).runFinal(inputs, outputs, exec);
			}
		};
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		super.loadValidatedSettingsFrom(settings);
		errorRateSettings.loadSettingsFrom(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		super.validateSettings(settings);
		errorRateSettings.validateSettings(settings);
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		super.saveSettingsTo(settings);
		errorRateSettings.saveSettingsTo(settings);
	}
}
