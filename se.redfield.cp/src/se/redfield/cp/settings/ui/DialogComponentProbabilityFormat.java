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
package se.redfield.cp.settings.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

import se.redfield.cp.settings.TargetSettings;
import se.redfield.cp.utils.PortDef;

public class DialogComponentProbabilityFormat extends DialogComponent {

	private final TargetSettings targetSettings;
	private final JTextField formatInput;
	private final JButton checkBtn;
	private final JLabel statusLabel;

	public DialogComponentProbabilityFormat(TargetSettings settings) {
		super(settings.getProbabilityFormatModel());
		this.targetSettings = settings;

		formatInput = new JTextField(15);
		formatInput.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				updateModel();
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				updateModel();
			}

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				updateModel();
			}
		});

		statusLabel = new JLabel("");
		statusLabel.setBorder(BorderFactory.createTitledBorder("Check results"));
		statusLabel.setVisible(false);
		statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		checkBtn = new JButton("Check");
		checkBtn.addActionListener(this::onCheck);

		JPanel inputsRow = new JPanel();
		inputsRow.add(new JLabel("Probability column format:"));
		inputsRow.add(formatInput);
		inputsRow.add(checkBtn);

		getComponentPanel().setLayout(new BoxLayout(getComponentPanel(), BoxLayout.Y_AXIS));
		getComponentPanel().add(inputsRow);
		getComponentPanel().add(statusLabel);
	}

	private void onCheck(ActionEvent e) {
		updateModel();

		statusLabel.setText(performCheck());
		statusLabel.setVisible(true);
	}

	private String performCheck() {
		DataColumnSpec targetColumnSpec = ((DataTableSpec) getLastTableSpec(
				targetSettings.getTargetColumnTable().getIdx())).getColumnSpec(targetSettings.getTargetColumn());

		if (!targetColumnSpec.getDomain().hasValues() || targetColumnSpec.getDomain().getValues().isEmpty()) {
			return "Insufficient domain information for the target column";
		}

		StringBuilder message = new StringBuilder("<html>");

		for (PortDef pTable : targetSettings.getProbabilityColumnsTables()) {
			if (targetSettings.getProbabilityColumnsTables().length > 1) {
				message.append(pTable.getName()).append("<br>");
			}

			DataTableSpec spec = (DataTableSpec) getLastTableSpec(pTable.getIdx());

			for (DataCell cell : targetColumnSpec.getDomain().getValues()) {
				String value = cell.toString();
				String pColumn = targetSettings.getProbabilityColumnName(value);

				message.append(pColumn).append(" : ");

				if (spec.containsName(pColumn)) {
					message.append("<font color='green'>\u2714</font> found");
				} else {
					message.append("<font color='red'>\u2718</font> not found");
				}

				message.append("<br>");
			}
		}
		return message.toString();
	}

	private void updateModel() {
		targetSettings.getProbabilityFormatModel().setStringValue(formatInput.getText());
		statusLabel.setVisible(false);
	}

	@Override
	protected void updateComponent() {
		formatInput.setText(targetSettings.getProbabilityFormat());
	}

	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		updateModel();

	}

	@Override
	protected void checkConfigurabilityBeforeLoad(PortObjectSpec[] specs) throws NotConfigurableException {
		// nothing to do
	}

	@Override
	protected void setEnabledComponents(boolean enabled) {
		formatInput.setEnabled(enabled);
		checkBtn.setEnabled(enabled);
	}

	@Override
	public void setToolTipText(String text) {
		formatInput.setToolTipText(text);
	}

}
