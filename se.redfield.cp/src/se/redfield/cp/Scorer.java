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

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import se.redfield.cp.nodes.ConformalPredictorScorerNodeModel;

/**
 * Class used by Conformal Scorer node to calculate quality metrics for a given
 * prediction table.
 *
 */
public class Scorer {

	private ConformalPredictorScorerNodeModel model;

	public Scorer(ConformalPredictorScorerNodeModel conformalPredictorScorerNodeModel) {
		this.model = conformalPredictorScorerNodeModel;
	}

	/**
	 * Creates output table spec.
	 */
	public DataTableSpec createOutputSpec() {
		List<DataColumnSpec> specs = new ArrayList<>();
		specs.add(new DataColumnSpecCreator("Target", StringCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator("Efficiency", DoubleCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator("Validity", DoubleCell.TYPE).createSpec());
		if (model.isAdditionalInfoMode()) {
			specs.add(new DataColumnSpecCreator("Exact match", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Soft match", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Total match", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Error", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Total", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Single class predictions", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Null predictions", DoubleCell.TYPE).createSpec());
		}
		return new DataTableSpec(specs.toArray(new DataColumnSpec[] {}));
	}

	public DataTableSpec createAdditionalEfficiencyMetricSpec() {
		List<DataColumnSpec> specs = new ArrayList<>();
		specs.add(new DataColumnSpecCreator("Efficiency", DoubleCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator("Validity", DoubleCell.TYPE).createSpec());
		if (model.isAdditionalInfoMode()) {
			specs.add(new DataColumnSpecCreator("Exact match", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Soft match", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Total match", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Error", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Total", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Single class predictions", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Null predictions", DoubleCell.TYPE).createSpec());
		}
		if (model.isAdditionalEfficiencyMetricsMode()) {
			specs.add(new DataColumnSpecCreator("Sum of p-values", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Unconfidence", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Fuzziness", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Observed unconfidence", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Observed Fuzziness", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Number of Labels", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Multiple", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Excess", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Observed multiple", DoubleCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Observed Excess", DoubleCell.TYPE).createSpec());
		}
		return new DataTableSpec(specs.toArray(new DataColumnSpec[] {}));
	}

	/**
	 * Processes input table. Callects the following metrics for each row: *
	 * <ul>
	 * <li>Exact match � number of correct predictions that belong to one class, and
	 * not belong to any mixed class.</li>
	 * <li>Soft match - number of correct predictions that belong to one of the
	 * mixed classes.</li>
	 * <li>Total match � Exact_match + Soft_match.</li>
	 * <li>Error � number of predictions that does not match real target class.</li>
	 * <li>Total � total number of records that belongs to the current target
	 * class.</li>
	 * <li>Efficiency = Exact_match/(Exact_match + Error)</li>
	 * <li>Validity = Total_match/Total</li>
	 * </ul>
	 * 
	 * @param inTable Input table.
	 * @param exec    Execution context.
	 * @return
	 * @throws CanceledExecutionException
	 */
	public BufferedDataTable[] process(BufferedDataTable inTable, ExecutionContext exec)
			throws CanceledExecutionException {
		Map<String, ClassScores> scores = new HashMap<>();
		DataTableSpec inputTableSpec = inTable.getSpec();
		ClassScores efficiencyScores = new ClassScores(null);

		DataTableSpec spec = inTable.getDataTableSpec();
		int targetIdx = spec.findColumnIndex(model.getTargetColumn());
		int classesIdx = spec.findColumnIndex(model.getClassesColumn());

		long total = inTable.size();
		long count = 0;

		for (DataRow row : inTable) {
			String target = row.getCell(targetIdx).toString();
			Set<String> classes = getClasses(row.getCell(classesIdx));
			ClassScores score = scores.computeIfAbsent(target, ClassScores::new);

			if (classes.contains(target)) {
				if (classes.size() == 1) {
					score.inc(Metric.STRICT_MATCH);
				} else {
					score.inc(Metric.SOFT_MATCH);
				}
			} else {
				score.inc(Metric.ERROR);
			}

			if (classes.isEmpty()) {
				score.inc(Metric.NULL_CLASS);
			} else if (classes.size() == 1) {
				score.inc(Metric.SINGLE_CLASS);
			}

			if (model.isAdditionalEfficiencyMetricsMode()) {
				efficiencyScores.inc(Metric.COUNT);

				Map<String, Double> pValues = inputTableSpec.getColumnSpec(targetIdx).getDomain().getValues().stream()
						.map(DataCell::toString)
						.collect(Collectors.toMap(str -> str,
								str -> ((DoubleCell) row
										.getCell(inputTableSpec.findColumnIndex(model.getProbabilityColumnName(str))))
										.getDoubleValue()));
				double max = 0, second = 0, sum = 0;
				for (Double p : pValues.values()) {
					sum += p;
					efficiencyScores.add(Metric.S, p);// Average Sum of p-values
					if (p > max) {
						max = p;
						second = max;
					} else if (p > second) {
						second = p;
					}
				}
				efficiencyScores.add(Metric.U, second);// Unconfidence
				efficiencyScores.add(Metric.F, sum - max);// Fuziness
				efficiencyScores.add(Metric.N, classes.size());
				if (classes.size() > 1)
					efficiencyScores.inc(Metric.M);
				efficiencyScores.add(Metric.E, classes.size() - 1);
				if (max == pValues.get(target))
					efficiencyScores.add(Metric.OU, second);
				else
					efficiencyScores.add(Metric.OU, max);
				efficiencyScores.add(Metric.OF, sum - pValues.get(target));// Observed Fuziness

				if (classes.contains(target)) {
					efficiencyScores.add(Metric.OE, classes.size() - 1);
					if (classes.size() > 1)
						efficiencyScores.inc(Metric.OM);
				} else {
					efficiencyScores.add(Metric.OE, classes.size());
					if (classes.size() > 0)
						efficiencyScores.inc(Metric.OM);
				}

			}

			exec.checkCanceled();
			exec.setProgress((double) count++ / total);
		}

		return createOutputTables(scores, efficiencyScores, exec);
	}

	private double getUnconfidence() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Creates {@link BufferedDataTable} from collected scores.
	 * 
	 * @param scores Collected scores.
	 * @param exec   Execution context.
	 * @return
	 */
	private BufferedDataTable[] createOutputTables(Map<String, ClassScores> scores, ClassScores efficiencyScores,
			ExecutionContext exec) {
		BufferedDataContainer cont = exec.createDataContainer(createOutputSpec());
		BufferedDataContainer efficiencyCont = exec.createDataContainer(createAdditionalEfficiencyMetricSpec());

		long idx = 0;
		for (ClassScores s : scores.values()) {
			cont.addRowToTable(createRow(s, idx++));
		}
		cont.close();

		idx = 0;
		efficiencyCont.addRowToTable(createSummaryRow(scores, efficiencyScores, idx++));
		efficiencyCont.close();

		return new BufferedDataTable[] { cont.getTable(), efficiencyCont.getTable() };
	}

	private DataRow createSummaryRow(Map<String, ClassScores> scores, ClassScores score, long idx) {
		List<DataCell> cells = new ArrayList<>();
		double efficency = 0, validity = 0, strictMatch = 0, softMatch = 0, totalMatch = 0, error = 0, total = 0,
				single = 0, empty = 0;
		int c = scores.size();
		for (ClassScores s : scores.values()) {
			efficency += s.getEfficiency();
			validity += s.getValidity();
			if (model.isAdditionalInfoMode()) {
				strictMatch += s.get(Metric.STRICT_MATCH);
				softMatch += s.get(Metric.SOFT_MATCH);
				totalMatch += s.getTotalMatch();
				error += s.get(Metric.ERROR);
				total += s.getTotal();
				single += s.get(Metric.SINGLE_CLASS);
				empty += s.get(Metric.NULL_CLASS);
			}
		}
		cells.add(new DoubleCell(efficency / c));
		cells.add(new DoubleCell(validity / c));
		if (model.isAdditionalInfoMode()) {
			cells.add(new DoubleCell(strictMatch));
			cells.add(new DoubleCell(softMatch));
			cells.add(new DoubleCell(totalMatch));
			cells.add(new DoubleCell(error));
			cells.add(new DoubleCell(total));
			cells.add(new DoubleCell(single));
			cells.add(new DoubleCell(empty));
		}
		if (model.isAdditionalEfficiencyMetricsMode()) {
			cells.add(new DoubleCell(score.getS())); // Sum of p-values
			cells.add(new DoubleCell(score.getU())); // Unconfidence
			cells.add(new DoubleCell(score.getF())); // Fuzziness
			cells.add(new DoubleCell(score.getOU())); // Observed unconfidence
			cells.add(new DoubleCell(score.getOF())); // Observed Fuzziness
			cells.add(new DoubleCell(score.getN())); // Number of Labels
			cells.add(new DoubleCell(score.getM())); // Multiple
			cells.add(new DoubleCell(score.getE())); // Excess
			cells.add(new DoubleCell(score.getOM())); // Observed multiple
			cells.add(new DoubleCell(score.getOE())); // Observed Excess
		}
		return new DefaultRow(RowKey.createRowKey(idx), cells);
	}

	/**
	 * Creates a single score row.
	 * 
	 * @param score Scores object.
	 * @param idx   Row index.
	 * @return Row.
	 */
	private DataRow createRow(ClassScores score, long idx) {
		List<DataCell> cells = new ArrayList<>();
		cells.add(new StringCell(score.getTarget()));
		cells.add(new DoubleCell(score.getEfficiency()));
		cells.add(new DoubleCell(score.getValidity()));
		if (model.isAdditionalInfoMode()) {
			cells.add(new DoubleCell(score.get(Metric.STRICT_MATCH)));
			cells.add(new DoubleCell(score.get(Metric.SOFT_MATCH)));
			cells.add(new DoubleCell(score.getTotalMatch()));
			cells.add(new DoubleCell(score.get(Metric.ERROR)));
			cells.add(new DoubleCell(score.getTotal()));
			cells.add(new DoubleCell(score.get(Metric.SINGLE_CLASS)));
			cells.add(new DoubleCell(score.get(Metric.NULL_CLASS)));
		}
		return new DefaultRow(RowKey.createRowKey(idx), cells);
	}

	/**
	 * Collects predicted classes as a string set from Classes cell.
	 * 
	 * @param cell Classes cell.
	 * @return
	 */
	private Set<String> getClasses(DataCell cell) {
		if (cell.isMissing()) {
			return Collections.emptySet();
		}
		if (cell.getType().isCollectionType()) {
			return ((CollectionDataValue) cell).stream().map(DataCell::toString).collect(toSet());
		} else {
			return new HashSet<>(Arrays.asList(cell.toString().split(model.getStringSeparator())));
		}
	}

	/**
	 * Class that holds all metrics calculated from input table.
	 *
	 */
	private class ClassScores {
		private String target;
		private Map<Metric, Double> metrics;

		public ClassScores(String target) {
			this.target = target;
			this.metrics = new EnumMap<>(Metric.class);
		}

		public void set(Metric m, double value) {
			this.metrics.put(m, value);
		}

		public double getOE() {
			return get(Metric.OE) / get(Metric.COUNT);
		}

		public double getOM() {
			return get(Metric.OM) / get(Metric.COUNT);
		}

		public double getE() {
			return get(Metric.E) / get(Metric.COUNT);
		}

		public double getM() {
			return get(Metric.M) / get(Metric.COUNT);
		}

		public double getN() {
			return get(Metric.N) / get(Metric.COUNT);
		}

		public double getOF() {
			return get(Metric.OF) / get(Metric.COUNT);
		}

		public double getOU() {
			return get(Metric.OU) / get(Metric.COUNT);
		}

		public double getF() {
			return get(Metric.F) / get(Metric.COUNT);
		}

		public double getU() {
			return get(Metric.U) / get(Metric.COUNT);
		}

		public double getS() {
			return get(Metric.S) / get(Metric.COUNT);
		}

		public void inc(Metric m) {
			metrics.compute(m, (k, v) -> v == null ? 1.0 : v + 1);
		}

		public void add(Metric m, double out_intervalsSize) {
			metrics.compute(m, (k, v) -> v == null ? out_intervalsSize : v + out_intervalsSize);
		}

		public double get(Metric m) {
			return metrics.getOrDefault(m, 0.0);
		}

		public String getTarget() {
			return target;
		}

		public double getTotalMatch() {
			return get(Metric.STRICT_MATCH) + get(Metric.SOFT_MATCH);
		}

		public double getTotal() {
			return getTotalMatch() + get(Metric.ERROR);
		}

		public double getEfficiency() {
			return (double) get(Metric.SINGLE_CLASS) / getTotal();
		}

		public double getValidity() {
			return (double) getTotalMatch() / getTotal();
		}
	}

	private enum Metric {
		STRICT_MATCH, SOFT_MATCH, ERROR, SINGLE_CLASS, NULL_CLASS, S, U, F, OU, OF, N, M, E, OM, OE, COUNT
	}
}
