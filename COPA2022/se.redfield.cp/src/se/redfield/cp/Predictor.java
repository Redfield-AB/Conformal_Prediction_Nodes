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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

import se.redfield.cp.nodes.AbstractConformalPredictorNodeModel;

/**
 * Class used by Conformal Predictor node to process input table and calculate
 * Rank and P-value for each row based on the calibration table probabilities.
 *
 */
public class Predictor {

	private AbstractConformalPredictorNodeModel model;

	public Predictor(AbstractConformalPredictorNodeModel model) {
		this.model = model;
	}

	/**
	 * Creates output table spec.
	 * 
	 * @param inPredictionTableSpecs Input prediction table spec.
	 * @return
	 */
	public DataTableSpec createOuputTableSpec(DataTableSpec inPredictionTableSpecs) {
		ColumnRearranger r = new ColumnRearranger(inPredictionTableSpecs);
		if (!model.getKeepAllColumns()) {
			r.keepOnly(model.getRequiredColumnNames(inPredictionTableSpecs));
		}

		Set<DataCell> values = inPredictionTableSpecs.getColumnSpec(model.getTargetColumnName()).getDomain()
				.getValues();
		for (DataCell v : values) {
			r.append(new ScoreCellFactory(v.toString(), inPredictionTableSpecs, null));
		}

		return r.createSpec();
	}

	/**
	 * Creates column rearranger that is used to process input prediction table.
	 * 
	 * @param predictionTableSpec Input prediction table spec.
	 * @param inCalibrationTable  Input calibration table.
	 * @param exec                Execution context.
	 * @return
	 * @throws CanceledExecutionException
	 */
	public ColumnRearranger createRearranger(DataTableSpec predictionTableSpec, BufferedDataTable inCalibrationTable,
			ExecutionContext exec) throws CanceledExecutionException {
		Map<String, List<Double>> calibrationProbabilities = collectCalibrationProbabilities(inCalibrationTable, exec);
		Set<DataCell> values = predictionTableSpec.getColumnSpec(model.getTargetColumnName()).getDomain().getValues();

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

	/**
	 * Collects probabilities from the calibration table. Collected probabilities
	 * grouped by target and sorted in desc order.
	 * 
	 * @param inCalibrationTable
	 * @param exec
	 * @return
	 * @throws CanceledExecutionException
	 */
	private Map<String, List<Double>> collectCalibrationProbabilities(BufferedDataTable inCalibrationTable,
			ExecutionContext exec) throws CanceledExecutionException {
		Map<String, List<Double>> result = new HashMap<>();
		int valIndex = inCalibrationTable.getDataTableSpec().findColumnIndex(model.getTargetColumnName());
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

	/**
	 * Creates score columns specs consist of Rank column (if option enabled) and
	 * P-value column.
	 * 
	 * @param value
	 * @return
	 */
	private DataColumnSpec[] createScoreColumnsSpecs(String value) {
		List<DataColumnSpec> columns = new ArrayList<>();

		if (model.getIncludeRankColumn()) {
			columns.add(new DataColumnSpecCreator(String.format(model.getPredictionRankColumnFormat(), value),
					LongCell.TYPE).createSpec());
		}
		columns.add(
				new DataColumnSpecCreator(String.format(model.getPredictionScoreColumnFormat(), value), DoubleCell.TYPE)
						.createSpec());

		return columns.toArray(new DataColumnSpec[] {});
	}

	/**
	 * Cell factory used to append P-value and optional Rank columns
	 *
	 */
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
			int[] ranks = getRanks(p);
			//Does not take into consideration that many probabilities can be equal to p
			// TODO adjust the calculation to calculate the exact p value
			// FIXED
			Random rand = new Random();
			double score = (((double) probabilities.size() - ranks[1]) + rand.nextDouble() * (((double) ranks[1] - ranks[0])))
					/ (probabilities.size() + 1);
			DoubleCell scoreCell = new DoubleCell(score);

			if (model.getIncludeRankColumn()) {
				return new DataCell[] { new LongCell(rank), scoreCell };
			} else {
				return new DataCell[] { scoreCell };
			}
		}

		/**
		 * Calculated the rank for a given probability. Rank is the position probability
		 * would take in a sorted list of probabilities from the calibration table.
		 * 
		 * @param p Probability.
		 * @return Rank.
		 */
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

		/**
		 * Calculated the rank for a given probability as well as the rank for the nearest smaller value. 
		 * Rank is the position probability would take in a sorted list of probabilities from the calibration table.
		 * 
		 * @param p Probability.
		 * @return [Rank, smaller rank].
		 */
		protected int[] getRanks(double p) {
			int[] idxs = new int[2];
			idxs[0] = getRank(p);
			int idx = idxs[0];
			while (idx < probabilities.size() && probabilities.get(idx) == p) {
				idx += 1;
			}
			idxs[1] = idx - 1;
			return idxs;	
		}
	}

}
