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

import se.redfield.cp.Calibrator;
import se.redfield.cp.ClassifierCellFactory;
import se.redfield.cp.Predictor;
import se.redfield.cp.settings.ClassifierSettings;
import se.redfield.cp.settings.CompactClassificationNodeSettigns;
import se.redfield.cp.utils.PortDef;

/**
 * Conformal Classifier node. Assigns predicted classes to each row based on
 * it's P-values and selected Significance Level. Works with classes column
 * represented as Collection or String column
 *
 */
public class CompactConformalClassificationNodeModel extends NodeModel {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(CompactConformalClassificationNodeModel.class);

	public static final PortDef PORT_PREDICTION_TABLE = new PortDef(1, "Prediction table");
	public static final PortDef PORT_CALIBRATION_TABLE = new PortDef(0, "Calibration table");

	private CompactClassificationNodeSettigns settings = new CompactClassificationNodeSettigns();
	private Calibrator calibrator = new Calibrator(settings);
	private Predictor predictor = new Predictor(settings);
	private ColumnRearranger rearranger;

	protected CompactConformalClassificationNodeModel() {
		super(2, 1);
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		pushFlowVariableDouble(ClassifierSettings.KEY_ERROR_RATE, settings.getClassifierSettings().getErrorRate());

		BufferedDataTable inCalibrationTable = inData[PORT_CALIBRATION_TABLE.getIdx()];
		BufferedDataTable inPredictionTable = inData[PORT_PREDICTION_TABLE.getIdx()];
		// Calibrate
		BufferedDataTable calibrationTable = calibrator.process(inCalibrationTable, exec);

		// predict
		ColumnRearranger r = predictor.createRearranger(inPredictionTable.getDataTableSpec(), calibrationTable,
				exec.createSubExecutionContext(0.1));
		inPredictionTable = exec.createColumnRearrangeTable(inPredictionTable, r, exec.createSubProgress(0.9));

		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inPredictionTable, rearranger, exec) };
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		settings.validateSettings(inSpecs, this::setWarningMessage);
		DataTableSpec predictionTableSpec = predictor.createOuputTableSpec(inSpecs[PORT_CALIBRATION_TABLE.getIdx()],
				inSpecs[PORT_PREDICTION_TABLE.getIdx()]);

		rearranger = createRearranger(predictionTableSpec);

		return new DataTableSpec[] { rearranger.createSpec() };
	}

	/**
	 * Creates ColumnRearranger
	 * 
	 * @param inSpec Input table spec
	 * @return rearranger
	 * @throws InvalidSettingsException
	 */
	private ColumnRearranger createRearranger(DataTableSpec inSpec) throws InvalidSettingsException {
		settings.getClassifierSettings().configure(inSpec);
		ColumnRearranger r = new ColumnRearranger(inSpec);
		r.append(new ClassifierCellFactory(settings.getClassifierSettings()));
		return r;
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		this.settings.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		this.settings.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		this.settings.loadSettingFrom(settings);
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
		rearranger = null;
	}

}
