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
package se.redfield.cp.nodes.ps.classifier;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;

import se.redfield.cp.settings.ui.PredictiveSystemsClassifierSettingsEditor;

/**
 * Node dialog for Predictive Systems Classifier node
 *
 */
public class PredictiveSystemsClassifierNodeDialog extends NodeDialogPane {

	private final PredictiveSystemsClassifierNodeSettings settings = new PredictiveSystemsClassifierNodeSettings();

	private final DialogComponentColumnNameSelection probabilityDistributionColumn;
	private final PredictiveSystemsClassifierSettingsEditor classifierSettingsEditor;

	/**
	 * Creates new instance
	 */
	@SuppressWarnings("unchecked")
	public PredictiveSystemsClassifierNodeDialog() {
		super();
		probabilityDistributionColumn = new DialogComponentColumnNameSelection(
				settings.getProbabilityDistributionColumnModel(), "Probability distribution column",
				PredictiveSystemsClassifierNodeModel.INPUT_TABLE.getIdx(), CollectionDataValue.class);
		classifierSettingsEditor = new PredictiveSystemsClassifierSettingsEditor(settings.getClassifierSettings());

		addTab("Settings", createSettingsTab());
	}

	private JComponent createSettingsTab() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 10, 0, 0);
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;

		probabilityDistributionColumn.getComponentPanel().setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(probabilityDistributionColumn.getComponentPanel(), c);

		c.gridy += 1;
		panel.add(classifierSettingsEditor, c);

		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridy += 1;
		panel.add(Box.createVerticalGlue(), c);
		return panel;
	}

	@Override
	protected void loadSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
		probabilityDistributionColumn.loadSettingsFrom(settings, specs);
		classifierSettingsEditor.loadSettingsFrom(settings, specs);
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		probabilityDistributionColumn.saveSettingsTo(settings);
		classifierSettingsEditor.saveSettingsTo(settings);
	}

}
