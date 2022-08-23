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

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;

import se.redfield.cp.settings.CalibratorNodeSettings;
import se.redfield.cp.settings.ui.DialogComponentProbabilityFormat;

/**
 * Node Dialog for Calibrator node.
 *
 */
public class ConformalPredictorCalibratorNodeDialog extends DefaultNodeSettingsPane {

	private final CalibratorNodeSettings settings = new CalibratorNodeSettings();

	/**
	 * Creates new instance
	 */
	@SuppressWarnings("unchecked")
	public ConformalPredictorCalibratorNodeDialog() {
		super();

		addDialogComponent(new DialogComponentColumnNameSelection(settings.getTargetSettings().getTargetColumnModel(),
				"Target column:", ConformalPredictorCalibratorNodeModel.PORT_INPUT_TABLE.getIdx(), DataValue.class));
		addDialogComponent(new DialogComponentProbabilityFormat(settings.getTargetSettings()));

		createNewGroup("Define output");

		addDialogComponent(
				new DialogComponentBoolean(settings.getKeepColumns().getKeepAllColumnsModel(), "Keep All Columns"));
		addDialogComponent(
				new DialogComponentBoolean(settings.getKeepColumns().getKeepIdColumnModel(), "Keep ID column"));
		addDialogComponent(new DialogComponentColumnNameSelection(settings.getKeepColumns().getIdColumnModel(),
				"ID column:", ConformalPredictorCalibratorNodeModel.PORT_INPUT_TABLE.getIdx(), DataValue.class));

	}

}
