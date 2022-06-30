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
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;

import se.redfield.cp.settings.CompactClassificationNodeSettigns;
import se.redfield.cp.settings.ui.DialogComponentProbabilityFormat;

/**
 * Node dialog for Classifier node
 *
 */
public class CompactConformalClassificationNodeDialog extends DefaultNodeSettingsPane {

	private final CompactClassificationNodeSettigns settings = new CompactClassificationNodeSettigns();

	@SuppressWarnings("unchecked")
	public CompactConformalClassificationNodeDialog() {
		super();

		addDialogComponent(new DialogComponentColumnNameSelection(settings.getTargetSettings().getTargetColumnModel(),
				"Target column:", CompactConformalClassificationNodeModel.PORT_CALIBRATION_TABLE.getIdx(),
				DataValue.class));
		addDialogComponent(new DialogComponentProbabilityFormat(settings.getTargetSettings()));

		createNewGroup("Define output");

		addDialogComponent(
				new DialogComponentBoolean(settings.getKeepColumns().getKeepAllColumnsModel(), "Keep All Columns"));
		addDialogComponent(
				new DialogComponentBoolean(settings.getKeepColumns().getKeepIdColumnModel(), "Keep ID column"));
		addDialogComponent(new DialogComponentColumnNameSelection(settings.getKeepColumns().getIdColumnModel(),
				"ID column:", CompactConformalClassificationNodeModel.PORT_PREDICTION_TABLE.getIdx(), DataValue.class));

		addDialogComponent(new DialogComponentBoolean(settings.getIncludeRankModel(), "Include Rank column"));

		createNewGroup("User defined error rate");
		addDialogComponent(new DialogComponentNumber(settings.getClassifierSettings().getErrorRateModel(),
				"Error rate (significance level)", 0.05,
				createFlowVariableModel(settings.getClassifierSettings().getErrorRateModel())));
		createNewGroup("Additional output");
		addDialogComponent(new DialogComponentBoolean(settings.getClassifierSettings().getClassesAsStringModel(),
				"Output Classes as String"));
		addDialogComponent(new DialogComponentString(settings.getClassifierSettings().getStringSeparatorModel(),
				"String separator"));
	}

}
