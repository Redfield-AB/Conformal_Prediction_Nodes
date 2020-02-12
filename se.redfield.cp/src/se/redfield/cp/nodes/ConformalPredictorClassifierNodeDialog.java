package se.redfield.cp.nodes;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class ConformalPredictorClassifierNodeDialog extends DefaultNodeSettingsPane {

	public ConformalPredictorClassifierNodeDialog() {
		super();

		SettingsModelDoubleBounded scoreThresholdSettings = ConformalPredictorClassifierNodeModel
				.createScoreThresholdSettings();
		SettingsModelBoolean classesAsStringSettings = ConformalPredictorClassifierNodeModel
				.createClassesAsStringSettings();
		SettingsModelString stringSeparatorSettings = ConformalPredictorClassifierNodeModel
				.createStringSeparatorSettings();

		classesAsStringSettings
				.addChangeListener(e -> stringSeparatorSettings.setEnabled(classesAsStringSettings.getBooleanValue()));

		addDialogComponent(new DialogComponentNumber(scoreThresholdSettings, "Significance level", 0.01));
		createNewGroup("Output");
		addDialogComponent(new DialogComponentBoolean(classesAsStringSettings, "Output Classes as String"));
		addDialogComponent(new DialogComponentString(stringSeparatorSettings, "String separator"));
	}
}
