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
package se.redfield.cp.core.prediction;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import se.redfield.cp.settings.PredictorRegressionSettings;
import se.redfield.cp.utils.KnimeUtils;

/**
 * Predictor for the regression case. Computes upper and lower bounds for the
 * regression value based on the calibration table.
 *
 */
public class PredictorRegression {

	private PredictorRegressionSettings settings;

	/**
	 * @param settings The settings.
	 */
	public PredictorRegression(PredictorRegressionSettings settings) {
		this.settings = settings;
	}

	/**
	 * Creates output table spec.
	 * 
	 * @param inPredictionTableSpecs Input prediction table spec.
	 * @return The output prediction table spec.
	 */
	public DataTableSpec createOuputTableSpec(DataTableSpec inPredictionTableSpecs) {
		ColumnRearranger r = createRearranger(inPredictionTableSpecs, 0);
		return r.createSpec();
	}

	/**
	 * Creates column rearranger that is used to process input prediction table.
	 * 
	 * @param predictionTableSpec Input prediction table spec.
	 * @param inCalibrationTable  Input calibration table.
	 * @param exec                Execution context.
	 * @return The rearranger.
	 * @throws CanceledExecutionException
	 */
	public ColumnRearranger createRearranger(DataTableSpec predictionTableSpec, BufferedDataTable inCalibrationTable,
			ExecutionContext exec) throws CanceledExecutionException {
		double alpha = getAlpha(inCalibrationTable, exec);

		return createRearranger(predictionTableSpec, alpha);
	}

	private ColumnRearranger createRearranger(DataTableSpec predictionTableSpec, double alpha) {
		ColumnRearranger r = new ColumnRearranger(predictionTableSpec);

		if (!settings.getKeepColumns().getKeepAllColumns()) {
			r.keepOnly(getRequiredColumnNames());
		}

		r.append(createPredictionLowerBoundCellFactory(predictionTableSpec, alpha));
		r.append(createPredictionUpperBoundCellFactory(predictionTableSpec, alpha));

		return r;
	}

	private CellFactory createPredictionLowerBoundCellFactory(DataTableSpec inputTableSpec, double alpha) {
		return createPredictionIntervalCellFactory(inputTableSpec, alpha, true);
	}

	private CellFactory createPredictionUpperBoundCellFactory(DataTableSpec inputTableSpec, double alpha) {
		return createPredictionIntervalCellFactory(inputTableSpec, alpha, false);
	}

	/**
	 * Creates cell factory that appends the nonconformity column to input table.
	 * 
	 * @param inputTableSpec Input table spec.
	 * @return
	 */
	private CellFactory createPredictionIntervalCellFactory(DataTableSpec inputTableSpec, double alpha,
			boolean isLower) {
		int predictionColumnIndex = inputTableSpec.findColumnIndex(settings.getPredictionColumnName());
		int sigmaColumnIndex = inputTableSpec.findColumnIndex(settings.getRegressionSettings().getSigmaColumn());

		String columnName;
		if (isLower) {
			columnName = settings.getLowerBoundColumnName();
		} else {
			columnName = settings.getUpperBoundColumnName();
		}

		return new AbstractCellFactory(new DataColumnSpecCreator(columnName, DoubleCell.TYPE).createSpec()) {

			@Override
			public DataCell[] getCells(DataRow row) {
				double dPrediction = KnimeUtils.getDouble(row.getCell(predictionColumnIndex),
						"Prediction column contains missing values");

				double bound;

				if (settings.getRegressionSettings().getNormalized()) {
					double dSigma = KnimeUtils.getDouble(row.getCell(sigmaColumnIndex),
							"Sigma column contains missing values");

					if (isLower)
						bound = dPrediction - alpha * (dSigma + settings.getRegressionSettings().getBeta());
					else
						bound = dPrediction + alpha * (dSigma + settings.getRegressionSettings().getBeta());
				} else {
					if (isLower)
						bound = dPrediction - alpha;
					else
						bound = dPrediction + alpha;
				}
				return new DataCell[] { new DoubleCell(bound) };
			}
		};
	}

	/**
	 * @param inCalibrationTable the calibration table.
	 * @return the alpha from the (significant level)'th percentile among the
	 *         calibration instances.
	 * @throws CanceledExecutionException
	 */
	private double getAlpha(BufferedDataTable inCalibrationTable, ExecutionContext exec)
			throws CanceledExecutionException {
		int alphaColumnIndex = inCalibrationTable.getDataTableSpec()
				.findColumnIndex(settings.getCalibrationAlphaColumnName()); // get target column
		double errorRate = settings.getErrorRate();

		List<Double> alphas = new ArrayList<>();
		CloseableRowIterator rowIterator = inCalibrationTable.iterator();
		while (rowIterator.hasNext()) {
			DataRow currentRow = rowIterator.next();
			DataCell cell = currentRow.getCell(alphaColumnIndex);
			alphas.add(((DoubleValue) cell).getDoubleValue());

			exec.checkCanceled();
			exec.setProgress((double) alphas.size() / inCalibrationTable.size());
		}
		int index = (int) (alphas.size() * errorRate);

		return alphas.get(index);
	}

	private String[] getRequiredColumnNames() {
		List<String> columns = new ArrayList<>();
		columns.add(settings.getPredictionColumnName());
		if (settings.getRegressionSettings().getNormalized()) {
			columns.add(settings.getRegressionSettings().getSigmaColumn());
		}
		if (settings.getKeepColumns().getKeepIdColumn()) {
			columns.add(settings.getKeepColumns().getIdColumn());
		}
		return columns.toArray(new String[] {});
	}
}
