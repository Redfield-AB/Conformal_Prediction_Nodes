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
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
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

import se.redfield.cp.core.calibration.CalibratorRegression;
import se.redfield.cp.core.prediction.PredictorRegression;
import se.redfield.cp.settings.CompactRegressionNodeSettings;
import se.redfield.cp.utils.PortDef;


/**
 * All-in-one Conformal Predictor for regression. Calculates the calibration
 * table and then uses this table to calculates upper and lower bounds for the
 * regression value.
 *
 */
public class CompactConformalRegressionNodeModel extends NodeModel {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(CompactConformalRegressionNodeModel.class);

	/**
	 * Prediction table input port
	 */
	public static final PortDef PORT_PREDICTION_TABLE = new PortDef(1, "Prediction table");
	/**
	 * Calibration table input port
	 */
	public static final PortDef PORT_CALIBRATION_TABLE = new PortDef(0, "Calibration table");

	private final CompactRegressionNodeSettings settings = new CompactRegressionNodeSettings();

	private final CalibratorRegression calibrator = new CalibratorRegression(settings);
	private final PredictorRegression predictor = new PredictorRegression(settings);

	protected CompactConformalRegressionNodeModel() {
		super(2, 1);
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {

		pushFlowVariableDouble(CompactRegressionNodeSettings.KEY_ERROR_RATE, settings.getErrorRate());

		BufferedDataTable inCalibrationTable = inData[PORT_CALIBRATION_TABLE.getIdx()];
		BufferedDataTable inPredictionTable = inData[PORT_PREDICTION_TABLE.getIdx()];

		BufferedDataTable calibrationTable = calibrator.process(inCalibrationTable, exec);

		ColumnRearranger r = predictor.createRearranger(inPredictionTable.getDataTableSpec(), calibrationTable,
				exec.createSubExecutionContext(0.1));

		return new BufferedDataTable[] {
				exec.createColumnRearrangeTable(inPredictionTable, r, exec.createSubProgress(0.9)) };
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		settings.validateSettings(inSpecs);

		return new DataTableSpec[] {
				predictor.createOuputTableSpec(inSpecs[PORT_PREDICTION_TABLE.getIdx()]) };
	}

	@Override
	public InputPortRole[] getInputPortRoles() {
		return new InputPortRole[] { InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, InputPortRole.DISTRIBUTED_STREAMABLE };
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
				BufferedDataTable inCalibrationTable = (BufferedDataTable) ((PortObjectInput) inputs[PORT_CALIBRATION_TABLE
						.getIdx()])
						.getPortObject();
				ColumnRearranger rearranger = predictor.createRearranger(
						(DataTableSpec) inSpecs[PORT_PREDICTION_TABLE.getIdx()],
						inCalibrationTable, exec.createSubExecutionContext(0.1));
				rearranger.createStreamableFunction(PORT_PREDICTION_TABLE.getIdx(), 0).runFinal(inputs, outputs,
						exec.createSubExecutionContext(0.9));
			}
		};
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		this.settings.loadSettingFrom(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		this.settings.validateSettings(settings);
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		this.settings.saveSettingsTo(settings);
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
		// nothing to do
	}

}
