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
import java.util.NoSuchElementException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

import se.redfield.cp.IterationsTableSeparator;

/**
 * Conformal Predictor Loop Start Node. Takes calibration/model tables collected
 * by training loop and splits it by iterations.
 *
 */
public class ConformalPredictorLoopStartNodeModel extends NodeModel implements LoopStartNodeTerminator {

	public static final int PORT_MODEL_TABLE = 0;
	public static final int PORT_CALIBRATION_TABLE = 1;

	private IterationsTableSeparator modelSeparator;
	private IterationsTableSeparator calibrationSeparator;
	private boolean terminateLoop;

	protected ConformalPredictorLoopStartNodeModel() {
		super(2, 2);
	}

	private String getIterationColumn() {
		return ConformalPredictorTrainingLoopEndNodeModel.DEFAULT_ITERATION_COLUMN_NAME;
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		validateTableSpec(inSpecs[PORT_MODEL_TABLE], "Model");
		validateTableSpec(inSpecs[PORT_CALIBRATION_TABLE], "Calibration");

		return new DataTableSpec[] { inSpecs[PORT_MODEL_TABLE], inSpecs[PORT_CALIBRATION_TABLE] };
	}

	private void validateTableSpec(DataTableSpec spec, String title) throws InvalidSettingsException {
		if (!spec.containsName(getIterationColumn())) {
			throw new InvalidSettingsException(
					title + " table is missing '" + getIterationColumn() + "' iteration column.");
		}
		DataType type = spec.getColumnSpec(getIterationColumn()).getType();
		if (!type.isCompatible(IntValue.class)) {
			throw new InvalidSettingsException(
					title + " table has unsupported iteration column type: " + type.getName());
		}
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		if (modelSeparator == null) {
			checkEmptyInput(inData[PORT_MODEL_TABLE], inData[PORT_CALIBRATION_TABLE]);
			modelSeparator = new IterationsTableSeparator(inData[PORT_MODEL_TABLE], getIterationColumn());
			calibrationSeparator = new IterationsTableSeparator(inData[PORT_CALIBRATION_TABLE], getIterationColumn());
		}

		validateNextPartitions();

		BufferedDataTable outModelTable = modelSeparator.next(exec);
		BufferedDataTable outCalibrationTable = calibrationSeparator.next(exec);

		terminateLoop = !modelSeparator.hasNext() && !calibrationSeparator.hasNext();

		return new BufferedDataTable[] { outModelTable, outCalibrationTable };
	}

	/**
	 * Ensures that both of input tables are not empty.
	 * 
	 * @param inModelTable       Input model table.
	 * @param inCalibrationTable Input calibration table
	 * 
	 * @throws IllegalArgumentException In case one of the tables is empty table.
	 */
	private void checkEmptyInput(BufferedDataTable inModelTable, BufferedDataTable inCalibrationTable) {
		if (inModelTable.size() == 0) {
			throw new IllegalArgumentException("Model table is empty");
		}
		if (inCalibrationTable.size() == 0) {
			throw new IllegalArgumentException("Calibration table is empty");
		}
	}

	/**
	 * Checks that both of separators has next segment and these segments correspond
	 * to the same iteration
	 * 
	 * @throws NoSuchElementException   If one of the separators doesn't have a next
	 *                                  segment
	 * @throws IllegalArgumentException If the next segments from two separators
	 *                                  correspond to different iterations
	 */
	private void validateNextPartitions() {
		Integer nextModelIter = modelSeparator.hasNext() ? modelSeparator.getNextIteration() : null;
		Integer nextCalibrationIter = calibrationSeparator.hasNext() ? calibrationSeparator.getNextIteration() : null;

		if (nextModelIter == null && nextCalibrationIter == null) {
			// Should not happen. Loop should be terminated at this point
			throw new IllegalStateException("No more data to iterate");
		} else if (nextModelIter == null) {
			throw new NoSuchElementException("Model table is missing data for iteration: " + nextCalibrationIter);
		} else if (nextCalibrationIter == null) {
			throw new NoSuchElementException("Calibration table is missing data for iteration: " + nextModelIter);
		} else {
			if (nextModelIter.intValue() != nextCalibrationIter.intValue()) {
				throw new IllegalArgumentException("Tables are out of sync. Next model table iteration is: "
						+ nextModelIter + " Next calibration table iteration is: " + nextCalibrationIter
						+ ". Possible reasons: one of the tables is missing data for required iteration or "
						+ "isn't propertly sorter (both tables should be sorted by the iteration column).");
			}
		}
	}

	@Override
	public boolean terminateLoop() {
		return terminateLoop;
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		// no settings
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		// no settings
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		// no settings
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
		terminateLoop = false;

		if (modelSeparator != null) {
			modelSeparator.close();
			modelSeparator = null;
		}
		if (calibrationSeparator != null) {
			calibrationSeparator.close();
			calibrationSeparator = null;
		}
	}

}
