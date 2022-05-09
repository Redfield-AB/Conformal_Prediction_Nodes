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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

import se.redfield.cp.utils.KnimeUtils;

/**
 * Conformal Predictor Training Loop End Node. Works with corresponing Start
 * Loop Node. Collects calibration and training model data.<br />
 * 
 * Tables are concatenated, iteration column is added.<br />
 *
 */
public class ConformalPredictorTrainingLoopEndNodeModel extends NodeModel implements LoopEndNode {

	public static final int PORT_MODEL_TABLE = 0;
	public static final int PORT_CALIBRATION_TABLE = 1;

	public static final String DEFAULT_ITERATION_COLUMN_NAME = "Iteration";

	private int iteration;
	private BufferedDataContainer calibrationContainer;
	private BufferedDataContainer modelContainer;

	protected ConformalPredictorTrainingLoopEndNodeModel() {
		super(new PortType[] { BufferedDataTable.TYPE, BufferedDataTable.TYPE },
				new PortType[] { BufferedDataTable.TYPE, BufferedDataTable.TYPE });
	}

	private String getIterationColumnName() {
		return DEFAULT_ITERATION_COLUMN_NAME;
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		return new DataTableSpec[] { appendIterationColumn(inSpecs[PORT_MODEL_TABLE]),
				appendIterationColumn(inSpecs[PORT_CALIBRATION_TABLE]) };
	}

	/**
	 * Appends iteration column to provided {@link DataTableSpec}.
	 * 
	 * @param inSpec Input spec.
	 * @return Result spec.
	 */
	private DataTableSpec appendIterationColumn(DataTableSpec inSpec) {
		DataColumnSpec iterColumn = new DataColumnSpecCreator(getIterationColumnName(), IntCell.TYPE).createSpec();
		return KnimeUtils.createSpec(inSpec, iterColumn);
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		if (!(getLoopStartNode() instanceof LoopStartNodeTerminator)) {
			throw new IllegalStateException("Loop End is not connected to corresponding Loop Start node.");
		}

		BufferedDataTable inCalibrationTable = inData[PORT_CALIBRATION_TABLE];
		BufferedDataTable inModelTable = inData[PORT_MODEL_TABLE];

		if (iteration == 0) {
			initContainers(inCalibrationTable, inModelTable, exec);
		}

		appendTable(calibrationContainer, inCalibrationTable, exec.createSubExecutionContext(0.5));
		appendTable(modelContainer, inModelTable, exec.createSubExecutionContext(0.5));

		boolean terminateLoop = ((LoopStartNodeTerminator) getLoopStartNode()).terminateLoop();

		if (terminateLoop) {
			calibrationContainer.close();
			modelContainer.close();
			return new BufferedDataTable[] { modelContainer.getTable(), calibrationContainer.getTable() };
		} else {
			iteration++;
			continueLoop();
			return new BufferedDataTable[2];
		}
	}

	/**
	 * Initializes {@link BufferedDataContainer} objects used to collect data on
	 * each iteration.
	 * 
	 * @param inCalibrationTable Input calibration table.
	 * @param inModelTable       Input model table.
	 * @param exec               Execution context.
	 */
	private void initContainers(BufferedDataTable inCalibrationTable, BufferedDataTable inModelTable,
			ExecutionContext exec) {
		calibrationContainer = exec.createDataContainer(appendIterationColumn(inCalibrationTable.getDataTableSpec()));
		modelContainer = exec.createDataContainer(appendIterationColumn(inModelTable.getDataTableSpec()));
	}

	/**
	 * Appends all rows from provided table to provided container. Appends an
	 * iteration cell to each row.
	 * 
	 * @param cont  Container to add rows.
	 * @param table Table to get rows from.
	 * @param exec  Execution context.
	 * @throws CanceledExecutionException
	 */
	private void appendTable(BufferedDataContainer cont, BufferedDataTable table, ExecutionContext exec)
			throws CanceledExecutionException {
		long count = 0;
		long totalCount = table.size();
		for (DataRow row : table) {
			cont.addRowToTable(new AppendedColumnRow(KnimeUtils.createRowKey(row.getKey(), iteration), row,
					new IntCell(iteration)));

			exec.checkCanceled();
			exec.setProgress((double) count++ / totalCount);
		}
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
		iteration = 0;
		calibrationContainer = null;
		modelContainer = null;
	}
}
