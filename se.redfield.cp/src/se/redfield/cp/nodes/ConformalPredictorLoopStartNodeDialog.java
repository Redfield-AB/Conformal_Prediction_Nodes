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
package se.redfield.cp.nodes;

import org.knime.core.data.IntValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class ConformalPredictorLoopStartNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public ConformalPredictorLoopStartNodeDialog() {
		super();

		SettingsModelString modelIterationColumnSettings = ConformalPredictorLoopStartNodeModel
				.createModelIterationColumnSettings();
		SettingsModelString calibrationIterationColumnSettings = ConformalPredictorLoopStartNodeModel
				.createCalibrationIterationColumnSettings();

		createNewGroup("Model iteration columns");
		addDialogComponent(new DialogComponentColumnNameSelection(modelIterationColumnSettings, "",
				ConformalPredictorLoopStartNodeModel.PORT_MODEL_TABLE, IntValue.class));
		createNewGroup("Calibration iteration columns");
		addDialogComponent(new DialogComponentColumnNameSelection(calibrationIterationColumnSettings, "",
				ConformalPredictorLoopStartNodeModel.PORT_CALIBRATION_TABLE, IntValue.class));
	}
}
