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
 * Node Dialog for Conformal Predictor Training Loop Start Node
 *
 */
public class ConformalPredictorTrainingLoopStartNodeDialog extends NodeDialogPane {
	private DialogComponentNumber iterationsInput;
	private SamplingNodeDialogPanel partitionPanel;

	public ConformalPredictorTrainingLoopStartNodeDialog() {
		super();

		iterationsInput = new DialogComponentNumber(
				ConformalPredictorTrainingLoopStartNodeModel.createIterationSettings(), "Number of iterations", 1);

		partitionPanel = new SamplingNodeDialogPanel();
		partitionPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder("Training/Calibration split")));

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(iterationsInput.getComponentPanel());
		panel.add(Box.createHorizontalStrut(5));
		panel.add(partitionPanel);

		addTab("Settings", panel);
	}

	@Override
	public void loadSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
		NodeSettingsRO partitionSettings = new NodeSettings(
				ConformalPredictorTrainingLoopStartNodeModel.KEY_PARTITION_SETTINGS);

		try {
			partitionSettings = settings
					.getNodeSettings(ConformalPredictorTrainingLoopStartNodeModel.KEY_PARTITION_SETTINGS);
		} catch (InvalidSettingsException e) {
			// ignore
		}

		iterationsInput.loadSettingsFrom(settings, specs);
		partitionPanel.loadSettingsFrom(partitionSettings, specs[0]);
	}

	@Override
	public void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		iterationsInput.saveSettingsTo(settings);
		partitionPanel.saveSettingsTo(
				settings.addNodeSettings(ConformalPredictorTrainingLoopStartNodeModel.KEY_PARTITION_SETTINGS));
	}
}
