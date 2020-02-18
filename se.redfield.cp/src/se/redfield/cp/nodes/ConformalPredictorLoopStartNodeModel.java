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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

import se.redfield.cp.Partitioner;

/**
 * Conformal Predictor Loop Start Node. Separated input table into 3 tables:
 * <ul>
 * <li>Training Table</li>
 * <li>Calibration Table</li>
 * <li>Test Table</li>
 * </ul>
 *
 * Test table is the same across the whole cycle. Training/Calibration Tables
 * are different on each iteration.
 */
public class ConformalPredictorLoopStartNodeModel extends NodeModel implements LoopStartNodeTerminator {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(ConformalPredictorLoopStartNodeModel.class);

	private static final String KEY_ITERATIONS = "iterations";
	public static final String KEY_TEST_PARTITION = "testPartition";
	public static final String KEY_CALIBRATION_PARTITION = "calibrationPartition";

	private static final String FW_ITERATION = "iteration";
	private static final String FW_ITERATIONS_NUM = "iterationsNum";

	private final SettingsModelIntegerBounded iterationsSettings = createIterationSettings();
	private final SamplingNodeSettings testPartitionSettings = new SamplingNodeSettings();
	private final SamplingNodeSettings calibrationPartitionSettings = new SamplingNodeSettings();

	private int iteration = 0;
	private final Partitioner testSetPartitioner = new Partitioner(testPartitionSettings, false);
	private final Partitioner calibrationSetPartitioner = new Partitioner(calibrationPartitionSettings, true);

	static SettingsModelIntegerBounded createIterationSettings() {
		return new SettingsModelIntegerBounded(KEY_ITERATIONS, 1, 1, 100);
	}

	protected ConformalPredictorLoopStartNodeModel() {
		super(1, 3);
	}

	private int getIterationsNum() {
		return iterationsSettings.getIntValue();
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec in = inSpecs[0];
		checkSettings(testPartitionSettings, in, "Training/Test split");
		checkSettings(calibrationPartitionSettings, in, "Training/Calibration split");
		return new DataTableSpec[] { in, in, in };
	}

	/**
	 * Validates sampling settings against input table spec.
	 * 
	 * @param partitionSettings Sampling settings.
	 * @param inSpec            Input table spec.
	 * @param title             Title to identify exact sampling settings in error
	 *                          messages.
	 * @throws InvalidSettingsException
	 */
	private void checkSettings(SamplingNodeSettings partitionSettings, DataTableSpec inSpec, String title)
			throws InvalidSettingsException {
		if (partitionSettings.countMethod() == null) {
			throw new InvalidSettingsException(title + ": No sampling method selected");
		}
		if (partitionSettings.samplingMethod() == SamplingMethods.Stratified
				&& !inSpec.containsName(partitionSettings.classColumn())) {
			throw new InvalidSettingsException(title + ": Column '" + partitionSettings.classColumn()
					+ "' for stratified sampling " + "does not exist");
		}
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		BufferedDataTable[] parts1 = testSetPartitioner.partition(inData[0], exec.createSubExecutionContext(0.5));
		BufferedDataTable testSetTable = parts1[1];

		BufferedDataTable[] parts2 = calibrationSetPartitioner.partition(parts1[0],
				exec.createSubExecutionContext(0.5));
		BufferedDataTable calibrationSetTable = parts2[1];
		BufferedDataTable trainingSetTable = parts2[0];

		pushFlowVariableInt(FW_ITERATION, iteration);
		pushFlowVariableInt(FW_ITERATIONS_NUM, getIterationsNum());

		iteration++;
		return new BufferedDataTable[] { trainingSetTable, testSetTable, calibrationSetTable };
	}

	@Override
	public boolean terminateLoop() {
		return iteration >= getIterationsNum();
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		iterationsSettings.saveSettingsTo(settings);
		testPartitionSettings.saveSettingsTo(settings.addNodeSettings(KEY_TEST_PARTITION));
		calibrationPartitionSettings.saveSettingsTo(settings.addNodeSettings(KEY_CALIBRATION_PARTITION));
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		iterationsSettings.validateSettings(settings);
		validateSamplingSettings(settings, KEY_TEST_PARTITION, "Training/Test split");
		validateSamplingSettings(settings, KEY_CALIBRATION_PARTITION, "Training/Calibration split");
	}

	/**
	 * Validates sampling settings consistency.
	 * 
	 * @param settings Node settings to load sampling settings from.
	 * @param key      Corresponding setting's key.
	 * @param prefix   Prefix to error message.
	 * @throws InvalidSettingsException
	 */
	private void validateSamplingSettings(NodeSettingsRO settings, String key, String prefix)
			throws InvalidSettingsException {
		try {
			validateSamplingSettings(settings.getNodeSettings(key));
		} catch (InvalidSettingsException e) {
			throw new InvalidSettingsException(prefix + ": " + e.getMessage());
		}
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
		testPartitionSettings.loadSettingsFrom(settings.getNodeSettings(KEY_TEST_PARTITION), false);
		calibrationPartitionSettings.loadSettingsFrom(settings.getNodeSettings(KEY_CALIBRATION_PARTITION), false);
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
		testSetPartitioner.reset();
		calibrationSetPartitioner.reset();
	}

}
