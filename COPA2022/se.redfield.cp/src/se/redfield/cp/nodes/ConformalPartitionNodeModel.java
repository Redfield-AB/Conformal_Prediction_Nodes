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

import java.util.Collections;
import java.util.Iterator;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList; 
import java.util.Random; 

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.sample.AbstractSamplingNodeModel;
import org.knime.base.node.preproc.sample.SamplingNodeSettings;
import org.knime.base.node.preproc.sample.SamplingNodeSettings.CountMethods;
import org.knime.base.node.preproc.sample.SamplingNodeSettings.SamplingMethods;
import org.knime.base.node.preproc.sample.StratifiedSamplingRowFilter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import se.redfield.cp.Partitioner;

import org.knime.base.node.preproc.sample.*;
/**
 *
 * @author 
 */
public class ConformalPartitionNodeModel extends AbstractSamplingNodeModel {
    /** Outport for training data: 0. */
    static final int OUTPORT_A = 0;

    /** Outport for test data: 1. */
    static final int OUTPORT_B = 1;
    
	public static final String KEY_PARTITION_SETTINGS = "partitionSettings";

	private final SamplingNodeSettings partitionSettings = new SamplingNodeSettings();

	private final Partitioner partitioner = new Partitioner(partitionSettings, false);
    
    /**
     * Creates node model, sets outport count to 2.
     */
    public ConformalPartitionNodeModel() {
        super(2);
    }

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec in = inSpecs[0];
		checkSettings(in);
		return new DataTableSpec[] { in, in };
	}

	/**
	 * Validates sampling settings against input table spec.
	 * 
	 * @param partitionSettings Sampling settings.
	 * @param inSpec            Input table spec.
	 * @throws InvalidSettingsException
	 */
	@Override
	protected void checkSettings(DataTableSpec inSpec) throws InvalidSettingsException {
		if (partitionSettings.countMethod() == null) {
			throw new InvalidSettingsException("No sampling method selected");
		}
		if (partitionSettings.samplingMethod() == SamplingMethods.Stratified
				&& !inSpec.containsName(partitionSettings.classColumn())) {
			throw new InvalidSettingsException(
					"Column '" + partitionSettings.classColumn() + "' for stratified sampling " + "does not exist");
		}
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
		validateSamplingSettings(settings.getNodeSettings(KEY_PARTITION_SETTINGS));
	}

	/**
	 * Validates sampling settings consistency.
	 * 
	 * @param settings Settings to validate.
	 * @throws InvalidSettingsException
	 */
	private void validateSamplingSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		SamplingNodeSettings tmp = new SamplingNodeSettings();
		tmp.loadSettingsFrom(settings, false);

		switch (tmp.countMethod()) {
		case Absolute:
			if (tmp.count() < 0) {
				throw new InvalidSettingsException("Invalid count: " + tmp.count());
			}
			break;
		case Relative:
			if (tmp.fraction() < 0 || tmp.fraction() > 1) {
				throw new InvalidSettingsException("Invalid fraction: " + tmp.fraction());
			}
			break;
		default:
			throw new InvalidSettingsException("Unknown counting method: " + tmp.countMethod());
		}

		if (tmp.samplingMethod() == SamplingMethods.Stratified && tmp.classColumn() == null) {
			throw new InvalidSettingsException("Class column is not selected");
		}
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
    

	