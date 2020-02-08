package se.redfield.cp;

import java.util.ArrayList;
import java.util.Collections;
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
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;

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

	public ColumnRearranger createRearranger(DataTableSpec predictionTableSpec, BufferedDataTable inCalibrationTable,
			ExecutionContext exec) throws CanceledExecutionException {
		Map<String, List<Double>> calibrationProbabilities = collectCalibrationProbabilities(inCalibrationTable, exec);
		Set<DataCell> values = predictionTableSpec.getColumnSpec(model.getSelectedColumnName()).getDomain().getValues();

		ColumnRearranger r = new ColumnRearranger(predictionTableSpec);

		if (!model.getKeepAllColumns()) {
			r.keepOnly(model.getRequiredColumnNames(predictionTableSpec));
		}

		for (DataCell v : values) {
			String val = v.toString();
			r.append(new ScoreCellFactory(val, predictionTableSpec, calibrationProbabilities.get(val)));
		}
		return r;
	}

	private Map<String, List<Double>> collectCalibrationProbabilities(BufferedDataTable inCalibrationTable,
			ExecutionContext exec) throws CanceledExecutionException {
		Map<String, List<Double>> result = new HashMap<>();
		int valIndex = inCalibrationTable.getDataTableSpec().findColumnIndex(model.getSelectedColumnName());
		int probIndex = inCalibrationTable.getDataTableSpec()
				.findColumnIndex(model.getCalibrationProbabilityColumnName());

		long rowCount = inCalibrationTable.size();
		long index = 0;
		ExecutionMonitor progress = exec.createSubProgress(0.5);

		for (DataRow row : inCalibrationTable) {
			String val = row.getCell(valIndex).toString();
			double probability = ((DoubleValue) row.getCell(probIndex)).getDoubleValue();

			List<Double> probabilities = result.computeIfAbsent(val, key -> new ArrayList<>());
			probabilities.add(probability);

			exec.checkCanceled();
			progress.setProgress((double) index++ / rowCount);
		}

		index = 0;
		progress = exec.createSubProgress(0.5);

		for (List<Double> p : result.values()) {
			Collections.sort(p, Collections.reverseOrder());

			exec.checkCanceled();
			progress.setProgress((double) index++ / result.size());
		}

		return result;
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

		public ScoreCellFactory(String value, DataTableSpec inSpec, List<Double> probabilities) {
			super(createScoreColumnsSpecs(value));
			this.pColumnIndex = inSpec.findColumnIndex(model.getProbabilityColumnName(value));
			this.probabilities = probabilities;
		}

		@Override
		public DataCell[] getCells(DataRow row) {
			double p = ((DoubleValue) row.getCell(pColumnIndex)).getDoubleValue();
			int rank = getRank(p);
			double score = ((double) probabilities.size() - rank) / (probabilities.size() + 1);
			return new DataCell[] { new LongCell(rank), new DoubleCell(score) };
		}

		protected int getRank(double p) {
			int idx = Collections.binarySearch(probabilities, p, Collections.reverseOrder());
			if (idx < 0) {// insertion index
				return -(idx + 1);
			} else {// exact match found
				// finding the first occurrence
				while (idx >= 0 && probabilities.get(idx) == p) {
					idx -= 1;
				}
				return idx + 1;
			}
		}
	}

}
