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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
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

	public Scorer(ConformalPredictorScorerNodeModel model) {
		this.model = model;
	}

	/**
	 * Creates output table spec.
	 */
	public DataTableSpec createOutputSpec() {
		List<DataColumnSpec> specs = new ArrayList<>();
		specs.add(new DataColumnSpecCreator("Target", StringCell.TYPE).createSpec());
		if (model.isAdditionalInfoMode()) {
			specs.add(new DataColumnSpecCreator("Exact match", LongCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Soft match", LongCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Total match", LongCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Error", LongCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Total", LongCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Single class predictions", LongCell.TYPE).createSpec());
			specs.add(new DataColumnSpecCreator("Null predictions", LongCell.TYPE).createSpec());
		}
		specs.add(new DataColumnSpecCreator("Efficiency", DoubleCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator("Validity", DoubleCell.TYPE).createSpec());
		return new DataTableSpec(specs.toArray(new DataColumnSpec[] {}));
	}

	/**
	 * Processes input table. Callects the following metrics for each row: *
	 * <ul>
	 * <li>Exact match – number of correct predictions that belong to one class, and
	 * not belong to any mixed class.</li>
	 * <li>Soft match - number of correct predictions that belong to one of the
	 * mixed classes.</li>
	 * <li>Total match – Exact_match + Soft_match.</li>
	 * <li>Error – number of predictions that does not match real target class.</li>
	 * <li>Total – total number of records that belongs to the current target
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
	public BufferedDataTable process(BufferedDataTable inTable, ExecutionContext exec)
			throws CanceledExecutionException {
		Map<String, ClassScores> scores = new HashMap<>();

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

			exec.checkCanceled();
			exec.setProgress((double) count++ / total);
		}

		return createOutputTable(scores, exec);
	}

	/**
	 * Creates {@link BufferedDataTable} from collected scores.
	 * 
	 * @param scores Collected scores.
	 * @param exec   Execution context.
	 * @return
	 */
	private BufferedDataTable createOutputTable(Map<String, ClassScores> scores, ExecutionContext exec) {
		BufferedDataContainer cont = exec.createDataContainer(createOutputSpec());

		long idx = 0;
		for (ClassScores s : scores.values()) {
			cont.addRowToTable(createRow(s, idx++));
		}

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
	private DataRow createRow(ClassScores score, long idx) {
		List<DataCell> cells = new ArrayList<>();
		cells.add(new StringCell(score.getTarget()));
		if (model.isAdditionalInfoMode()) {
			cells.add(new LongCell(score.get(Metric.STRICT_MATCH)));
			cells.add(new LongCell(score.get(Metric.SOFT_MATCH)));
			cells.add(new LongCell(score.getTotalMatch()));
			cells.add(new LongCell(score.get(Metric.ERROR)));
			cells.add(new LongCell(score.getTotal()));
			cells.add(new LongCell(score.get(Metric.SINGLE_CLASS)));
			cells.add(new LongCell(score.get(Metric.NULL_CLASS)));
		}
		cells.add(new DoubleCell(score.getEfficiency()));
		cells.add(new DoubleCell(score.getValidity()));
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
		private Map<Metric, Long> metrics;

		public ClassScores(String target) {
			this.target = target;
			this.metrics = new EnumMap<>(Metric.class);
		}

		public void inc(Metric m) {
			metrics.compute(m, (k, v) -> v == null ? 1L : v + 1);
		}

		public long get(Metric m) {
			return metrics.getOrDefault(m, 0L);
		}

		public String getTarget() {
			return target;
		}

		public long getTotalMatch() {
			return get(Metric.STRICT_MATCH) + get(Metric.SOFT_MATCH);
		}

		public long getTotal() {
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
		STRICT_MATCH, SOFT_MATCH, ERROR, SINGLE_CLASS, NULL_CLASS
	}
}
