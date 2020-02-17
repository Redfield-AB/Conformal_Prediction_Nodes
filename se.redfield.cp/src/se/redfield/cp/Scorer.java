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
		}
		specs.add(new DataColumnSpecCreator("Accuracy (strict)", DoubleCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator("Accuracy (soft)", DoubleCell.TYPE).createSpec());
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
	 * <li>Accuracy (strict) = Exact_match/(Exact_match + Error)</li>
	 * <li>Accuracy (soft) = Total_match/Total</li>
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

			score.incTotal();

			if (classes.contains(target)) {
				if (classes.size() == 1) {
					score.incTpExclusive();
				} else {
					score.incTpInclusive();
				}
			} else {
				score.incFn();
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
			cells.add(new LongCell(score.getTpExclusive()));
			cells.add(new LongCell(score.getTpInclusive()));
			cells.add(new LongCell(score.getTotalTp()));
			cells.add(new LongCell(score.getFn()));
			cells.add(new LongCell(score.getTotal()));
		}
		cells.add(new DoubleCell(score.getAccuracySimple()));
		cells.add(new DoubleCell(score.getAccuracyAdvanced()));
		return new DefaultRow(RowKey.createRowKey(idx), cells);
	}

	/**
	 * Collects predicted classes as a string set from Classes cell.
	 * 
	 * @param cell Classes cell.
	 * @return
	 */
	private Set<String> getClasses(DataCell cell) {
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
		private long total;
		private long tpExclusive;
		private long tpInclusive;
		private long fn;

		public ClassScores(String target) {
			this.target = target;
		}

		public String getTarget() {
			return target;
		}

		public void incTotal() {
			total++;
		}

		public void incTpExclusive() {
			tpExclusive++;
		}

		public void incTpInclusive() {
			tpInclusive++;
		}

		public void incFn() {
			fn++;
		}

		public long getTotal() {
			return total;
		}

		public long getTpExclusive() {
			return tpExclusive;
		}

		public long getTpInclusive() {
			return tpInclusive;
		}

		public long getFn() {
			return fn;
		}

		public long getTotalTp() {
			return tpExclusive + tpInclusive;
		}

		public double getAccuracySimple() {
			return (double) tpExclusive / (tpExclusive + fn);
		}

		public double getAccuracyAdvanced() {
			return (double) getTotalTp() / total;
		}
	}
}
