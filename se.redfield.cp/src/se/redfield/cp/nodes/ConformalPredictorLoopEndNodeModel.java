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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.GlobalSettings.AggregationContext;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.general.FirstOperator;
import org.knime.base.data.aggregation.numerical.MedianOperator;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.base.node.preproc.groupby.GroupByTable;
import org.knime.base.node.preproc.groupby.MemoryGroupByTable;
import org.knime.core.data.DataCell;
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
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

import se.redfield.cp.utils.KnimeUtils;

/**
 * Conformal Predictor Loop End Node. Works with corresponing Start Loop Node.
 * Collects calibration, prediction and training model data.<br />
 * 
 * Calibration tables are concatenated, iteration column is added.<br />
 * 
 * Prediction tables grouped by RowKey. Rank, P, and P-value columns are
 * aggregated using median operator. The rest of the column aggregated by
 * {@link FirstOperator}.<br />
 * 
 * Training models port is optional. Any tables passed to this port gets
 * concatenated with addition of iteration column.
 *
 */
public class ConformalPredictorLoopEndNodeModel extends NodeModel implements LoopEndNode {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(ConformalPredictorLoopEndNodeModel.class);

	public static final int PORT_CALIBRATION_TABLE = 0;
	public static final int PORT_PREDICTION_TABLE = 1;
	public static final int PORT_MODEL_TABLE = 2;

	public static final String P_COLUMN_REGEX = "^P \\((.+=.+)\\)$";
	public static final String RANK_COLUMN_REGEX = "^Rank \\((.+)\\)$";
	public static final String SCORE_COLUMN_REGEX = "^P-value \\((?<value>.+)\\)$";

	private static final String ORIGINAL_ROWID_COLUMN_NAME = "Original RowId";

	private static final String DEFAULT_ITERATION_COLUMN_NAME = "Iteration";

	private int iteration;
	private BufferedDataContainer calibrationContainer;
	private BufferedDataContainer predictionContainer;
	private BufferedDataContainer modelContainer;
	private ColumnAggregator[] columnAggregators;

	protected ConformalPredictorLoopEndNodeModel() {
		super(new PortType[] { BufferedDataTable.TYPE_OPTIONAL, BufferedDataTable.TYPE,
				BufferedDataTable.TYPE_OPTIONAL },
				new PortType[] { BufferedDataTable.TYPE, BufferedDataTable.TYPE, BufferedDataTable.TYPE_OPTIONAL });
	}

	private String getIterationColumnName() {
		return DEFAULT_ITERATION_COLUMN_NAME;
	}

	private List<String> getGroupByCols() {
		return Arrays.asList(ORIGINAL_ROWID_COLUMN_NAME);
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		if (iteration == 0) {
			initColumnAggregators(inSpecs[PORT_PREDICTION_TABLE]);
		}

		return new DataTableSpec[] { createCalibrationTableSpec(inSpecs[PORT_CALIBRATION_TABLE]),
				createPredictionTableSpec(inSpecs[PORT_PREDICTION_TABLE]),
				createModelTableSpec(inSpecs[PORT_MODEL_TABLE]) };
	}

	/**
	 * Creates {@link DataTableSpec} for output calibration table by appending
	 * iteration column to input table spec.
	 * 
	 * @param inSpec Input calibration table spec
	 * @return
	 */
	private DataTableSpec createCalibrationTableSpec(DataTableSpec inSpec) {
		return appendIterationColumn(inSpec);
	}

	/**
	 * Appends iteration column to provided {@link DataTableSpec}.
	 * 
	 * @param inSpec Input spec.
	 * @return Result spec.
	 */
	private DataTableSpec appendIterationColumn(DataTableSpec inSpec) {
		if (inSpec == null) {
			return null;
		}
		DataColumnSpec iterColumn = new DataColumnSpecCreator(getIterationColumnName(), IntCell.TYPE).createSpec();
		return KnimeUtils.createSpec(inSpec, iterColumn);
	}

	/**
	 * Create Model table ouput spec by appending iteration column to input table
	 * spec.
	 * 
	 * @param inSpec Input table spec.
	 * @return Result spec.
	 */
	private DataTableSpec createModelTableSpec(DataTableSpec inSpec) {
		return appendIterationColumn(inSpec);
	}

