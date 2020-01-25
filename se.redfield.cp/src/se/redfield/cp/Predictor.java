package se.redfield.cp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import se.redfield.cp.nodes.ConformalPredictorNodeModel;

public class Predictor {

	private ConformalPredictorNodeModel model;

	public Predictor(ConformalPredictorNodeModel model) {
		this.model = model;
	}

	public DataTableSpec createOuputTableSpec(DataTableSpec inPredictionTableSpecs) {
		ColumnRearranger r = new ColumnRearranger(inPredictionTableSpecs);
		if (!model.getKeepAllColumns()) {
			r.keepOnly(model.getRequiredColumnNames(inPredictionTableSpecs));
		}

		Set<DataCell> values = inPredictionTableSpecs.getColumnSpec(model.getSelectedColumnName()).getDomain()
				.getValues();
		for (DataCell v : values) {
			r.append(new ScoreCellFactory(v.toString(), inPredictionTableSpecs, null));
		}

		return r.createSpec();
	}

	public BufferedDataTable process(BufferedDataTable inPredictionTable, BufferedDataTable inCalibrationTable,
			ExecutionContext exec) throws CanceledExecutionException {
		Map<String, List<Double>> calibrationProbabilities = collectCalibrationProbabilities(inCalibrationTable, exec);
		Set<DataCell> values = inPredictionTable.getDataTableSpec().getColumnSpec(model.getSelectedColumnName())
				.getDomain().getValues();

		BufferedDataTable cur = inPredictionTable;

		if (!model.getKeepAllColumns()) {
			cur = stripPredictionTable(inPredictionTable, exec);
		}

		for (DataCell v : values) {
			String val = v.toString();
			String pColumn = model.getProbabilityColumnName(val);

			BufferedDataTableSorter sorter = new BufferedDataTableSorter(cur, Arrays.asList(pColumn),
					new boolean[] { false });
			BufferedDataTable sorted = sorter.sort(exec);

			ColumnRearranger r = createRearranger(sorted.getDataTableSpec(), val, calibrationProbabilities.get(val));
			cur = exec.createColumnRearrangeTable(sorted, r, exec);
		}
		return cur;
	}

	private Map<String, List<Double>> collectCalibrationProbabilities(BufferedDataTable inCalibrationTable,
			ExecutionContext exec) throws CanceledExecutionException {
		BufferedDataTableSorter sorter = new BufferedDataTableSorter(inCalibrationTable,
				Arrays.asList(model.getSelectedColumnName(), model.getCalibrationProbabilityColumnName()),
				new boolean[] { true, false });
		BufferedDataTable sortedTable = sorter.sort(exec);

		Map<String, List<Double>> result = new HashMap<>();
		String prevValue = null;
		List<Double> curProbabilities = null;
		int columnIndex = sortedTable.getDataTableSpec().findColumnIndex(model.getSelectedColumnName());
		int probabilityIndex = sortedTable.getDataTableSpec()
				.findColumnIndex(model.getCalibrationProbabilityColumnName());

		for (DataRow row : sortedTable) {
			String value = row.getCell(columnIndex).toString();
			if (prevValue == null || !prevValue.equals(value)) {
				prevValue = value;

				curProbabilities = new ArrayList<>();
				result.put(value, curProbabilities);
			}

			Double probability = ((DoubleValue) row.getCell(probabilityIndex)).getDoubleValue();
			curProbabilities.add(probability);
		}
		return result;
	}

	private BufferedDataTable stripPredictionTable(BufferedDataTable inTable, ExecutionContext exec)
			throws CanceledExecutionException {
		ColumnRearranger r = new ColumnRearranger(inTable.getDataTableSpec());
		r.keepOnly(model.getRequiredColumnNames(inTable.getDataTableSpec()));
		return exec.createColumnRearrangeTable(inTable, r, exec);
	}

	private ColumnRearranger createRearranger(DataTableSpec spec, String value, List<Double> probabilities) {
		ColumnRearranger r = new ColumnRearranger(spec);
		r.append(new ScoreCellFactory(value, spec, probabilities));
		return r;
	}

	private DataColumnSpec[] createScoreColumnsSpecs(String value) {
		DataColumnSpec indexCol = new DataColumnSpecCreator(String.format(model.getPredictionRankColumnFormat(), value),
				LongCell.TYPE).createSpec();
		DataColumnSpec scoreCol = new DataColumnSpecCreator(
				String.format(model.getPredictionScoreColumnFormat(), value), DoubleCell.TYPE).createSpec();
		return new DataColumnSpec[] { indexCol, scoreCol };
	}

	private class ScoreCellFactory extends AbstractCellFactory {

		private int pColumnIndex;
		private List<Double> probabilities;
		private int currentIndex;

		public ScoreCellFactory(String value, DataTableSpec inSpec, List<Double> probabilities) {
			super(createScoreColumnsSpecs(value));
			this.pColumnIndex = inSpec.findColumnIndex(model.getProbabilityColumnName(value));
			this.probabilities = probabilities;
			this.currentIndex = 0;
		}

		@Override
		public DataCell[] getCells(DataRow row) {
			double p = ((DoubleValue) row.getCell(pColumnIndex)).getDoubleValue();
			int rank = getRank(p);
			double score = ((double) probabilities.size() - rank) / (probabilities.size() + 1);
			return new DataCell[] { new LongCell(rank), new DoubleCell(score) };
		}

		private int getRank(double p) {
			while (currentIndex < probabilities.size() && probabilities.get(currentIndex) >= p) {
				currentIndex++;
			}
			return currentIndex > 0 ? currentIndex - 1 : currentIndex;
		}
	}
}
