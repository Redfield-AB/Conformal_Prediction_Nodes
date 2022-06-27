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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Base class for Predictor and Calibrator nodes dialogs
 *
 */
public abstract class AbstractConformalPredictorNodeDialog extends DefaultNodeSettingsPane {

	private SettingsModelString targetColumnSettings;
	private SettingsModelString predictionColumnSettings;
	private SettingsModelBoolean keepAllSettings;
	private SettingsModelBoolean keepIdColumnSetting;
	private SettingsModelString idColumnSettings;
	private SettingsModelBoolean includeRank;

	@SuppressWarnings("unchecked")
	public AbstractConformalPredictorNodeDialog(int tableIndex) {
		super();

		targetColumnSettings = AbstractConformalPredictorNodeModel.createTargetColumnSettings();
		predictionColumnSettings = AbstractConformalPredictorNodeModel.createPredictionColumnSettings();
		keepAllSettings = AbstractConformalPredictorNodeModel.createKeepAllColumnsSettingsModel();
		keepIdColumnSetting = AbstractConformalPredictorNodeModel.createKeepIdColumnSettings();
		idColumnSettings = AbstractConformalPredictorNodeModel.createIdColumnSettings();
		// includeRank = ConformalPredictorNodeModel.createIncludeRankSettings();

		keepAllSettings.addChangeListener(e -> {
			keepIdColumnSetting.setEnabled(!keepAllSettings.getBooleanValue());
			if (!keepIdColumnSetting.isEnabled()) {
				keepIdColumnSetting.setBooleanValue(false);
			}
		});
		keepIdColumnSetting.addChangeListener(e -> idColumnSettings.setEnabled(keepIdColumnSetting.getBooleanValue()));
		addDialogComponent(new DialogComponentColumnNameSelection(targetColumnSettings, "Target column:", tableIndex,
				DataValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(predictionColumnSettings, "Prediction column:",
				tableIndex, DataValue.class));

		createNewGroup("Define output");

		addDialogComponent(new DialogComponentBoolean(keepAllSettings, "Keep All Columns"));
		addDialogComponent(new DialogComponentBoolean(keepIdColumnSetting, "Keep ID column"));
		addDialogComponent(
				new DialogComponentColumnNameSelection(idColumnSettings, "ID column:", tableIndex, DataValue.class));

		// addDialogComponent(new DialogComponentBoolean(includeRank, "Include Rank
		// column"));
	}

	@Override
	public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs)
			throws NotConfigurableException {

		keepIdColumnSetting.setEnabled(!keepAllSettings.getBooleanValue());
		idColumnSettings.setEnabled(keepIdColumnSetting.getBooleanValue());
	}

}
