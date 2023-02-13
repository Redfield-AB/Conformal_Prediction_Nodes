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
package se.redfield.cp.settings.ui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;

import se.redfield.cp.settings.PredictiveSystemsClassifierSettings;

public class PredictiveSystemsClassifierSettingsEditor extends JPanel {

	private final PredictiveSystemsClassifierSettings settings;

	private final DialogComponentColumnNameSelection targetColumn;

	private final PercentilesEditor lowerPercentiles;
	private final PercentilesEditor higherPercentiles;

	@SuppressWarnings("unchecked")
	public PredictiveSystemsClassifierSettingsEditor(PredictiveSystemsClassifierSettings settings) {
		this.settings = settings;

		DialogComponentBoolean hasTarget = new DialogComponentBoolean(settings.getHasTargetModel(), "Target value:");
		DialogComponentBoolean hasTargetColumn = new DialogComponentBoolean(settings.getHasTargetColumnModel(),
				"Target colum:");
		DialogComponentNumber target = new DialogComponentNumber(settings.getTargetModel(), "", 0.1);
		target.getComponentPanel().setLayout(new FlowLayout(FlowLayout.LEFT));
		targetColumn = new DialogComponentColumnNameSelection(settings.getTargetColumnModel(), "",
				settings.getTable().getIdx(), DoubleValue.class);
		targetColumn.getComponentPanel().setLayout(new FlowLayout(FlowLayout.LEFT));

		lowerPercentiles = new PercentilesEditor("Lower percentiles");
		higherPercentiles = new PercentilesEditor("Higher percentiles");

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;

		add(hasTarget.getComponentPanel(), c);

		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		add(target.getComponentPanel(), c);

		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.gridx = 0;
		c.gridy += 1;
		add(hasTargetColumn.getComponentPanel(), c);

		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		add(targetColumn.getComponentPanel(), c);

		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy += 1;
		add(lowerPercentiles, c);

		c.gridy += 1;
		add(higherPercentiles, c);

		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridy += 1;
		add(Box.createVerticalGlue(), c);
	}

	public void loadSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
		try {
			targetColumn.loadSettingsFrom(settings, specs);
			this.settings.loadSettingsFrom(settings);
		} catch (InvalidSettingsException e) {
			// ignore
		}

		lowerPercentiles.setPercentiles(this.settings.getLowerPercentiles());
		higherPercentiles.setPercentiles(this.settings.getHigherPercentiles());
	}

	public void saveSettingsTo(NodeSettingsWO setitngs) {
		this.settings.setLowerPercentiles(lowerPercentiles.getPercentiles());
		this.settings.setHigherPercentiles(higherPercentiles.getPercentiles());
		this.settings.saveSettingsTo(setitngs);
	}
}
