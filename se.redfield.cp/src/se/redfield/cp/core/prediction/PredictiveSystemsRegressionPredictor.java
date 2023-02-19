/*
 * Copyright (c) 2022 Redfield AB.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import se.redfield.cp.settings.PredictiveSystemsRegressionSettings;
import se.redfield.cp.utils.KnimeUtils;

public class PredictiveSystemsRegressionPredictor {

	private final PredictiveSystemsRegressionSettings settings;

	/**
	 * @param settings The predictor settings.
	 */
	public PredictiveSystemsRegressionPredictor(PredictiveSystemsRegressionSettings settings) {
		this.settings = settings;
	}

	/**
	 * Creates output table spec.
	 * 
	 * @param inPredictionTableSpecs Input prediction table spec.
	 * @return The output prediction table spec.
	 */
	public DataTableSpec createOuputTableSpec(DataTableSpec inPredictionTableSpecs) {
		ColumnRearranger r = createRearranger(inPredictionTableSpecs, new ArrayList<Double>());
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
		List<Double> alphas = getAlpha(inCalibrationTable, exec);

		return createRearranger(predictionTableSpec, alphas);
	}

	private ColumnRearranger createRearranger(DataTableSpec predictionTableSpec, List<Double> alpha) {
		ColumnRearranger r = new ColumnRearranger(predictionTableSpec);

		if (!settings.getKeepColumns().getKeepAllColumns()) {
			r.keepOnly(getRequiredColumnNames());
		}

		r.append(createDistributionCellFactory(predictionTableSpec, alpha));

		return r;
	}

	private CellFactory createDistributionCellFactory(DataTableSpec inputTableSpec, List<Double> alphas) {
		int predictionColumnIndex = inputTableSpec.findColumnIndex(settings.getPredictionColumnName());
		int sigmaColumnIndex = inputTableSpec.findColumnIndex(settings.getRegressionSettings().getSigmaColumn());

		return new AbstractCellFactory(new DataColumnSpecCreator(settings.getDistributionColumnName(),
				ListCell.getCollectionType(DoubleCell.TYPE)).createSpec()) {

			@Override
			public DataCell[] getCells(DataRow row) {
				double dPrediction = KnimeUtils.getDouble(row.getCell(predictionColumnIndex),
						"Prediction column contains missing values");
				double[] probabilities = new double[alphas.size()];
				if (settings.getRegressionSettings().getNormalized()) {
					double dSigma = KnimeUtils.getDouble(row.getCell(sigmaColumnIndex),
							"Sigma column contains missing values");
					for (int i = 0; i < alphas.size(); i++) {
						probabilities[i] = dPrediction
								+ (alphas.get(i) * (dSigma + settings.getRegressionSettings().getBeta()));
					}
				} else {
					for (int i = 0; i < alphas.size(); i++) {
						probabilities[i] = dPrediction + alphas.get(i);
					}
				}

				ListCell cell = CollectionCellFactory.createListCell(
						Arrays.stream(probabilities).mapToObj(DoubleCell::new).collect(Collectors.toList()));
				return new DataCell[] { cell };
			}
		};
	}

	/**
	 * @param inCalibrationTable the calibration table.
	 * @return the alpha from the (significant level)'th percentile among the
	 *         calibration instances.
	 * @throws CanceledExecutionException
	 */
	private List<Double> getAlpha(BufferedDataTable inCalibrationTable, ExecutionContext exec)
			throws CanceledExecutionException {
		int alphaColumnIndex = inCalibrationTable.getDataTableSpec()
				.findColumnIndex(settings.getCalibrationAlphaColumnName()); // get target column
		// double errorRate = settings.getErrorRate();

		List<Double> alphas = new ArrayList<>();
		CloseableRowIterator rowIterator = inCalibrationTable.iterator();
		while (rowIterator.hasNext()) {
			DataRow currentRow = rowIterator.next();
			DataCell cell = currentRow.getCell(alphaColumnIndex);
			alphas.add(((DoubleValue) cell).getDoubleValue());

			exec.checkCanceled();
			exec.setProgress((double) alphas.size() / inCalibrationTable.size());
		}
		// int index = (int) (alphas.size() * errorRate);
		Collections.sort(alphas);

		return alphas;
	}

	private String[] getRequiredColumnNames() {
		Set<String> columns = new HashSet<>();
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
