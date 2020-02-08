package se.redfield.cp.nodes;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public abstract class AbstractConformalPredictorNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public AbstractConformalPredictorNodeDialog(int tableIndex) {
		super();

		SettingsModelString targetColumnSettings = AbstractConformalPredictorNodeModel.createColumnNameSettingsModel();
		SettingsModelBoolean keepAllSettings = AbstractConformalPredictorNodeModel.createKeepAllColumnsSettingsModel();
		SettingsModelBoolean keepIdColumnSetting = AbstractConformalPredictorNodeModel.createKeepIdColumnSettings();
		SettingsModelString idColumnSettings = AbstractConformalPredictorNodeModel.createIdColumnSettings();

		keepIdColumnSetting.setEnabled(!keepAllSettings.getBooleanValue());
		idColumnSettings.setEnabled(keepIdColumnSetting.getBooleanValue());

		keepAllSettings.addChangeListener(e -> {
			keepIdColumnSetting.setEnabled(!keepAllSettings.getBooleanValue());
			if (!keepIdColumnSetting.isEnabled()) {
				keepIdColumnSetting.setBooleanValue(false);
			}
		});
		keepIdColumnSetting.addChangeListener(e -> idColumnSettings.setEnabled(keepIdColumnSetting.getBooleanValue()));

		addDialogComponent(new DialogComponentColumnNameSelection(targetColumnSettings, "Target column:", tableIndex,
				DataValue.class));

		createNewGroup("Output");

		addDialogComponent(new DialogComponentBoolean(keepAllSettings, "Keep All Columns"));
		addDialogComponent(new DialogComponentBoolean(keepIdColumnSetting, "Keep ID column"));
		addDialogComponent(
				new DialogComponentColumnNameSelection(idColumnSettings, "ID column:", tableIndex, DataValue.class));
	}

}
