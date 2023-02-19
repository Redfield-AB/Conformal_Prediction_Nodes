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

import static se.redfield.cp.nodes.ConformalPredictorScorerRegressionNodeModel.PORT_INPUT_TABLE;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;

import se.redfield.cp.settings.ConformalPredictorScorerRegressionSettings;

/**
 * Node dialog for Regression Scorer Node.
 *
 */
public class ConformalPredictorScorerRegressionNodeDialog extends NodeDialogPane {

	private final ConformalPredictorScorerRegressionSettings settings = new ConformalPredictorScorerRegressionSettings();

	private final DialogComponentColumnNameSelection targetColumn;
	private final DialogComponentColumnNameSelection upperBound;
	private final DialogComponentColumnNameSelection lowerBound;

	/**
	 * Creates new instance
	 */
	@SuppressWarnings("unchecked")
	public ConformalPredictorScorerRegressionNodeDialog() {
		super();

		targetColumn = new DialogComponentColumnNameSelection(settings.getTargetColumnModel(), "",
				PORT_INPUT_TABLE.getIdx(), DoubleValue.class);
		upperBound = new DialogComponentColumnNameSelection(settings.getUpperboundColumnModel(), "",
				PORT_INPUT_TABLE.getIdx(), DoubleValue.class);
		lowerBound = new DialogComponentColumnNameSelection(settings.getLowerboundColumnModel(), "",
				PORT_INPUT_TABLE.getIdx(), DoubleValue.class);

		addTab("Settings", createSettingsTab());
	}

	private Component createSettingsTab() {
		DialogComponentBoolean additionalInfo = new DialogComponentBoolean(settings.getAdditionalInfoModel(),
				"Compute additional prediction information");
		DialogComponentBoolean hasUpperBound = new DialogComponentBoolean(settings.getHasUpperBoundModel(),
				"Upper bound:");
		DialogComponentBoolean hasLowerBound = new DialogComponentBoolean(settings.getHasLowerBoundModel(),
				"Lower bound:");

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(0, 10, 0, 0);
		panel.add(new JLabel("Target column:"), c);

		c.gridx += 1;
		c.insets = new Insets(0, 0, 0, 0);
		panel.add(targetColumn.getComponentPanel(), c);

		c.gridx = 0;
		c.gridy += 1;
		panel.add(hasUpperBound.getComponentPanel(), c);

		c.gridx += 1;
		panel.add(upperBound.getComponentPanel(), c);

		c.gridx = 0;
		c.gridy += 1;
		panel.add(hasLowerBound.getComponentPanel(), c);

		c.gridx += 1;
		panel.add(lowerBound.getComponentPanel(), c);

		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy += 1;
		panel.add(additionalInfo.getComponentPanel(), c);

		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 3;
		c.gridx = 0;
		c.gridy += 1;
		panel.add(Box.createGlue(), c);
		return panel;
	}

	@Override
	protected void loadSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
		try {
			this.settings.loadValidatedSettingsFrom(settings);
		} catch (InvalidSettingsException e) {
			// ignore
		}

		targetColumn.loadSettingsFrom(settings, specs);
		upperBound.loadSettingsFrom(settings, specs);
		lowerBound.loadSettingsFrom(settings, specs);
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		this.settings.saveSettingsTo(settings);
	}

}
