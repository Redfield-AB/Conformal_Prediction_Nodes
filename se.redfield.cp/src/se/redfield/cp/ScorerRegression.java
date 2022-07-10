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
package se.redfield.cp;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import se.redfield.cp.nodes.ConformalPredictorScorerRegressionNodeModel;

/**
 * Class used by Regression Scorer node to calculate quality metrics for a given
 * prediction table.
 *
 */
public class ScorerRegression {

	private ConformalPredictorScorerRegressionNodeModel model;

	public ScorerRegression(ConformalPredictorScorerRegressionNodeModel conformalPredictorScorerRegressionNodeModel) {
		this.model = conformalPredictorScorerRegressionNodeModel;
	}

	/**
	 * Creates output table spec.
	 */
	public DataTableSpec createOutputSpec() {
		List<DataColumnSpec> specs = new ArrayList<>();
		if (model.isAdditionalInfoMode()) {
			specs.add(new DataColumnSpecCreator("Median interval size", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Min interval size", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Max interval size", DoubleCell.TYPE).createSpec());
		}
		specs.add(new DataColumnSpecCreator("Error rate", DoubleCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator("Mean interval size", DoubleCell.TYPE).createSpec());
		return new DataTableSpec(specs.toArray(new DataColumnSpec[] {}));
	}

	/**
	 * Processes input table. Collects the following metrics for each row: *
	 * <ul>
	 * <li>Error rate =
	 * <li>Efficiency =
	 * </ul>
	 * 
	 * @param inTable Input table.
	 * @param exec    Execution context.
	 * @return
	 * @throws CanceledExecutionException
	 */
	public BufferedDataTable process(BufferedDataTable inTable, ExecutionContext exec)
			throws CanceledExecutionException {
		RegressionScores score = new RegressionScores();

		DataTableSpec spec = inTable.getDataTableSpec();
		int targetIdx = spec.findColumnIndex(model.getTargetColumn());
		int upperboundIdx = spec.findColumnIndex(model.getUpperBoundColumnName());
		int lowerboundIdx = spec.findColumnIndex(model.getLowerBoundColumnName());
		double maxIntervalsSize = 0.;
		double minIntervalsSize = 1.;

		long total = inTable.size();
		long count = 0;

		List<Double> interval = new ArrayList<>();

		for (DataRow row : inTable) {
			score.inc(Metric.COUNT);

			double regression = ((DoubleValue) row.getCell(targetIdx)).getDoubleValue();
			double lowerboundRegression = ((DoubleValue) row.getCell(lowerboundIdx)).getDoubleValue();
			double upperboundRegression = ((DoubleValue) row.getCell(upperboundIdx)).getDoubleValue();
			double intervalsSize = upperboundRegression - lowerboundRegression;// trouble!!!!

			if (intervalsSize > maxIntervalsSize) {
				maxIntervalsSize = intervalsSize;
				score.set(Metric.MAX_INTERVAL_SIZE, maxIntervalsSize);
			}
			if (intervalsSize < minIntervalsSize) {
				minIntervalsSize = intervalsSize;
				score.set(Metric.MIN_INTERVAL_SIZE, minIntervalsSize);
			}

			if (regression >= lowerboundRegression && regression <= upperboundRegression) {
				score.inc(Metric.VALIDATE);
			}

			score.add(Metric.MEAN_INTERVAL_SIZE, intervalsSize);
			interval.add(intervalsSize);

			exec.checkCanceled();
			exec.setProgress((double) count / total);
			count++;
		}

		interval.sort(null);
		double median = interval.get(interval.size() / 2);
		score.set(Metric.MEDIAN_INTERVAL_SIZE, median);

		return createOutputTable(score, exec);

	}

	/**
	 * Creates {@link BufferedDataTable} from collected scores.
	 * 
	 * @param scores Collected scores.
	 * @param exec   Execution context.
	 * @return
	 */
	private BufferedDataTable createOutputTable(RegressionScores score, ExecutionContext exec) {
		BufferedDataContainer cont = exec.createDataContainer(createOutputSpec());
		cont.addRowToTable(createRow(score, 0)); // only one row, as it is the total results we are calculating
		cont.close();
		return cont.getTable();
	}

	/**
	 * Creates a single score row.
	 * 
	 * @param score Scores object.
	 * @param idx   Row index.
	 * @return Row.
	 */
	private DataRow createRow(RegressionScores score, long idx) {
		List<DataCell> cells = new ArrayList<>();
		if (model.isAdditionalInfoMode()) {
			cells.add(new DoubleCell(score.getMedianIntervalSize()));
			cells.add(new DoubleCell(score.getMinIntervalSize()));
			cells.add(new DoubleCell(score.getMaxIntervalSize()));
		}
		cells.add(new DoubleCell(score.getErrorRate()));
		cells.add(new DoubleCell(score.getMeanIntervalSize()));
		return new DefaultRow(RowKey.createRowKey(idx), cells);
	}

	/**
	 * Class that holds all metrics calculated from input table.
	 *
	 */
	private class RegressionScores {
		private Map<Metric, Double> metrics;

		public RegressionScores() {
			this.metrics = new EnumMap<>(Metric.class);
		}

		public void add(Metric m, double value) {
			metrics.compute(m, (k, v) -> v == null ? value : v + value);
		}

		public void inc(Metric m) {
			metrics.compute(m, (k, v) -> v == null ? 1.0 : v + 1);
		}

		public void set(Metric m, double value) {
			metrics.put(m, value);
		}

		public double get(Metric m) {
			return metrics.getOrDefault(m, 0.0);
		}

		public double getErrorRate() {
			return 1 - get(Metric.VALIDATE) / get(Metric.COUNT);
		}

		public double getMeanIntervalSize() {
			return get(Metric.MEAN_INTERVAL_SIZE) / get(Metric.COUNT);
		}

		public double getMaxIntervalSize() {
			return get(Metric.MAX_INTERVAL_SIZE);
		}

		public double getMinIntervalSize() {
			return get(Metric.MIN_INTERVAL_SIZE);
		}

		public double getMedianIntervalSize() {

			return get(Metric.MEDIAN_INTERVAL_SIZE);
		}
	}

	private enum Metric {
		VALIDATE, MEAN_INTERVAL_SIZE, MEDIAN_INTERVAL_SIZE, COUNT, ERROR_RATE, EFFICIENCY, MAX_INTERVAL_SIZE,
		MIN_INTERVAL_SIZE
	}
}