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
package se.redfield.cp.core.scoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import se.redfield.cp.core.scoring.Scores.Metric;
import se.redfield.cp.settings.ConformalPredictorScorerRegressionSettings;

/**
 * Class used by Regression Scorer node to calculate quality metrics for a given
 * prediction table.
 *
 */
public class ScorerRegression {

	private ConformalPredictorScorerRegressionSettings settings;
	private List<ScoreColumn> baseColumns;
	private List<ScoreColumn> additionalColumns;

	/**
	 * @param settings The scorer settings.
	 */
	public ScorerRegression(ConformalPredictorScorerRegressionSettings settings) {
		this.settings = settings;

		baseColumns = Arrays.asList(new ScoreColumn("Error rate",
				s -> 1 - s.get(Scores.Metric.VALID) / s.get(Scores.Metric.COUNT)), //
				new ScoreColumn("Mean interval size", Scores.Metric.INTERVAL_SIZE, true));

		additionalColumns = Arrays.asList(
				new ScoreColumn("Median interval size", Scores.Metric.MEDIAN_INTERVAL_SIZE), //
				new ScoreColumn("Min interval size", Scores.Metric.MIN_INTERVAL_SIZE), //
				new ScoreColumn("Max interval size", Scores.Metric.MAX_INTERVAL_SIZE));
	}

	/**
	 * Creates output table spec.
	 * 
	 * @return The table spec.
	 */
	public DataTableSpec createOutputSpec() {
		DataColumnSpec[] specs = getIncludedColumns().stream().map(ScoreColumn::createSpec)
				.toArray(DataColumnSpec[]::new);
		return new DataTableSpec(specs);
	}

	private List<ScoreColumn> getIncludedColumns() {
		List<ScoreColumn> result = new ArrayList<>();
		if (settings.isAdditionalInfoMode()) {
			result.addAll(additionalColumns);
		}
		result.addAll(baseColumns);
		return result;
	}

	/**
	 * Processes input table and creates a table with scores.
	 * 
	 * @param inTable Input table.
	 * @param exec    Execution context.
	 * @return The scores table.
	 * @throws CanceledExecutionException
	 */
	public BufferedDataTable process(BufferedDataTable inTable, ExecutionContext exec)
			throws CanceledExecutionException {
		Scores score = new Scores();

		DataTableSpec spec = inTable.getDataTableSpec();
		int targetIdx = spec.findColumnIndex(settings.getTargetColumn());
		int upperboundIdx = settings.hasUpperBound() ? spec.findColumnIndex(settings.getUpperBoundColumnName()) : -1;
		int lowerboundIdx = settings.hasLowerBound() ? spec.findColumnIndex(settings.getLowerBoundColumnName()) : -1;

		long total = inTable.size();
		long count = 0;

		List<Double> interval = new ArrayList<>();

		for (DataRow row : inTable) {
			score.inc(Metric.COUNT);

			double regression = ((DoubleValue) row.getCell(targetIdx)).getDoubleValue();
			double lowerboundRegression = lowerboundIdx > -1
					? ((DoubleValue) row.getCell(lowerboundIdx)).getDoubleValue()
					: Double.NEGATIVE_INFINITY;
			double upperboundRegression = upperboundIdx > -1
					? ((DoubleValue) row.getCell(upperboundIdx)).getDoubleValue()
					: Double.POSITIVE_INFINITY;
			double intervalsSize = upperboundRegression - lowerboundRegression;// trouble!!!!

			score.max(Metric.MAX_INTERVAL_SIZE, intervalsSize);
			score.min(Metric.MIN_INTERVAL_SIZE, intervalsSize);

			if (regression >= lowerboundRegression && regression <= upperboundRegression) {
				score.inc(Metric.VALID);
			}

			score.add(Metric.INTERVAL_SIZE, intervalsSize);
			interval.add(intervalsSize);

			exec.checkCanceled();
			exec.setProgress((double) count / total);
			count++;
		}

		if (settings.isAdditionalInfoMode()) {
			interval.sort(null);
			double median = interval.get(interval.size() / 2);
			score.set(Metric.MEDIAN_INTERVAL_SIZE, median);
		}

		return createOutputTable(score, exec);

	}

	private BufferedDataTable createOutputTable(Scores score, ExecutionContext exec) {
		BufferedDataContainer cont = exec.createDataContainer(createOutputSpec());
		cont.addRowToTable(createRow(score));
		cont.close();
		return cont.getTable();
	}

	private DataRow createRow(Scores score) {
		List<DataCell> cells = getIncludedColumns().stream().map(c -> c.createCell(score)).collect(Collectors.toList());
		return new DefaultRow(RowKey.createRowKey(0L), cells);
	}

}