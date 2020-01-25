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

public class ConformalPredictorLoopStartNodeModel extends NodeModel implements LoopStartNodeTerminator {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(ConformalPredictorLoopStartNodeModel.class);

	private static final String KEY_ITERATIONS = "iterations";
	public static final String KEY_TEST_PARTITION = "testPartition";
	public static final String KEY_CALIBRATION_PARTITION = "calibrationPartition";

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
		checkSettings(testPartitionSettings, in, "Test Set");
		checkSettings(calibrationPartitionSettings, in, "Calibration Set");
		return new DataTableSpec[] { in, in, in };
	}

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
		BufferedDataTable[] parts1 = testSetPartitioner.partition(inData[0], exec);
		BufferedDataTable testSetTable = parts1[0];

		BufferedDataTable[] parts2 = calibrationSetPartitioner.partition(parts1[1], exec);
		BufferedDataTable calibrationSetTable = parts2[0];
		BufferedDataTable trainingSetTable = parts2[1];

		iteration++;
		return new BufferedDataTable[] { trainingSetTable, calibrationSetTable, testSetTable };
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
		validateSamplingSettings(settings, KEY_TEST_PARTITION, "Test Set");
		validateSamplingSettings(settings, KEY_CALIBRATION_PARTITION, "Calibration Set");
	}

	private void validateSamplingSettings(NodeSettingsRO settings, String key, String prefix)
			throws InvalidSettingsException {
		try {
			validateSamplingSettings(settings.getNodeSettings(key));
		} catch (InvalidSettingsException e) {
			throw new InvalidSettingsException(prefix + ": " + e.getMessage());
		}
	}

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
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void reset() {
		iteration = 0;
		testSetPartitioner.reset();
		calibrationSetPartitioner.reset();
	}

}
