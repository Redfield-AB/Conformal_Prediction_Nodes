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

import static se.redfield.cp.nodes.CompactConformalRegressionNodeModel.PORT_CALIBRATION_TABLE;
import static se.redfield.cp.nodes.CompactConformalRegressionNodeModel.PORT_PREDICTION_TABLE;

import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;

import se.redfield.cp.settings.CompactRegressionNodeSettings;

/**
 * Node dialog for Compact Conformal Regression node.
 *
 */
public class CompactConformalRegressionNodeDialog extends DefaultNodeSettingsPane {

	private CompactRegressionNodeSettings settings = new CompactRegressionNodeSettings();

	/**
	 * Creates new instance
	 */
	@SuppressWarnings("unchecked")
	public CompactConformalRegressionNodeDialog() {
		super();

		addDialogComponent(new DialogComponentColumnNameSelection(settings.getTargetColumnModel(), "Target column:",
				PORT_CALIBRATION_TABLE.getIdx(), DoubleValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(settings.getPredictionColumnModel(),
				"Prediction column:", PORT_CALIBRATION_TABLE.getIdx(), DoubleValue.class));

		createNewGroup("Conformal Regression");
		addDialogComponent(
				new DialogComponentBoolean(settings.getRegressionSettings().getNormalizedModel(), "Use Normalization"));

		addDialogComponent(
				new DialogComponentColumnNameSelection(settings.getRegressionSettings().getSigmaColumnModel(),
						"Difficulty column:", PORT_CALIBRATION_TABLE.getIdx(), false, DoubleValue.class));
		addDialogComponent(new DialogComponentNumber(settings.getRegressionSettings().getBetaModel(), "Beta", 0.05,
				createFlowVariableModel(settings.getRegressionSettings().getBetaModel())));

		createNewGroup("User defined error rate");
		addDialogComponent(new DialogComponentNumber(settings.getErrorRateModel(), "Error rate (significance level)",
				0.05, createFlowVariableModel(settings.getErrorRateModel())));

		createNewGroup("Define output");
		addDialogComponent(
				new DialogComponentBoolean(settings.getKeepColumns().getKeepAllColumnsModel(), "Keep All Columns"));
		addDialogComponent(
				new DialogComponentBoolean(settings.getKeepColumns().getKeepIdColumnModel(), "Keep ID column"));
		addDialogComponent(new DialogComponentColumnNameSelection(settings.getKeepColumns().getIdColumnModel(),
				"ID column:", PORT_PREDICTION_TABLE.getIdx(), DataValue.class));
	}
}
