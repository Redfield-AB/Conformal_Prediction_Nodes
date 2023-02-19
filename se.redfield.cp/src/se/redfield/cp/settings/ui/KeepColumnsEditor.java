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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.port.PortObjectSpec;

import se.redfield.cp.settings.KeepColumnsSettings;

/**
 * Editor panel for the {@link KeepColumnsSettings}.
 * 
 * @author Alexander Bondaletov, Redfield SE
 *
 */
public class KeepColumnsEditor extends JPanel {
	private static final long serialVersionUID = 1L;

	private final KeepColumnsSettings settings;
	private final DialogComponentColumnNameSelection idColumn;

	/**
	 * @param settings The settings
	 */
	@SuppressWarnings("unchecked")
	public KeepColumnsEditor(KeepColumnsSettings settings) {
		this.settings = settings;

		DialogComponentBoolean keepAll = new DialogComponentBoolean(settings.getKeepAllColumnsModel(),
				"Keep All Columns");
		DialogComponentBoolean keepId = new DialogComponentBoolean(settings.getKeepIdColumnModel(), "Keep ID column:");
		idColumn = new DialogComponentColumnNameSelection(settings.getIdColumnModel(), "", settings.getTable().getIdx(),
				DataValue.class);

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		add(keepAll.getComponentPanel(), c);

		c.gridy += 1;
		c.gridwidth = 1;
		add(keepId.getComponentPanel(), c);

		c.gridx += 1;
		add(idColumn.getComponentPanel(), c);

		c.gridx += 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(Box.createHorizontalGlue(), c);

		setBorder(BorderFactory.createTitledBorder("Define output"));
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

		idColumn.loadSettingsFrom(settings, specs);
	}
}
