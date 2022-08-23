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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Node dialog for Predictor Scorer Node.
 *
 */
public class ConformalPredictorScorerNodeDialog extends DefaultNodeSettingsPane {

	private DataTableSpec spec;
	private SettingsModelString classesColumnSettings;
	private DialogComponentString stringSeparatorComp;
	private SettingsModelBoolean additionalEfficiencyMetricsSettings;

	/**
	 * Creates new instance
	 */
	@SuppressWarnings("unchecked")
	public ConformalPredictorScorerNodeDialog() {
		super();

		SettingsModelString targetColumnSettings = ConformalPredictorScorerNodeModel.createTargetColumnSettings();
		classesColumnSettings = ConformalPredictorScorerNodeModel.createClassesColumnSettings();
		SettingsModelString stringSeparatorSettings = ConformalPredictorScorerNodeModel.createStringSeparatorSettings();
		SettingsModelBoolean additionalInfoSettings = ConformalPredictorScorerNodeModel.createAdditionalInfoSettings();
		additionalEfficiencyMetricsSettings = ConformalPredictorScorerNodeModel
				.createAdditionalEfficiencyMetricsSettings();

		stringSeparatorComp = new DialogComponentString(stringSeparatorSettings, "String separator:");
		stringSeparatorComp.getComponentPanel().setVisible(false);

		classesColumnSettings.addChangeListener(e -> calcStringSeparatorVisibility());

		addDialogComponent(
				new DialogComponentColumnNameSelection(targetColumnSettings, "Target column:", 0, DataValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(classesColumnSettings, "Classes column:", 0,
				CollectionDataValue.class, StringValue.class));
		addDialogComponent(stringSeparatorComp);
		createNewGroup("Output");
		addDialogComponent(new DialogComponentBoolean(additionalInfoSettings, "Additional prediction information"));
		addDialogComponent(
				new DialogComponentBoolean(additionalEfficiencyMetricsSettings, "Additional efficiency metrics"));
	}

	private void calcStringSeparatorVisibility() {
		if (spec != null) {
			DataColumnSpec columnSpec = spec.getColumnSpec(classesColumnSettings.getStringValue());
			stringSeparatorComp.getComponentPanel()
					.setVisible(columnSpec != null && columnSpec.getType().isCompatible(StringValue.class));
		}
	}

	@Override
	public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs)
			throws NotConfigurableException {
		this.spec = specs[0];
		calcStringSeparatorVisibility();
	}
}
