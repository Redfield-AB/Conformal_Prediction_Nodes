package se.redfield.cp.nodes;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is an example implementation of the node dialog of the
 * "ConformalPredictionCalibrator" node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}. In general, one can create an
 * arbitrary complex dialog using Java Swing.
 * 
 * @author Redfield AB
 */
public class ConformalPredictorCalibratorNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New dialog pane for configuring the node. The dialog created here will show
	 * up when double clicking on a node in KNIME Analytics Platform.
	 */
	@SuppressWarnings("unchecked")
	protected ConformalPredictorCalibratorNodeDialog() {
		super();
		SettingsModelString stringSettings = ConformalPredictorCalibratorNodeModel.createColumnNameSettingsModel();
		// Add a new String component to the dialog.
		addDialogComponent(new DialogComponentColumnNameSelection(stringSettings, "Column: ", 0, DataValue.class));
	}
}
