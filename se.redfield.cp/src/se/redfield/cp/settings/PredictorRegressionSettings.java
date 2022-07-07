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
package se.redfield.cp.settings;

public interface PredictorRegressionSettings {
	public static final String PREDICTION_LOWER_COLUMN_DEFAULT_NAME = "Lower bound";
	public static final String PREDICTION_UPPER_COLUMN_DEFAULT_NAME = "Upper bound";

	public String getPredictionColumnName();

	public RegressionSettings getRegressionSettings();

	public KeepColumnsSettings getKeepColumns();

	public double getErrorRate();

	default String getCalibrationAlphaColumnName() {
		return CalibratorRegressionSettings.CALIBRATION_ALHPA_COLUMN_DEFAULT_NAME;
	}

	default String getCalibrationRankColumnName() {
		return CalibratorSettings.CALIBRATION_RANK_COLUMN_DEFAULT_NAME;
	}

	default String getLowerBoundColumnName() {
		return PREDICTION_LOWER_COLUMN_DEFAULT_NAME;
	}

	default String getUpperBoundColumnName() {
		return PREDICTION_UPPER_COLUMN_DEFAULT_NAME;
	}
}
