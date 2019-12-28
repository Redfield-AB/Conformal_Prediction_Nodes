package se.redfield.cp.nodes;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class ConformalPredictorNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public ConformalPredictorNodeDialog() {
		super();

		SettingsModelString columnSettings = ConformalPredictorNodeModel.createColumnNameSettingsModel();
		addDialogComponent(new DialogComponentColumnNameSelection(columnSettings, "Column:",
				ConformalPredictorNodeModel.PORT_PREDICTION_TABLE, DataValue.class));
	}
}
