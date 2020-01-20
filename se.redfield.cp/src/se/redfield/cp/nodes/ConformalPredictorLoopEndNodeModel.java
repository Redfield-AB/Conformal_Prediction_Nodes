package se.redfield.cp.nodes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.GlobalSettings.AggregationContext;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.general.FirstOperator;
import org.knime.base.data.aggregation.numerical.MedianOperator;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.base.node.preproc.groupby.MemoryGroupByTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

import se.redfield.cp.utils.KnimeUtils;

public class ConformalPredictorLoopEndNodeModel extends NodeModel implements LoopEndNode {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(ConformalPredictorLoopEndNodeModel.class);

	public static final int PORT_CALIBRATION_TABLE = 0;
	public static final int PORT_PREDICTION_TABLE = 1;
	public static final int PORT_MODEL_TABLE = 2;

	private static final String P_COLUMN_REGEX = "^P \\((.+=.+)\\)$";
	private static final String RANK_COLUMN_REGEX = "^Rank \\((.+)\\)$";
	private static final String SCORE_COLUMN_REGEX = "^Score \\((.+)\\)$";

	private static final String ORIGINAL_ROWID_COLUMN_NAME = "Group ID";

	private static final String DEFAULT_ITERATION_COLUMN_NAME = "Iteration";

	private int iteration;
	private BufferedDataContainer calibrationContainer;
	private BufferedDataContainer predictionContainer;
	private BufferedDataContainer modelContainer;

	protected ConformalPredictorLoopEndNodeModel() {
		super(3, 3);
	}

	private String getIterationColumnName() {
		return DEFAULT_ITERATION_COLUMN_NAME;
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		if (!(getLoopStartNode() instanceof LoopStartNodeTerminator)) {
			throw new IllegalStateException("Loop End is not connected to corresponding Loop Start node.");
		}

		BufferedDataTable inCalibrationTable = inData[PORT_CALIBRATION_TABLE];
		BufferedDataTable inPredictionTable = inData[PORT_PREDICTION_TABLE];
		BufferedDataTable inModelTable = inData[PORT_MODEL_TABLE];

		if (calibrationContainer == null) {
			iteration = 0;
			calibrationContainer = exec
					.createDataContainer(appendIterationColumn(inCalibrationTable.getDataTableSpec()));
			predictionContainer = exec
					.createDataContainer(createConcatenatedTableSpec(inPredictionTable.getDataTableSpec()));
			modelContainer = exec.createDataContainer(appendIterationColumn(inModelTable.getDataTableSpec()));
		}

		for (DataRow row : inCalibrationTable) {
			calibrationContainer.addRowToTable(new AppendedColumnRow(KnimeUtils.createRowKey(row.getKey(), iteration),
					row, new IntCell(iteration)));
		}

		for (DataRow row : inPredictionTable) {
			predictionContainer.addRowToTable(new AppendedColumnRow(KnimeUtils.createRowKey(row.getKey(), iteration),
					row, new StringCell(row.getKey().getString())));
		}

		for (DataRow row : inModelTable) {
			modelContainer.addRowToTable(new AppendedColumnRow(KnimeUtils.createRowKey(row.getKey(), iteration), row,
					new IntCell(iteration)));
		}

		boolean terminateLoop = ((LoopStartNodeTerminator) getLoopStartNode()).terminateLoop();
		if (terminateLoop) {
			calibrationContainer.close();
			predictionContainer.close();
			modelContainer.close();

			return new BufferedDataTable[] { calibrationContainer.getTable(), collectPredictionTable(exec),
					modelContainer.getTable() };
		} else {
			iteration++;
			continueLoop();
			return new BufferedDataTable[3];
		}
	}

	private DataTableSpec createConcatenatedTableSpec(DataTableSpec inSpec) {
		DataColumnSpec origRowIdColumn = new DataColumnSpecCreator(ORIGINAL_ROWID_COLUMN_NAME, StringCell.TYPE)
				.createSpec();
		return new DataTableSpec(inSpec, new DataTableSpec(origRowIdColumn));
	}

	private BufferedDataTable collectPredictionTable(ExecutionContext exec) throws CanceledExecutionException {
		BufferedDataTable table = predictionContainer.getTable();
		DataTableSpec spec = table.getSpec();
		List<String> groupByCols = Arrays.asList(ORIGINAL_ROWID_COLUMN_NAME);

		List<ColumnAggregator> aggregators = new ArrayList<>();
		List<Pattern> patterns = Arrays.asList(//
				Pattern.compile(P_COLUMN_REGEX), //
				Pattern.compile(RANK_COLUMN_REGEX), //
				Pattern.compile(SCORE_COLUMN_REGEX));

		GlobalSettings globalSettings = GlobalSettings.builder()
				.setFileStoreFactory(FileStoreFactory.createWorkflowFileStoreFactory(exec))
				.setGroupColNames(groupByCols)//
				.setMaxUniqueValues(10000)//
				.setValueDelimiter("")//
				.setDataTableSpec(table.getDataTableSpec())//
				.setNoOfRows(table.size())//
				.setAggregationContext(AggregationContext.ROW_AGGREGATION).build();

		for (int i = 0; i < spec.getNumColumns(); i++) {
			DataColumnSpec colSpec = spec.getColumnSpec(i);
			ColumnAggregator aggregator = null;

			if (patterns.stream().anyMatch(p -> p.asPredicate().test(colSpec.getName()))) {
				aggregator = new ColumnAggregator(colSpec,
						new MedianOperator(globalSettings, new OperatorColumnSettings(false, colSpec)));
			} else if (!colSpec.getName().equals(ORIGINAL_ROWID_COLUMN_NAME)) {
				aggregator = new ColumnAggregator(colSpec,
						new FirstOperator(globalSettings, new OperatorColumnSettings(true, colSpec)));
			}

			if (aggregator != null) {
				aggregators.add(aggregator);
			}
		}

		MemoryGroupByTable res = new MemoryGroupByTable(exec, table, groupByCols,
				aggregators.toArray(new ColumnAggregator[] {}), globalSettings, false,
				ColumnNamePolicy.KEEP_ORIGINAL_NAME, false);

		return res.getBufferedTable();
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		return new DataTableSpec[] { appendIterationColumn(inSpecs[PORT_CALIBRATION_TABLE]), null,
				appendIterationColumn(inSpecs[PORT_MODEL_TABLE]) };// TODO prediction table spec
	}

	private DataTableSpec appendIterationColumn(DataTableSpec inSpec) {
		DataColumnSpec iterColumn = new DataColumnSpecCreator(getIterationColumnName(), IntCell.TYPE).createSpec();
		return KnimeUtils.createSpec(inSpec, iterColumn);
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		// TODO Auto-generated method stub

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
		iteration = -1;
		calibrationContainer = null;
		predictionContainer = null;
	}

}
