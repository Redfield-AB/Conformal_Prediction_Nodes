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

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableOperator;

import se.redfield.cp.CalibratorRegression;
import se.redfield.cp.PredictorRegression;

/**
 * Conformal Predictor Node. Uses calibration data to calculate Rank and P-value
 * for each row of the input prediction table.
 *
 */
public class CompactConformalRegressionNodeModel extends ConformalPredictorRegressionNodeModel {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(CompactConformalRegressionNodeModel.class);
	private final CalibratorRegression calibrator = new CalibratorRegression(this);
	private final PredictorRegression predictor = new PredictorRegression(this);

	protected CompactConformalRegressionNodeModel() {
		super();
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {

		pushFlowVariableDouble(super.KEY_ERROR_RATE, getErrorRate());

		BufferedDataTable inCalibrationTable = inData[PORT_CALIBRATION_TABLE];
		BufferedDataTable inPredictionTable = inData[PORT_PREDICTION_TABLE];

		BufferedDataTable calibrationTable = calibrator.process(inCalibrationTable, exec);

		ColumnRearranger r = predictor.createRearranger(inPredictionTable.getDataTableSpec(), calibrationTable,
				exec.createSubExecutionContext(0.1));

		return new BufferedDataTable[] {
				exec.createColumnRearrangeTable(inPredictionTable, r, exec.createSubProgress(0.9)) };
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		validateSettings(inSpecs[PORT_CALIBRATION_TABLE]);
		validateSettings(inSpecs[PORT_PREDICTION_TABLE]);
//		validateCalibrationTable(inSpecs[PORT_CALIBRATION_TABLE], inSpecs[PORT_PREDICTION_TABLE]);

		return new DataTableSpec[] {
				predictor.createOuputTableSpec(inSpecs[PORT_CALIBRATION_TABLE], inSpecs[PORT_PREDICTION_TABLE]) };
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
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		super.validateSettings(settings);
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		super.saveSettingsTo(settings);
	}
}
