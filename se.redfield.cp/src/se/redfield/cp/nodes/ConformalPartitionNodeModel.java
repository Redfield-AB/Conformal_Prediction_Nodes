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

import se.redfield.cp.core.Partitioner;
import se.redfield.cp.settings.SamplingSettings;

/**
 *
 * The node performs the partitioning of the data in train- and test set.
 */
public class ConformalPartitionNodeModel extends NodeModel {
    /** Outport for training data: 0. */
    static final int OUTPORT_A = 0;

    /** Outport for test data: 1. */
    static final int OUTPORT_B = 1;
    
	/**
	 * The settings key for partitionSettings
	 */
	public static final String KEY_PARTITION_SETTINGS = "partitionSettings";

	private final SamplingSettings partitionSettings = new SamplingSettings();

	private final Partitioner partitioner = new Partitioner(partitionSettings, false);
    
    /**
     * Creates node model, sets outport count to 2.
     */
    public ConformalPartitionNodeModel() {
		super(1, 2);
    }

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec in = inSpecs[0];
		partitionSettings.validate(in);
		return new DataTableSpec[] { in, in };
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		BufferedDataTable[] parts = partitioner.partition(inData[0], exec, true);
		return new BufferedDataTable[] { parts[0], parts[1] };
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		partitionSettings.saveSettingsTo(settings.addNodeSettings(KEY_PARTITION_SETTINGS));
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		partitionSettings.validateSettings(settings.getNodeSettings(KEY_PARTITION_SETTINGS));
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		partitionSettings.loadSettingsFrom(settings.getNodeSettings(KEY_PARTITION_SETTINGS), false);
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
		partitioner.reset();
	}
}
    

	