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
package se.redfield.cp.nodes.ps.regression;

import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;

import se.redfield.cp.settings.CalibratorRegressionNodeSettings;

/**
 * Node Dialog for Calibrator(Regression) node.
 *
 */
public class PredictiveSystemsCalibratorRegressionNodeDialog extends DefaultNodeSettingsPane {

	private final CalibratorRegressionNodeSettings settings = new CalibratorRegressionNodeSettings();

	/**
	 * Creates new instance
	 */
	@SuppressWarnings("unchecked")
	public PredictiveSystemsCalibratorRegressionNodeDialog() {
		super();
		int tableIndex = PredictiveSystemsCalibratorRegressionNodeModel.PORT_INPUT_TABLE.getIdx();

		addDialogComponent(new DialogComponentColumnNameSelection(settings.getTargetColumnModel(), "Target column:",
				tableIndex, DoubleValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(settings.getPredictionColumnModel(),
				"Prediction column:", tableIndex, DoubleValue.class));

		createNewGroup("Conformal Predictive Systems");
		addDialogComponent(
				new DialogComponentBoolean(settings.getRegressionSettings().getNormalizedModel(), "Use Normalization"));

		addDialogComponent(new DialogComponentColumnNameSelection(
				settings.getRegressionSettings().getSigmaColumnModel(), "Difficulty column:",
				tableIndex, false, DoubleValue.class));
		addDialogComponent(new DialogComponentNumber(settings.getRegressionSettings().getBetaModel(), "Beta", 0.05,
				createFlowVariableModel(settings.getRegressionSettings().getBetaModel())));

		createNewGroup("Define output");

		addDialogComponent(
				new DialogComponentBoolean(settings.getKeepColumns().getKeepAllColumnsModel(), "Keep All Columns"));
		addDialogComponent(
				new DialogComponentBoolean(settings.getKeepColumns().getKeepIdColumnModel(), "Keep ID column"));
		addDialogComponent(
				new DialogComponentColumnNameSelection(settings.getKeepColumns().getIdColumnModel(), "ID column:",
						tableIndex, DataValue.class));
	}

}
