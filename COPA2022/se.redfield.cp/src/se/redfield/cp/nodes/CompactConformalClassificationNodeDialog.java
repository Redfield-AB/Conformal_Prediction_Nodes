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
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Node dialog for Classifier node
 *
 */
public class CompactConformalClassificationNodeDialog extends AbstractConformalPredictorNodeDialog {

	private SettingsModelBoolean classesAsStringSettings;
	private SettingsModelString stringSeparatorSettings;

	public CompactConformalClassificationNodeDialog() {
		super(CompactConformalClassificationNodeModel.PORT_PREDICTION_TABLE);

		SettingsModelDoubleBounded errorRateSettings = ConformalPredictorClassifierNodeModel.createErrorRateSettings();
		classesAsStringSettings = ConformalPredictorClassifierNodeModel.createClassesAsStringSettings();
		stringSeparatorSettings = ConformalPredictorClassifierNodeModel.createStringSeparatorSettings();

		classesAsStringSettings
				.addChangeListener(e -> stringSeparatorSettings.setEnabled(classesAsStringSettings.getBooleanValue()));
		createNewGroup("User defined error rate");
		addDialogComponent(new DialogComponentNumber(errorRateSettings, "Error rate (significance level)", 0.05,
				createFlowVariableModel(errorRateSettings)));
		createNewGroup("Additional output");
		addDialogComponent(new DialogComponentBoolean(classesAsStringSettings, "Output Classes as String"));
		addDialogComponent(new DialogComponentString(stringSeparatorSettings, "String separator"));
	}

	@Override
	public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs)
			throws NotConfigurableException {
		stringSeparatorSettings.setEnabled(classesAsStringSettings.getBooleanValue());
	}
}
