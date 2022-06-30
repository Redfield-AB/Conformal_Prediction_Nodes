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

public interface CalibratorSettings {
	public static final String CALIBRATION_P_COLUMN_DEFAULT_NAME = "P";
	public static final String CALIBRATION_RANK_COLUMN_DEFAULT_NAME = "Rank";

	public TargetSettings getTargetSettings();

	public KeepColumnsSettings getKeepColumns();

	public default String getCalibrationProbabilityColumnName() {
		return CALIBRATION_P_COLUMN_DEFAULT_NAME;
	}

	public default String getCalibrationRankColumnName() {
		return CALIBRATION_RANK_COLUMN_DEFAULT_NAME;
	}
}
