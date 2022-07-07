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

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Node dialog for Regression Scorer Node.
 *
 */
public class ConformalPredictorScorerRegressionNodeDialog extends DefaultNodeSettingsPane {

	//private DataTableSpec spec;
	private DialogComponentString stringSeparatorComp;

	@SuppressWarnings("unchecked")
	public ConformalPredictorScorerRegressionNodeDialog() {
		super();

		SettingsModelString targetColumnSettings = ConformalPredictorScorerRegressionNodeModel.createTargetColumnSettings();
		SettingsModelString upperboundColumnSettings = ConformalPredictorScorerRegressionNodeModel.createUpperBoundColumnSettings();
		SettingsModelString lowerboundColumnSettings = ConformalPredictorScorerRegressionNodeModel.createLowerBoundColumnSettings();

		SettingsModelString stringSeparatorSettings = ConformalPredictorScorerRegressionNodeModel
				.createStringSeparatorSettings();
		SettingsModelBoolean additionalInfoSettings = ConformalPredictorScorerRegressionNodeModel.createAdditionalInfoSettings();

		stringSeparatorComp = new DialogComponentString(stringSeparatorSettings, "String separator:");
		stringSeparatorComp.getComponentPanel().setVisible(false);

		addDialogComponent(new DialogComponentColumnNameSelection(targetColumnSettings, "Target column:", 0, DataValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(upperboundColumnSettings, "Upper bound:", 0, DataValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(lowerboundColumnSettings, "Lower bound:", 0, DataValue.class));
		addDialogComponent(stringSeparatorComp);
		createNewGroup("Output");
		addDialogComponent(new DialogComponentBoolean(additionalInfoSettings, "Additional prediction information"));
	}

	private void calcStringSeparatorVisibility() {
	}

	@Override
	public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs)
			throws NotConfigurableException {
		//this.spec = specs[0];
		calcStringSeparatorVisibility();
	}
}
