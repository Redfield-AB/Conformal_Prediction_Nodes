package se.redfield.cp.nodes;

import org.knime.base.node.preproc.sample.SamplingNodeDialogPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

public class ConformalPredictorLoopStartNodeDialog extends DefaultNodeSettingsPane {

	private SamplingNodeDialogPanel testSetPanel;
	private SamplingNodeDialogPanel calibrationSetPanel;

	public ConformalPredictorLoopStartNodeDialog() {
		super();

		SettingsModelIntegerBounded iterationsSettings = ConformalPredictorLoopStartNodeModel.createIterationSettings();
		addDialogComponent(new DialogComponentNumber(iterationsSettings, "Number of iterations", 1));

		testSetPanel = new SamplingNodeDialogPanel();
		calibrationSetPanel = new SamplingNodeDialogPanel();

		addTab("Test Set", testSetPanel);
		addTab("Calibration Set", calibrationSetPanel);
	}

	@Override
	public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs)
			throws NotConfigurableException {
		NodeSettingsRO testSetSettings = new NodeSettings(ConformalPredictorLoopStartNodeModel.KEY_TEST_PARTITION);
		NodeSettingsRO calibrationSetSettings = new NodeSettings(
				ConformalPredictorLoopStartNodeModel.KEY_CALIBRATION_PARTITION);
		try {
			testSetSettings = settings.getNodeSettings(ConformalPredictorLoopStartNodeModel.KEY_TEST_PARTITION);
			calibrationSetSettings = settings
					.getNodeSettings(ConformalPredictorLoopStartNodeModel.KEY_CALIBRATION_PARTITION);
		} catch (InvalidSettingsException e) {
		}

		testSetPanel.loadSettingsFrom(testSetSettings, specs[0]);
		calibrationSetPanel.loadSettingsFrom(calibrationSetSettings, specs[0]);
	}

	@Override
	public void saveAdditionalSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		testSetPanel.saveSettingsTo(settings.addNodeSettings(ConformalPredictorLoopStartNodeModel.KEY_TEST_PARTITION));
		calibrationSetPanel.saveSettingsTo(
				settings.addNodeSettings(ConformalPredictorLoopStartNodeModel.KEY_CALIBRATION_PARTITION));
	}
}