	/**
	 * Create Prediction table output spec.
	 * 
	 * @param inSpec Input prediction table spec.
	 * @return
	 */
	private DataTableSpec createPredictionTableSpec(DataTableSpec inSpec) {
		return GroupByTable.createGroupByTableSpec(createConcatenatedTableSpec(inSpec), getGroupByCols(),
				columnAggregators, ColumnNamePolicy.KEEP_ORIGINAL_NAME);
	}

	/**
	 * Creates new spec by appending 'original row id' column.
	 * 
	 * @param inSpec Original spec.
	 * @return Result spec.
	 */
	private DataTableSpec createConcatenatedTableSpec(DataTableSpec inSpec) {
		DataColumnSpec origRowIdColumn = new DataColumnSpecCreator(ORIGINAL_ROWID_COLUMN_NAME, StringCell.TYPE)
				.createSpec();
		return new DataTableSpec(inSpec, new DataTableSpec(origRowIdColumn));
	}

	/**
	 * Initializes column aggregator used to group predicton table. Rank, P and
	 * P-value columns are aggregated using {@link MedianOperator}. Any additional
	 * columns are aggregated using {@link FirstOperator}.
	 * 
	 * @param inPredictionTableSpec Input prediction table spec.
	 */
	private void initColumnAggregators(DataTableSpec inPredictionTableSpec) {
		List<ColumnAggregator> aggregators = new ArrayList<>();
		List<Pattern> patterns = Arrays.asList(//
				Pattern.compile(P_COLUMN_REGEX), //
				Pattern.compile(RANK_COLUMN_REGEX), //
				Pattern.compile(SCORE_COLUMN_REGEX));

		DataTableSpec spec = createConcatenatedTableSpec(inPredictionTableSpec);

		for (int i = 0; i < spec.getNumColumns(); i++) {
			DataColumnSpec colSpec = spec.getColumnSpec(i);
			ColumnAggregator aggregator = null;

			if (patterns.stream().anyMatch(p -> p.asPredicate().test(colSpec.getName()))) {
				aggregator = new ColumnAggregator(colSpec,
						new MedianOperator(GlobalSettings.DEFAULT, new OperatorColumnSettings(false, colSpec)));
			} else if (!colSpec.getName().equals(ORIGINAL_ROWID_COLUMN_NAME)) {
				aggregator = new ColumnAggregator(colSpec,
						new FirstOperator(GlobalSettings.DEFAULT, new OperatorColumnSettings(true, colSpec)));
			}

			if (aggregator != null) {
				aggregators.add(aggregator);
			}
		}

		columnAggregators = aggregators.toArray(new ColumnAggregator[] {});
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		if (!(getLoopStartNode() instanceof LoopStartNodeTerminator)) {
			throw new IllegalStateException("Loop End is not connected to corresponding Loop Start node.");
		}

		BufferedDataTable inCalibrationTable = inData[PORT_CALIBRATION_TABLE];
		BufferedDataTable inPredictionTable = inData[PORT_PREDICTION_TABLE];
		BufferedDataTable inModelTable = inData[PORT_MODEL_TABLE];

		if (iteration == 0) {
			initContainers(inCalibrationTable, inPredictionTable, inModelTable, exec);
		}

		boolean terminateLoop = ((LoopStartNodeTerminator) getLoopStartNode()).terminateLoop();
		double maxSubProgress = terminateLoop ? 0.25 : 0.33;

		appendTable(calibrationContainer, inCalibrationTable, exec.createSubExecutionContext(maxSubProgress));
		appendTable(predictionContainer, inPredictionTable, exec.createSubExecutionContext(maxSubProgress),
				row -> new StringCell(row.getKey().getString()));
		appendTable(modelContainer, inModelTable, exec.createSubExecutionContext(maxSubProgress));

		if (terminateLoop) {
			return collectResults(exec.createSubExecutionContext(maxSubProgress));
		} else {
			iteration++;
			continueLoop();
			return new BufferedDataTable[3];
		}
	}

