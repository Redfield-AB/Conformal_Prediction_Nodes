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

/**
 * Predictor(regression) configuration.
 * 
 * @author Alexander Bondaletov
 *
 */
public interface PredictorRegressionSettings {
	/**
	 * The lower bound column default name.
	 */
	public static final String PREDICTION_LOWER_COLUMN_DEFAULT_NAME = "Lower bound";
	/**
	 * The upper bound column default name.
	 */
	public static final String PREDICTION_UPPER_COLUMN_DEFAULT_NAME = "Upper bound";

	/**
	 * @return The prediction column name.
	 */
	public String getPredictionColumnName();

	/**
	 * @return The regression settings.
	 */
	public RegressionSettings getRegressionSettings();

	/**
	 * @return The keep columns settings.
	 */
	public KeepColumnsSettings getKeepColumns();

	/**
	 * @return The error rate.
	 */
	public double getErrorRate();

	/**
	 * @return The Alpha column name.
	 */
	default String getCalibrationAlphaColumnName() {
		return CalibratorRegressionSettings.CALIBRATION_ALHPA_COLUMN_DEFAULT_NAME;
	}

	/**
	 * @return The Rank column name.
	 */
	default String getCalibrationRankColumnName() {
		return CalibratorSettings.CALIBRATION_RANK_COLUMN_DEFAULT_NAME;
	}

	/**
	 * @return The lower bound column name.
	 */
	default String getLowerBoundColumnName() {
		return PREDICTION_LOWER_COLUMN_DEFAULT_NAME;
	}

	/**
	 * @return The upper bound column name.
	 */
	default String getUpperBoundColumnName() {
		return PREDICTION_UPPER_COLUMN_DEFAULT_NAME;
	}
}
