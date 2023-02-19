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

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import se.redfield.cp.core.PredictiveSystemsClassifierCellFactory;
import se.redfield.cp.core.calibration.CalibratorRegression;
import se.redfield.cp.core.prediction.PredictiveSystemsRegressionPredictor;
import se.redfield.cp.utils.PortDef;

public class CompactPredictiveSystemsRegressionNodeModel extends NodeModel {

	/**
	 * Prediction table input port
	 */
	public static final PortDef PORT_PREDICTION_TABLE = new PortDef(1, "Prediction table");
	/**
	 * Calibration table input port
	 */
	public static final PortDef PORT_CALIBRATION_TABLE = new PortDef(0, "Calibration table");

	private final CompactPredictiveSystemsRegressionNodeSettings settings = new CompactPredictiveSystemsRegressionNodeSettings();

	private final CalibratorRegression calibrator = new CalibratorRegression(settings, true);
	private final PredictiveSystemsRegressionPredictor predictor = new PredictiveSystemsRegressionPredictor(settings);
	private ColumnRearranger classifierRearranger;

	protected CompactPredictiveSystemsRegressionNodeModel() {
		super(2, 1);
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		BufferedDataTable inCalibrationTable = inData[PORT_CALIBRATION_TABLE.getIdx()];
		BufferedDataTable inPredictionTable = inData[PORT_PREDICTION_TABLE.getIdx()];

		BufferedDataTable calibrationTable = calibrator.process(inCalibrationTable, exec);

		ColumnRearranger r = predictor.createRearranger(inPredictionTable.getDataTableSpec(), calibrationTable,
				exec.createSubExecutionContext(0.1));
		BufferedDataTable predictionTable = exec.createColumnRearrangeTable(inPredictionTable, r,
				exec.createSubProgress(0.45));

		return new BufferedDataTable[] {
				exec.createColumnRearrangeTable(predictionTable, classifierRearranger, exec.createSubProgress(0.45)) };
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		settings.validateSettings(inSpecs);

		DataTableSpec predictionTableSpec = predictor.createOuputTableSpec(inSpecs[PORT_PREDICTION_TABLE.getIdx()]);
		classifierRearranger = createClassifierRearranger(predictionTableSpec);

		return new DataTableSpec[] { classifierRearranger.createSpec() };
	}

	private ColumnRearranger createClassifierRearranger(DataTableSpec inSpec) {
		ColumnRearranger r = new ColumnRearranger(inSpec);
		r.append(new PredictiveSystemsClassifierCellFactory(settings.getDistributionColumnName(),
				settings.getClassifierSettings(), inSpec));
		return r;
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
