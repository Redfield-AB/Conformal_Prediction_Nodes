package se.redfield.cp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
	private Calibrator calibrator;

	public Predictor(ConformalPredictorNodeModel model, Calibrator calibrator) {
		this.model = model;
		this.calibrator = calibrator;
	}

	public DataTableSpec createOuputTableSpec(DataTableSpec inPredictionTableSpecs) {
		List<DataColumnSpec> colls = new ArrayList<>();
		for (int i = 0; i < inPredictionTableSpecs.getNumColumns(); i++) {
			colls.add(inPredictionTableSpecs.getColumnSpec(i));
		}

		Set<DataCell> values = inPredictionTableSpecs.getColumnSpec(model.getSelectedColumnName()).getDomain()
				.getValues();
		for (DataCell v : values) {
			colls.addAll(Arrays.asList(createScoreColumnsSpecs(v.toString())));
		}
		return new DataTableSpec(colls.toArray(new DataColumnSpec[] {}));
	}

	private DataColumnSpec[] createScoreColumnsSpecs(String value) {
		DataColumnSpec indexCol = new DataColumnSpecCreator(String.format(model.getPredictionRankColumnFormat(), value),
				LongCell.TYPE).createSpec();
		DataColumnSpec scoreCol = new DataColumnSpecCreator(
				String.format(model.getPredictionScoreColumnFormat(), value), DoubleCell.TYPE).createSpec();
		return new DataColumnSpec[] { indexCol, scoreCol };
	}

	public BufferedDataTable process(BufferedDataTable inPredictionTable, ExecutionContext exec)
			throws CanceledExecutionException {
		Set<DataCell> values = inPredictionTable.getDataTableSpec().getColumnSpec(model.getSelectedColumnName())
				.getDomain().getValues();

		BufferedDataTable cur = inPredictionTable;
		for (DataCell v : values) {
			String val = v.toString();
			String pColumn = model.getProbabilityColumnName(val);

			BufferedDataTableSorter sorter = new BufferedDataTableSorter(cur, Arrays.asList(pColumn),
					new boolean[] { false });
			BufferedDataTable sorted = sorter.sort(exec);

			ColumnRearranger r = createRearranger(sorted.getDataTableSpec(), val,
					calibrator.getCalibrationProbabilities().get(val));
			cur = exec.createColumnRearrangeTable(sorted, r, exec);
		}
		return cur;
	}

	private ColumnRearranger createRearranger(DataTableSpec spec, String value, List<Double> probabilities) {
		ColumnRearranger r = new ColumnRearranger(spec);
		int pColumnIndex = spec.findColumnIndex(model.getProbabilityColumnName(value));
		r.append(new ScoreCellFactory(value, pColumnIndex, probabilities));
		return r;
	}

	private class ScoreCellFactory extends AbstractCellFactory {

		private int pColumnIndex;
		private List<Double> probabilities;
		private int currentIndex;

		public ScoreCellFactory(String value, int pColumnIndex, List<Double> probabilities) {
			super(createScoreColumnsSpecs(value));
			this.pColumnIndex = pColumnIndex;
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
