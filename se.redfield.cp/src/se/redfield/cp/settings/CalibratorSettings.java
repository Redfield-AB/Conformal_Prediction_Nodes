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
 * Calibrator configuration.
 * 
 * @author Alexander Bondaletov
 *
 */
public interface CalibratorSettings {
	/**
	 * The P column default name
	 */
	public static final String CALIBRATION_P_COLUMN_DEFAULT_NAME = "P";
	/**
	 * The Rank column default name
	 */
	public static final String CALIBRATION_RANK_COLUMN_DEFAULT_NAME = "Rank";

	/**
	 * @return The target settings.
	 */
	public TargetSettings getTargetSettings();

	/**
	 * @return The keep column settings
	 */
	public KeepColumnsSettings getKeepColumns();

	/**
	 * @return The P column name.
	 */
	public default String getCalibrationProbabilityColumnName() {
		return CALIBRATION_P_COLUMN_DEFAULT_NAME;
	}

	/**
	 * @return The Rank column name
	 */
	public default String getCalibrationRankColumnName() {
		return CALIBRATION_RANK_COLUMN_DEFAULT_NAME;
	}
}
