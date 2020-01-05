package se.redfield.cp.nodes;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;

public class ConformalPredictorClassifierNodeDialog extends DefaultNodeSettingsPane {

	public ConformalPredictorClassifierNodeDialog() {
		super();

		SettingsModelDoubleBounded scoreThresholdSettings = ConformalPredictorClassifierNodeModel
				.createScoreThresholdSettings();

		addDialogComponent(new DialogComponentNumber(scoreThresholdSettings, "Score Threshold", 0.01));
	}
}
