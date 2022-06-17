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
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Base class for Predictor and Calibrator nodes dialogs
 *
 */
public abstract class AbstractConformalPredictorRegressionNodeDialog extends AbstractConformalPredictorNodeDialog {

	private SettingsModelBoolean normalizedSetting;

	@SuppressWarnings("unchecked")
	public AbstractConformalPredictorRegressionNodeDialog(int tableIndex, boolean visibleTarget) {
		super(tableIndex, visibleTarget);
		createNewGroup("Conformal Regression");
		SettingsModelDoubleBounded betaSettings = ConformalPredictorRegressionNodeModel.createBetaSettings();


		SettingsModelString sigmaColumnSettings = AbstractConformalPredictorRegressionNodeModel.createSigmaColumnNameSettingsModel();
		normalizedSetting = AbstractConformalPredictorRegressionNodeModel.createNormalizedSettingsModel();
		
		normalizedSetting.addChangeListener(e -> {
			sigmaColumnSettings.setEnabled(normalizedSetting.getBooleanValue());
			betaSettings.setEnabled(normalizedSetting.getBooleanValue());
		});		
		sigmaColumnSettings.setEnabled(normalizedSetting.getBooleanValue());
		betaSettings.setEnabled(normalizedSetting.getBooleanValue());

		addDialogComponent(new DialogComponentBoolean(normalizedSetting, "Use Normalization"));

		addDialogComponent(new DialogComponentColumnNameSelection(sigmaColumnSettings, "Difficulty column:", tableIndex,
				DataValue.class));
		addDialogComponent(new DialogComponentNumber(betaSettings, "Beta", 0.05,
				createFlowVariableModel(betaSettings)));
	}

}
