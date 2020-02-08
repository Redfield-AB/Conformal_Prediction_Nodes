package se.redfield.cp.nodes;

import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

public class ConformalPredictorNodeDialog extends AbstractConformalPredictorNodeDialog {

	public ConformalPredictorNodeDialog() {
		super(ConformalPredictorNodeModel.PORT_PREDICTION_TABLE);

		SettingsModelBoolean includeRank = ConformalPredictorNodeModel.createIncludeRankSettings();

		addDialogComponent(new DialogComponentBoolean(includeRank, "Include Rank column"));
	}
}