	/**
	 * Initializes {@link BufferedDataContainer} objects used to collect data on
	 * each iteration.
	 * 
	 * @param inCalibrationTable Input calibration table.
	 * @param inPredictionTable  Input prediction table.
	 * @param inModelTable       Input model table.
	 * @param exec               Execution context.
	 */
	private void initContainers(BufferedDataTable inCalibrationTable, BufferedDataTable inPredictionTable,
			BufferedDataTable inModelTable, ExecutionContext exec) {
		if (inCalibrationTable != null) {
			calibrationContainer = exec
					.createDataContainer(appendIterationColumn(inCalibrationTable.getDataTableSpec()));
		}
		predictionContainer = exec
				.createDataContainer(createConcatenatedTableSpec(inPredictionTable.getDataTableSpec()));
		if (inModelTable != null) {
			modelContainer = exec.createDataContainer(appendIterationColumn(inModelTable.getDataTableSpec()));
		}
	}

	/**
	 * Appends all row from provided table to provided container along with
	 * appending iteration column.
	 * 
	 * @param cont  Container to add rows.
	 * @param table Table to get rows from.
	 * @param exec  Execution context.
	 * @throws CanceledExecutionException
	 */
	private void appendTable(BufferedDataContainer cont, BufferedDataTable table, ExecutionContext exec)
			throws CanceledExecutionException {
		appendTable(cont, table, exec, row -> new IntCell(iteration));
	}

	/**
	 * Appends all rows from provided table to provided container. Appends a cell to
	 * each row provided by appendCell lambda function.
	 * 
	 * @param cont         Container to add rows.
	 * @param table        Table to get rows from.
	 * @param exec         Execution context.
	 * @param appendedCell Functions that takes a {@link DataRow} and generates
	 *                     appended column for this row.
	 * @throws CanceledExecutionException
	 */
	private void appendTable(BufferedDataContainer cont, BufferedDataTable table, ExecutionContext exec,
			Function<DataRow, DataCell> appendedCell) throws CanceledExecutionException {
		if (cont != null) {
			long count = 0;
			long totalCount = table.size();
			for (DataRow row : table) {
				cont.addRowToTable(new AppendedColumnRow(KnimeUtils.createRowKey(row.getKey(), iteration), row,
						appendedCell.apply(row)));

				exec.checkCanceled();
				exec.setProgress((double) count++ / totalCount);
			}
		}
	}

	/**
	 * Generates output tables from data collected on each iteration
	 * 
	 * @param exec Execution context.
	 * @return Otput tables.
	 * @throws CanceledExecutionException
	 */
	private BufferedDataTable[] collectResults(ExecutionContext exec) throws CanceledExecutionException {
		return new BufferedDataTable[] { getFromContainer(calibrationContainer, exec), collectPredictionTable(exec),
				getFromContainer(modelContainer, exec) };
	}

	/**
	 * Fetches table from nullable {@link BufferedDataContainer}. Closes container
	 * and returns {@link BufferedDataTable}.
	 * 
	 * @param container {@link BufferedDataContainer} to fetch table from. Could be
	 *                  null.
	 * @param exec      Execution context
	 * @return {@link BufferedDataTable} from provided container or Void table in
	 *         case container is null
	 */
	private BufferedDataTable getFromContainer(BufferedDataContainer container, ExecutionContext exec) {
		if (container == null) {
			return exec.createVoidTable(new DataTableSpec());
		}
		container.close();
		return container.getTable();
	}

	/**
	 * Generates output prediction table. Groups collected rows by original RowKey
	 * and preconfigured columnAggregators.
	 * 
	 * @param exec Execution context.
	 * @return Output Prediction table.
	 * @throws CanceledExecutionException
	 */
	private BufferedDataTable collectPredictionTable(ExecutionContext exec) throws CanceledExecutionException {
		BufferedDataTable table = getFromContainer(predictionContainer, exec);
		List<String> groupByCols = getGroupByCols();

		GlobalSettings globalSettings = GlobalSettings.builder()
				.setFileStoreFactory(FileStoreFactory.createWorkflowFileStoreFactory(exec))
				.setGroupColNames(groupByCols)//
				.setMaxUniqueValues(10000)//
				.setValueDelimiter("")//
				.setDataTableSpec(table.getDataTableSpec())//
				.setNoOfRows(table.size())//
				.setAggregationContext(AggregationContext.ROW_AGGREGATION).build();

		MemoryGroupByTable res = new MemoryGroupByTable(exec, table, groupByCols, columnAggregators, globalSettings,
				false, ColumnNamePolicy.KEEP_ORIGINAL_NAME, false);

		return res.getBufferedTable();
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
		predictionContainer = null;
		modelContainer = null;
		columnAggregators = null;
	}

}
