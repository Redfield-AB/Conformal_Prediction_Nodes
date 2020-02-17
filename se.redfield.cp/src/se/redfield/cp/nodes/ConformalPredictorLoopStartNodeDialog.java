/*
 * Copyright (c) 2020 Redfield AB.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, Version 3, as
 * published by the Free Software Foundation.
 *  
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package se.redfield.cp.nodes;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.Border;

import org.knime.base.node.preproc.sample.SamplingNodeDialogPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;

/**
 * Node dialog for Conformal Predictor Loop Start node.
 *
 */
public class ConformalPredictorLoopStartNodeDialog extends NodeDialogPane {

	private DialogComponentNumber iterationsInput;
	private SamplingNodeDialogPanel testSetPanel;
	private SamplingNodeDialogPanel calibrationSetPanel;

	public ConformalPredictorLoopStartNodeDialog() {
		super();

		iterationsInput = new DialogComponentNumber(ConformalPredictorLoopStartNodeModel.createIterationSettings(),
				"Number of cross-validation iterations", 1);

		testSetPanel = new SamplingNodeDialogPanel();
		testSetPanel.setBorder(createBorder("Training/Test split"));

		calibrationSetPanel = new SamplingNodeDialogPanel();
		calibrationSetPanel.setBorder(createBorder("Training/Calibration split"));

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(iterationsInput.getComponentPanel());
		panel.add(Box.createHorizontalStrut(5));
		panel.add(testSetPanel);
		panel.add(new JSeparator());
		panel.add(calibrationSetPanel);

		addTab("Settings", panel);
	}

	private Border createBorder(String title) {
		return BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(title));
	}

	@Override
	public void loadSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
		NodeSettingsRO testSetSettings = new NodeSettings(ConformalPredictorLoopStartNodeModel.KEY_TEST_PARTITION);
		NodeSettingsRO calibrationSetSettings = new NodeSettings(
				ConformalPredictorLoopStartNodeModel.KEY_CALIBRATION_PARTITION);
		try {
			testSetSettings = settings.getNodeSettings(ConformalPredictorLoopStartNodeModel.KEY_TEST_PARTITION);
			calibrationSetSettings = settings
					.getNodeSettings(ConformalPredictorLoopStartNodeModel.KEY_CALIBRATION_PARTITION);
		} catch (InvalidSettingsException e) {
			// ignore
		}

		iterationsInput.loadSettingsFrom(settings, specs);
		testSetPanel.loadSettingsFrom(testSetSettings, specs[0]);
		calibrationSetPanel.loadSettingsFrom(calibrationSetSettings, specs[0]);
	}

	@Override
	public void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		iterationsInput.saveSettingsTo(settings);
		testSetPanel.saveSettingsTo(settings.addNodeSettings(ConformalPredictorLoopStartNodeModel.KEY_TEST_PARTITION));
		calibrationSetPanel.saveSettingsTo(
				settings.addNodeSettings(ConformalPredictorLoopStartNodeModel.KEY_CALIBRATION_PARTITION));
	}

}
