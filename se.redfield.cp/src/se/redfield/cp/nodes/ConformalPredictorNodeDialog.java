package se.redfield.cp.nodes;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class ConformalPredictorNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public ConformalPredictorNodeDialog() {
		super();

		SettingsModelString columnSettings = ConformalPredictorNodeModel.createColumnNameSettingsModel();
		SettingsModelBoolean keepAllSettings = ConformalPredictorNodeModel.createKeepAllColumnsSettingsModel();
		addDialogComponent(new DialogComponentColumnNameSelection(columnSettings, "Column:",
				ConformalPredictorNodeModel.PORT_CALIBRATION_TABLE, DataValue.class));
		addDialogComponent(new DialogComponentBoolean(keepAllSettings, "Keep All Columns"));
	}
}
