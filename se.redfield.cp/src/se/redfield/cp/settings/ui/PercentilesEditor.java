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
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.node.util.SharedIcons;

public class PercentilesEditor extends JPanel {

	private final List<JSpinner> inputs;

	public PercentilesEditor(String title) {
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createTitledBorder(title));

		inputs = new ArrayList<>();
	}

	public double[] getPercentiles() {
		return getPercentilesList().stream().mapToDouble(Double::doubleValue).toArray();
	}

	private List<Double> getPercentilesList() {
		return inputs.stream().map(s -> (Double) s.getValue()).collect(Collectors.toList());
	}

	public void setPercentiles(double[] percentiles) {
		removeAll();
		inputs.clear();

		for (int i = 0; i < percentiles.length; i++) {
			addInputRow(percentiles[i], i, i == percentiles.length - 1);
		}

		JButton bAdd = new JButton("Add", SharedIcons.ADD_PLUS.get());
		bAdd.addActionListener(e->onAdd());

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridwidth = 5;
		c.gridy = percentiles.length;
		c.insets = new Insets(10, 0, 10, 0);
		add(bAdd, c);
	}

	private void addInputRow(double value, int row, boolean last) {
		boolean first = inputs.isEmpty();
		int index = inputs.size();

		JSpinner input = new JSpinner(new SpinnerNumberModel(value, 0, 100, 5));
		inputs.add(input);

		JButton bUp = new JButton(SharedIcons.MOVE_UP.get());
		bUp.setEnabled(!first);
		bUp.addActionListener(e -> moveUp(index));

		JButton bDown = new JButton(SharedIcons.MOVE_DOWN.get());
		bDown.setEnabled(!last);
		bDown.addActionListener(e -> moveDown(index));

		JButton bRemove = new JButton(SharedIcons.DELETE_TRASH.get());
		bRemove.addActionListener(e -> onRemove(index));

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = row;
		c.insets = new Insets(5, 10, 5, 20);
		add(input, c);

		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.gridx += 1;
		c.insets = new Insets(5, 0, 5, 5);
		add(bUp, c);

		c.gridx += 1;
		add(bDown, c);

		c.gridx += 1;
		add(bRemove, c);

		c.gridx += 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		add(Box.createHorizontalGlue(), c);
	}

	private void moveUp(int index) {
		moveTo(index, -1);
	}

	private void moveDown(int index) {
		moveTo(index, 1);
	}

	private void moveTo(int index, int direction) {
		double[] percentiles = getPercentiles();
		double temp = percentiles[index + direction];
		percentiles[index + direction] = percentiles[index];
		percentiles[index] = temp;
		setPercentiles(percentiles);
	}

	private void onRemove(int index) {
		List<Double> percentiles = getPercentilesList();
		percentiles.remove(index);
		setPercentiles(percentiles.stream().mapToDouble(Double::doubleValue).toArray());
	}

	private void onAdd() {
		List<Double> percentiles = getPercentilesList();
		percentiles.add(0.0);
		setPercentiles(percentiles.stream().mapToDouble(Double::doubleValue).toArray());
	}
}
