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

import org.knime.base.node.preproc.sample.SamplingNodeSettings;
import org.knime.base.node.preproc.sample.SamplingNodeSettings.SamplingMethods;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

import se.redfield.cp.Partitioner;

/**
 * Conformal Predictor Training Loop Start Node. Separated input table into 2
 * tables:
 * <ul>
 * <li>Training Table</li>
 * <li>Calibration Table</li>
 * </ul>
 *
 * Separation is performed on each iteration.
 */
public class ConformalPredictorTrainingLoopStartNodeModel extends NodeModel implements LoopStartNodeTerminator {

	private static final String KEY_ITERATIONS = "iterations";
	public static final String KEY_PARTITION_SETTINGS = "partitionSettings";

	private static final String FW_ITERATION = "iteration";
	private static final String FW_ITERATIONS_NUM = "iterationsNum";

	private final SettingsModelIntegerBounded iterationsSettings = createIterationSettings();
	private final SamplingNodeSettings partitionSettings = new SamplingNodeSettings();

	private int iteration = 0;
	private final Partitioner partitioner = new Partitioner(partitionSettings, true);

	static SettingsModelIntegerBounded createIterationSettings() {
		return new SettingsModelIntegerBounded(KEY_ITERATIONS, 1, 1, 100);
	}

	protected ConformalPredictorTrainingLoopStartNodeModel() {
		super(1, 2);
	}

	private int getIterationsNum() {
		return iterationsSettings.getIntValue();
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec in = inSpecs[0];
		checkSettings(in);
		return new DataTableSpec[] { in, in };
	}

	/**
	 * Validates sampling settings against input table spec.
	 * 
	 * @param partitionSettings Sampling settings.
	 * @param inSpec            Input table spec.
	 * @throws InvalidSettingsException
	 */
	private void checkSettings(DataTableSpec inSpec) throws InvalidSettingsException {
		if (partitionSettings.countMethod() == null) {
			throw new InvalidSettingsException("No sampling method selected");
		}
		if (partitionSettings.samplingMethod() == SamplingMethods.Stratified
				&& !inSpec.containsName(partitionSettings.classColumn())) {
			throw new InvalidSettingsException(
					"Column '" + partitionSettings.classColumn() + "' for stratified sampling " + "does not exist");
		}
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		BufferedDataTable[] parts = partitioner.partition(inData[0], exec);

		pushFlowVariableInt(FW_ITERATION, iteration);
		pushFlowVariableInt(FW_ITERATIONS_NUM, getIterationsNum());

		iteration++;
		return new BufferedDataTable[] { parts[0], parts[1] };
	}

	@Override
	public boolean terminateLoop() {
		return iteration >= getIterationsNum();
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		iterationsSettings.saveSettingsTo(settings);
		partitionSettings.saveSettingsTo(settings.addNodeSettings(KEY_PARTITION_SETTINGS));
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		iterationsSettings.validateSettings(settings);
		validateSamplingSettings(settings.getNodeSettings(KEY_PARTITION_SETTINGS));
	}

	/**
	 * Validates sampling settings consistency.
	 * 
	 * @param settings Settings to validate.
	 * @throws InvalidSettingsException
	 */
	private void validateSamplingSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		SamplingNodeSettings tmp = new SamplingNodeSettings();
		tmp.loadSettingsFrom(settings, false);

		switch (tmp.countMethod()) {
		case Absolute:
			if (tmp.count() < 0) {
				throw new InvalidSettingsException("Invalid count: " + tmp.count());
			}
			break;
		case Relative:
			if (tmp.fraction() < 0 || tmp.fraction() > 1) {
				throw new InvalidSettingsException("Invalid fraction: " + tmp.fraction());
			}
			break;
		default:
			throw new InvalidSettingsException("Unknown counting method: " + tmp.countMethod());
		}

		if (tmp.samplingMethod() == SamplingMethods.Stratified && tmp.classColumn() == null) {
			throw new InvalidSettingsException("Class column is not selected");
		}
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		iterationsSettings.loadSettingsFrom(settings);
		partitionSettings.loadSettingsFrom(settings.getNodeSettings(KEY_PARTITION_SETTINGS), false);
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
		partitioner.reset();
	}

}
