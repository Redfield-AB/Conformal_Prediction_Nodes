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
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

import se.redfield.cp.utils.KnimeUtils;

/**
 * Conformal Prediction Loop End Node. Works with corresponing Start Loop Node.
 * Collects prediction data.<br />
 * 
 * Prediction tables grouped by RowKey. Rank, P, and P-value columns are
 * aggregated using median operator. The rest of the columns aggregated by
 * {@link FirstOperator}.<br />
 * 
 */
public class ConformalPredictorLoopEndNodeModel extends NodeModel implements LoopEndNode {

	public static final String P_COLUMN_REGEX = "^P \\((.+=.+)\\)$";
	public static final String RANK_COLUMN_REGEX = "^Rank \\((.+)\\)$";
	public static final String P_VALUE_COLUMN_REGEX = "^p-value \\((?<value>.+)\\)$";
	public static final String LOWER_COLUMN_REGEX = "^Lower bound \\((.+)\\)$";
	public static final String UPPER_COLUMN_REGEX = "^Upper bound \\((.+)\\)$";

	private static final String ORIGINAL_ROWID_COLUMN_NAME = "Original RowId";
	
	private boolean isClassification = true;

	private int iteration;
	private BufferedDataContainer container;
	private ColumnAggregator[] columnAggregators;

	protected ConformalPredictorLoopEndNodeModel() {
		super(1, 1);
	}

	private List<String> getGroupByCols() {
		return Arrays.asList(ORIGINAL_ROWID_COLUMN_NAME);
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		if (iteration == 0) {
			initColumnAggregators(inSpecs[0]);
		}

		return new DataTableSpec[] { createOutputTableSpec(inSpecs[0]) };
	}

	/**
	 * Create Prediction table output spec.
	 * 
	 * @param inSpec Input prediction table spec.
	 * @return
	 */
	private DataTableSpec createOutputTableSpec(DataTableSpec inSpec) {
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
	 * P-value columns (for classification) and Lower and Upper bound columns (for regression) are aggregated using {@link MedianOperator}. Any additional
	 * columns are aggregated using {@link FirstOperator}.
	 * 
	 * @param inPredictionTableSpec Input prediction table spec.
	 */
	private void initColumnAggregators(DataTableSpec inPredictionTableSpec) {
		List<ColumnAggregator> aggregators = new ArrayList<>();
		List<Pattern> patterns;
		if (isClassification(inPredictionTableSpec)) {
			patterns = Arrays.asList(//
					Pattern.compile(P_COLUMN_REGEX), //
					Pattern.compile(RANK_COLUMN_REGEX), //
					Pattern.compile(P_VALUE_COLUMN_REGEX));
		} else {
			patterns = Arrays.asList(//
					Pattern.compile(LOWER_COLUMN_REGEX), //
					Pattern.compile(UPPER_COLUMN_REGEX));
		}
		

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

	private boolean isClassification(DataTableSpec spec) {
		if (spec.containsName(P_COLUMN_REGEX) && spec.containsName(P_VALUE_COLUMN_REGEX) && spec.containsName(RANK_COLUMN_REGEX))
			return true;
		return false;
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		if (!(getLoopStartNode() instanceof LoopStartNodeTerminator)) {
			throw new IllegalStateException("Loop End is not connected to corresponding Loop Start node.");
		}

		BufferedDataTable inPredictionTable = inData[0];

		if (iteration == 0) {
			container = exec.createDataContainer(createConcatenatedTableSpec(inPredictionTable.getDataTableSpec()));
		}

		boolean terminateLoop = ((LoopStartNodeTerminator) getLoopStartNode()).terminateLoop();
		double maxSubProgress = terminateLoop ? 0.5 : 1;

		appendTable(container, inPredictionTable, exec.createSubExecutionContext(maxSubProgress));

		if (terminateLoop) {
			container.close();
			return new BufferedDataTable[] { collectPredictionTable(exec.createSubExecutionContext(maxSubProgress)) };
		} else {
			iteration++;
			continueLoop();
			return new BufferedDataTable[1];
		}
	}

	/**
	 * Appends all rows from provided table to provided container. Appends an
	 * original rowId cell to each row.
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
					new StringCell(row.getKey().getString())));

			exec.checkCanceled();
			exec.setProgress((double) count++ / totalCount);
		}
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
		BufferedDataTable table = container.getTable();
		List<String> groupByCols = getGroupByCols();

		GlobalSettings globalSettings = GlobalSettings.builder()
				.setFileStoreFactory(FileStoreFactory.createWorkflowFileStoreFactory(exec))
				.setGroupColNames(groupByCols) //
				.setMaxUniqueValues(10000) //
				.setValueDelimiter("") //
				.setDataTableSpec(table.getDataTableSpec()) //
				.setNoOfRows(table.size()) //
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
		container = null;
		columnAggregators = null;
	}

}
