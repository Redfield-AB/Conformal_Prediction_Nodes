package se.redfield.cp.nodes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableOperator;

import se.redfield.cp.utils.ColumnPatternExtractor;

public class ConformalPredictorClassifierNodeModel extends NodeModel {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(ConformalPredictorClassifierNodeModel.class);

	private static final String KEY_SCORE_THRESHOLD = "scoreThreshold";

	private static final double DEFAULT_SCORE_THRESHOLD = 0.8;
	public static final String DEFAULT_CLASSES_COLUMN_NAME = "Classes";

	private static final SettingsModelDoubleBounded scoreThresholdSettings = createScoreThresholdSettings();

	private ColumnRearranger rearranger;

	static SettingsModelDoubleBounded createScoreThresholdSettings() {
		return new SettingsModelDoubleBounded(KEY_SCORE_THRESHOLD, DEFAULT_SCORE_THRESHOLD, 0, 1);
	}

	protected ConformalPredictorClassifierNodeModel() {
		super(1, 1);
	}

	private double getScoreThreshold() {
		return scoreThresholdSettings.getDoubleValue();
	}

	private String getScoreColumnPattern() {
		return ConformalPredictorLoopEndNodeModel.SCORE_COLUMN_REGEX;
	}

	private String getClassesColumnName() {
		return DEFAULT_CLASSES_COLUMN_NAME;
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inData[0], rearranger, exec) };
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		Map<String, Integer> scoreColumns = new ColumnPatternExtractor(getScoreColumnPattern()).match(inSpecs[0]);
		if (scoreColumns.isEmpty()) {
			throw new InvalidSettingsException("No Score columns found in provided table");
		}

		rearranger = createRearranger(inSpecs[0], scoreColumns);

		return new DataTableSpec[] { rearranger.createSpec() };
	}

	private ColumnRearranger createRearranger(DataTableSpec inSpec, Map<String, Integer> scoreColumns) {
		ColumnRearranger r = new ColumnRearranger(inSpec);
		r.append(new ClassifierCellFactory(scoreColumns));
		return r;
	}

	private class ClassifierCellFactory extends AbstractCellFactory {

		private Map<String, Integer> scoreColumns;

		public ClassifierCellFactory(Map<String, Integer> scoreColumns) {
			super(createClassColumnSpec());
			this.scoreColumns = scoreColumns;
		}

		@Override
		public DataCell[] getCells(DataRow row) {
			List<DataCell> classes = new ArrayList<>();
			for (Entry<String, Integer> e : scoreColumns.entrySet()) {
				double score = ((DoubleValue) row.getCell(e.getValue())).getDoubleValue();
				if (score > getScoreThreshold()) {
					classes.add(new StringCell(e.getKey()));
				}
			}
			return new DataCell[] { CollectionCellFactory.createSetCell(classes) };
		}

	}

	private DataColumnSpec createClassColumnSpec() {
		return new DataColumnSpecCreator(getClassesColumnName(), SetCell.getCollectionType(StringCell.TYPE))
				.createSpec();
	}

	@Override
	public StreamableOperator createStreamableOperator(PartitionInfo partitionInfo, PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		return rearranger.createStreamableFunction();
	}

	@Override
	public InputPortRole[] getInputPortRoles() {
		return new InputPortRole[] { InputPortRole.DISTRIBUTED_STREAMABLE };
	}

	@Override
	public OutputPortRole[] getOutputPortRoles() {
		return new OutputPortRole[] { OutputPortRole.DISTRIBUTED };
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		scoreThresholdSettings.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		scoreThresholdSettings.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		scoreThresholdSettings.loadSettingsFrom(settings);
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
		rearranger = null;
	}

}
