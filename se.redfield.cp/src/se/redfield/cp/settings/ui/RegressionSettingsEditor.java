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

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.port.PortObjectSpec;

import se.redfield.cp.settings.RegressionSettings;
import se.redfield.cp.utils.FlowVariableCreator;

/**
 * Editor panel for the {@link RegressionSettings}.
 * 
 * @author Alexander Bondaletov, Redfield SE
 *
 */
public class RegressionSettingsEditor extends JPanel {
	private static final long serialVersionUID = 1L;

	private final RegressionSettings settings;

	private final DialogComponentColumnNameSelection sigmaColumn;

	/**
	 * @param settings  The regression settings.
	 * @param fwCreator The flow variable creator.
	 */
	@SuppressWarnings("unchecked")
	public RegressionSettingsEditor(RegressionSettings settings, FlowVariableCreator fwCreator) {
		this.settings = settings;

		DialogComponentBoolean useNormalization = new DialogComponentBoolean(settings.getNormalizedModel(),
				"Use Normalization");
		useNormalization.getComponentPanel().setLayout(new FlowLayout(FlowLayout.LEFT));

		sigmaColumn = new DialogComponentColumnNameSelection(settings.getSigmaColumnModel(), "",
				settings.getTables()[0].getIdx(), false, DoubleValue.class);

		DialogComponentNumber beta = new DialogComponentNumber(settings.getBetaModel(), "", 0.05,
				fwCreator.createFlowVariableModel(settings.getBetaModel()));

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;

		add(useNormalization.getComponentPanel(), c);

		addRow(1, "Difficulty column:", sigmaColumn.getComponentPanel());
		addRow(2, "Beta", beta.getComponentPanel());

		setBorder(BorderFactory.createTitledBorder("Normalization"));
	}

	private void addRow(int row, String label, Component comp) {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = row;
		c.insets = new Insets(5, 10, 0, 0);

		add(new JLabel(label), c);

		c.gridx += 1;
		add(comp, c);

		c.gridx += 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(Box.createHorizontalGlue(), c);
	}

	/**
	 * @param settings The settings object.
	 * @param specs    The input specs.
	 * @throws NotConfigurableException
	 */
	public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws NotConfigurableException {
		try {
			this.settings.loadSettingFrom(settings);
		} catch (InvalidSettingsException e) {
			// ignore
		}

		sigmaColumn.loadSettingsFrom(settings, specs);
	}
}
