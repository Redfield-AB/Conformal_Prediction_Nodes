/*
 * Copyright (c) 2022 Redfield AB.
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
package se.redfield.cp.nodes.ps.compact;

import static se.redfield.cp.nodes.ps.compact.CompactPredictiveSystemsRegressionNodeModel.PORT_CALIBRATION_TABLE;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;

import se.redfield.cp.settings.ui.KeepColumnsEditor;
import se.redfield.cp.settings.ui.PredictiveSystemsClassifierSettingsEditor;
import se.redfield.cp.settings.ui.RegressionSettingsEditor;

/**
 * The node dialog for the {@link CompactPredictiveSystemsRegressionNodeModel}
 * node.
 * 
 * @author Alexander Bondaletov
 *
 */
public class CompactPredictiveSystemsRegressionNodeDialog extends NodeDialogPane {

	private final CompactPredictiveSystemsRegressionNodeSettings settings = new CompactPredictiveSystemsRegressionNodeSettings();

	private final DialogComponentColumnNameSelection targetColumn;
	private final DialogComponentColumnNameSelection predictionColumn;
	private final RegressionSettingsEditor regressionPanel;
	private final KeepColumnsEditor keepColumnPanel;
	private final PredictiveSystemsClassifierSettingsEditor classifierPanel;

	/**
	 * Creates new instance
	 */
	@SuppressWarnings("unchecked")
	public CompactPredictiveSystemsRegressionNodeDialog() {
		super();
		targetColumn = new DialogComponentColumnNameSelection(settings.getTargetColumnModel(), "",
				PORT_CALIBRATION_TABLE.getIdx(), DoubleValue.class);
		predictionColumn = new DialogComponentColumnNameSelection(settings.getPredictionColumnModel(), "",
				PORT_CALIBRATION_TABLE.getIdx(), DoubleValue.class);
		regressionPanel = new RegressionSettingsEditor(settings.getRegressionSettings(), this::createFlowVariableModel);
		keepColumnPanel = new KeepColumnsEditor(settings.getKeepColumns());
		classifierPanel = new PredictiveSystemsClassifierSettingsEditor(settings.getClassifierSettings());
		classifierPanel.setBorder(BorderFactory.createTitledBorder("Classification settings"));

		addTab("Settings", createSettingsTab());
	}

	private Component createSettingsTab() {
		JPanel panel = new JPanel(new GridBagLayout());
		addRow(0, panel, "Target column", targetColumn.getComponentPanel());
		addRow(1, panel, "Prediction column", predictionColumn.getComponentPanel());

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridwidth = 3;
		c.gridx = 0;
		c.gridy = 2;
		panel.add(regressionPanel, c);

		c.gridy += 1;
		panel.add(keepColumnPanel, c);

		c.gridy += 1;
		panel.add(classifierPanel, c);

		c.gridy += 1;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		panel.add(Box.createVerticalGlue(), c);
		return panel;
	}

	private static void addRow(int row, JPanel panel, String label, Component comp) {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = row;
		c.insets = new Insets(5, 10, 0, 0);

		panel.add(new JLabel(label), c);

		c.gridx += 1;
		panel.add(comp, c);

		c.gridx += 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(Box.createHorizontalGlue(), c);
	}

	@Override
	protected void loadSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
		try {
			this.settings.loadSettingFrom(settings);
			classifierPanel.loadSettingsFrom(
					settings.getNodeSettings(CompactPredictiveSystemsRegressionNodeSettings.KEY_CLASSIFIER), specs);
		} catch (InvalidSettingsException e) {
			// ignore
		}
		targetColumn.loadSettingsFrom(settings, specs);
		predictionColumn.loadSettingsFrom(settings, specs);
		regressionPanel.loadSettingsFrom(settings, specs);
		keepColumnPanel.loadSettingsFrom(settings, specs);
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		classifierPanel.updateModel();
		this.settings.saveSettingsTo(settings);
	}
}
