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

public interface PredictorSettings {
	public static final String PREDICTION_RANK_COLUMN_DEFAULT_FORMAT = "Rank (%s)";
	public static final String PREDICTION_SCORE_COLUMN_DEFAULT_FORMAT = "p-value (%s)";

	public TargetSettings getTargetSettings();

	public KeepColumnsSettings getKeepColumns();

	public boolean getIncludeRankColumn();

	public default String getCalibrationProbabilityColumnName() {
		return CalibratorSettings.CALIBRATION_P_COLUMN_DEFAULT_NAME;
	}

	public default String getPredictionRankColumnFormat() {
		return PREDICTION_RANK_COLUMN_DEFAULT_FORMAT;
	}

	public default String getPredictionScoreColumnFormat() {
		return PREDICTION_SCORE_COLUMN_DEFAULT_FORMAT;
	}
}
