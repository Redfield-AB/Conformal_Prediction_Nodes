package se.redfield.cp.nodes;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class ConformalPredictorScorerNodeDialog extends DefaultNodeSettingsPane {

	private DataTableSpec spec;
	private SettingsModelString classesColumnSettings;
	private DialogComponentString stringSeparatorComp;

	@SuppressWarnings("unchecked")
	public ConformalPredictorScorerNodeDialog() {
		super();

		SettingsModelString targetColumnSettings = ConformalPredictorScorerNodeModel.createTargetColumnSettings();
		classesColumnSettings = ConformalPredictorScorerNodeModel.createClassesColumnSettings();
		SettingsModelString stringSeparatorSettings = ConformalPredictorClassifierNodeModel
				.createStringSeparatorSettings();

		stringSeparatorComp = new DialogComponentString(stringSeparatorSettings, "String separator:");
		stringSeparatorComp.getComponentPanel().setVisible(false);

		classesColumnSettings.addChangeListener(e -> calcStringSeparatorVisibility());

		addDialogComponent(
				new DialogComponentColumnNameSelection(targetColumnSettings, "Target column:", 0, DataValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(classesColumnSettings, "Classes column:", 0,
				CollectionDataValue.class, StringValue.class));
		addDialogComponent(stringSeparatorComp);
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
