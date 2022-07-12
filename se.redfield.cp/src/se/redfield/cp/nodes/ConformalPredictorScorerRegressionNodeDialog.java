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

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Node dialog for Regression Scorer Node.
 *
 */
public class ConformalPredictorScorerRegressionNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * Creates new instance
	 */
	@SuppressWarnings("unchecked")
	public ConformalPredictorScorerRegressionNodeDialog() {
		super();

		SettingsModelString targetColumnSettings = ConformalPredictorScorerRegressionNodeModel.createTargetColumnSettings();
		SettingsModelString upperboundColumnSettings = ConformalPredictorScorerRegressionNodeModel.createUpperBoundColumnSettings();
		SettingsModelString lowerboundColumnSettings = ConformalPredictorScorerRegressionNodeModel.createLowerBoundColumnSettings();

		SettingsModelBoolean additionalInfoSettings = ConformalPredictorScorerRegressionNodeModel.createAdditionalInfoSettings();

		addDialogComponent(
				new DialogComponentColumnNameSelection(targetColumnSettings, "Target column:", 0, DoubleValue.class));
		addDialogComponent(
				new DialogComponentColumnNameSelection(upperboundColumnSettings, "Upper bound:", 0, DoubleValue.class));
		addDialogComponent(
				new DialogComponentColumnNameSelection(lowerboundColumnSettings, "Lower bound:", 0, DoubleValue.class));
		createNewGroup("Output");
		addDialogComponent(new DialogComponentBoolean(additionalInfoSettings, "Additional prediction information"));
	}

}
