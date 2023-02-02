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
package se.redfield.cp.core.calibration;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;

import se.redfield.cp.settings.CalibratorRegressionSettings;
import se.redfield.cp.utils.KnimeUtils;

/**
 * Class used by Conformal Calibrator Node to process input table into output
 * calibration table.
 *
 */
public class CalibratorPredictiveSystems extends CalibratorRegression {

	/**
	 * Creates instance
	 * 
	 * @param settings
	 */
	public CalibratorPredictiveSystems(CalibratorRegressionSettings settings) {
		super(settings);
	}

	/**
	 * Creates cell factory that appends the nonconformity column to input table.
	 * 
	 * @param inputTableSpec Input table spec.
	 * @return
	 */
	private CellFactory createNonconformityCellFactory(DataTableSpec inputTableSpec) {
		int targetColumnIndex = inputTableSpec.findColumnIndex(settings.getTargetColumnName());
		int predictionColumnIndex = inputTableSpec.findColumnIndex(settings.getPredictionColumnName());
		int sigmaColumnIndex = inputTableSpec.findColumnIndex(settings.getRegressionSettings().getSigmaColumn());

		return new AbstractCellFactory(
				new DataColumnSpecCreator(settings.getCalibrationAlphaColumnName(), DoubleCell.TYPE).createSpec()) {

			@Override
			public DataCell[] getCells(DataRow row) {
				double dTarget = KnimeUtils.getDouble(row.getCell(targetColumnIndex),
						"Target column contains missing values");
				double dPrediction = KnimeUtils.getDouble(row.getCell(predictionColumnIndex),
						"Prediction column contains missing values");

				double nonconformityScore = 0;
				if (settings.getRegressionSettings().getNormalized()) {
					double dSigma = KnimeUtils.getDouble(row.getCell(sigmaColumnIndex),
							"Sigma column contains missing values");

					nonconformityScore = (dTarget - dPrediction)
								/ (dSigma + settings.getRegressionSettings().getBeta());
				} else {
					nonconformityScore = (dTarget - dPrediction);
				}

				return new DataCell[] { new DoubleCell(nonconformityScore) };
			}
		};
	}
}
