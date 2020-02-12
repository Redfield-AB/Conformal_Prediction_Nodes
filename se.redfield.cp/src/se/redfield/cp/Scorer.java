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

public class Scorer {

	private ConformalPredictorScorerNodeModel model;

	public Scorer(ConformalPredictorScorerNodeModel model) {
		this.model = model;
	}

	public DataTableSpec createOutputSpec() {
		List<DataColumnSpec> specs = new ArrayList<>();
		specs.add(new DataColumnSpecCreator("Target", StringCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator("TP_Exclusive", LongCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator("TP_Inclusive", LongCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator("TP_Total", LongCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator("FN", LongCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator("Total", LongCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator("Accuracy (Simple)", DoubleCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator("Accuracy (Advanced)", DoubleCell.TYPE).createSpec());
		return new DataTableSpec(specs.toArray(new DataColumnSpec[] {}));
	}

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

	private BufferedDataTable createOutputTable(Map<String, ClassScores> scores, ExecutionContext exec) {
		BufferedDataContainer cont = exec.createDataContainer(createOutputSpec());

		long idx = 0;
		for (ClassScores s : scores.values()) {
			cont.addRowToTable(createRow(s, idx++));
		}

		cont.close();
		return cont.getTable();
	}

	private DataRow createRow(ClassScores score, long idx) {
		List<DataCell> cells = new ArrayList<>();
		cells.add(new StringCell(score.getTarget()));
		cells.add(new LongCell(score.getTpExclusive()));
		cells.add(new LongCell(score.getTpInclusive()));
		cells.add(new LongCell(score.getTotalTp()));
		cells.add(new LongCell(score.getFn()));
		cells.add(new LongCell(score.getTotal()));
		cells.add(new DoubleCell(score.getAccuracySimple()));
		cells.add(new DoubleCell(score.getAccuracyAdvanced()));
		return new DefaultRow(RowKey.createRowKey(idx), cells);
	}

	private Set<String> getClasses(DataCell cell) {
		if (cell.getType().isCollectionType()) {
			return ((CollectionDataValue) cell).stream().map(DataCell::toString).collect(toSet());
		} else {
			return new HashSet<>(Arrays.asList(cell.toString().split(model.getStringSeparator())));
		}
	}

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
