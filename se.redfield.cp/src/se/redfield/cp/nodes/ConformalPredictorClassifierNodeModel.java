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
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableOperator;

import se.redfield.cp.core.ClassifierCellFactory;
import se.redfield.cp.settings.ClassifierSettings;

/**
 * Conformal Classifier node. Assigns predicted classes to each row based on
 * it's P-values and selected Significance Level. Works with classes column
 * represented as Collection or String column
 *
 */
public class ConformalPredictorClassifierNodeModel extends NodeModel {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(ConformalPredictorClassifierNodeModel.class);

	private final ClassifierSettings settings = new ClassifierSettings();

	private ColumnRearranger rearranger;

	protected ConformalPredictorClassifierNodeModel() {
		super(1, 1);
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		pushFlowVariableDouble(ClassifierSettings.KEY_ERROR_RATE, settings.getErrorRate());
		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inData[0], rearranger, exec) };
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec inSpec = inSpecs[0];

		settings.configure(inSpec);
		rearranger = createRearranger(inSpec);

		return new DataTableSpec[] { rearranger.createSpec() };
	}

	/**
	 * Creates ColumnRearranger
	 * 
	 * @param inSpec Input table spec
	 * @return rearranger
	 */
	private ColumnRearranger createRearranger(DataTableSpec inSpec) {
		ColumnRearranger r = new ColumnRearranger(inSpec);
		r.append(new ClassifierCellFactory(settings));
		return r;
	}

	@Override
	public StreamableOperator createStreamableOperator(PartitionInfo partitionInfo, PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		return rearranger.createStreamableFunction();
	}

	@Override
	public InputPortRole[] getInputPortRoles() {
		return new InputPortRole[] { InputPortRole.DISTRIBUTED_STREAMABLE };
	}

	@Override
	public OutputPortRole[] getOutputPortRoles() {
		return new OutputPortRole[] { OutputPortRole.DISTRIBUTED };
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
		this.settings.loadSettingsFrom(settings);
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

	@Override
	protected void reset() {
		rearranger = null;
	}

}
