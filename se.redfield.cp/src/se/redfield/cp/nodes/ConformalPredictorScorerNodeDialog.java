package se.redfield.cp.nodes;

import org.knime.core.data.DataValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class ConformalPredictorScorerNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public ConformalPredictorScorerNodeDialog() {
		super();

		SettingsModelString targetColumnSettings = ConformalPredictorScorerNodeModel.createTargetColumnSettings();
		SettingsModelString classesColumnSettings = ConformalPredictorScorerNodeModel.createClassesColumnSettings();

		addDialogComponent(
				new DialogComponentColumnNameSelection(targetColumnSettings, "Target column:", 0, DataValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(classesColumnSettings, "Classes column:", 0,
				CollectionDataValue.class));
	}
}
