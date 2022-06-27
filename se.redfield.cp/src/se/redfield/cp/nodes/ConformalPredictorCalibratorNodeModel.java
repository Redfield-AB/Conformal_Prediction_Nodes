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

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import se.redfield.cp.Calibrator;
import se.redfield.cp.settings.CalibratorNodeSettings;
import se.redfield.cp.utils.PortDef;

/**
 * Conformal Calibrator node. Assign ranks to each row bases on prediction
 * probability.
 *
 */
public class ConformalPredictorCalibratorNodeModel extends NodeModel {
	public static final PortDef PORT_INPUT_TABLE = new PortDef(0, "Input table");

	private final CalibratorNodeSettings settings = new CalibratorNodeSettings();

	private final Calibrator calibrator = new Calibrator(settings);

	protected ConformalPredictorCalibratorNodeModel() {
		super(1, 1);
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		if (settings.getTargetColumnName().isEmpty()) {
			attemptAutoconfig(inSpecs);
		}

		settings.validateSettings(inSpecs);

		return new DataTableSpec[] { calibrator.createOutputSpec(inSpecs[PORT_INPUT_TABLE.getIdx()]) };
	}

	private void attemptAutoconfig(DataTableSpec[] inSpecs) {
		String[] columnNames = inSpecs[PORT_INPUT_TABLE.getIdx()].getColumnNames();
		for (String column : columnNames) {
			try {
				settings.getTargetColumnModel().setStringValue(column);
				settings.validateSettings(inSpecs);
				setWarningMessage(String.format("Node autoconfigured with '%s' column", column));
				return;
			} catch (InvalidSettingsException e) {
				settings.getTargetColumnModel().setStringValue("");
			}
		}
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		return new BufferedDataTable[] { calibrator.process(inData[PORT_INPUT_TABLE.getIdx()], exec) };
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		this.settings.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		this.settings.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		this.settings.loadSettingFrom(settings);
	}

	@Override
	protected void reset() {
		// nothing to do
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no internals
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no internals
	}
}
