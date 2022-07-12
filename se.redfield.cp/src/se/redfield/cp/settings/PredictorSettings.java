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
 * Predictor configuration.
 * 
 * @author Alexander Bondaletov
 *
 */
public interface PredictorSettings {
	/**
	 * The Rank column default format.
	 */
	public static final String PREDICTION_RANK_COLUMN_DEFAULT_FORMAT = "Rank (%s)";
	/**
	 * The p-value column default format.
	 */
	public static final String PREDICTION_P_VALUE_COLUMN_DEFAULT_FORMAT = "p-value (%s)";

	/**
	 * @return The target settings.
	 */
	public TargetSettings getTargetSettings();

	/**
	 * @return The keep columns settings.
	 */
	public KeepColumnsSettings getKeepColumns();

	/**
	 * @return Whether to include rank column
	 */
	public boolean getIncludeRankColumn();

	/**
	 * @return The probability column name from the calibration table.
	 */
	public default String getCalibrationProbabilityColumnName() {
		return CalibratorSettings.CALIBRATION_P_COLUMN_DEFAULT_NAME;
	}

	/**
	 * @return The Rank column format.
	 */
	public default String getPredictionRankColumnFormat() {
		return PREDICTION_RANK_COLUMN_DEFAULT_FORMAT;
	}

	/**
	 * @return The p-value column format.
	 */
	public default String getPredictionPValueColumnFormat() {
		return PREDICTION_P_VALUE_COLUMN_DEFAULT_FORMAT;
	}
}
