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
package se.redfield.cp.core;

import java.util.Random;

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.sample.LinearSamplingRowFilter;
import org.knime.base.node.preproc.sample.Sampler;
import org.knime.base.node.preproc.sample.SamplingNodeSettings;
import org.knime.base.node.preproc.sample.SamplingNodeSettings.CountMethods;
import org.knime.base.node.preproc.sample.SamplingNodeSettings.SamplingMethods;
import org.knime.base.node.preproc.sample.StratifiedSamplingRowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * Class used to partition provided table based on a given sampling settings.
 *
 */
public class Partitioner {

	private final SamplingNodeSettings settings;
	private final boolean iterationDependent;

	private int iteration;
	private long defaultSeed;

	/**
	 * Creates instance.
	 * 
	 * @param settings           Sampling settings.
	 * @param iterationDependent Flag specifies that partitioning result has to be
	 *                           different on each iteration (only for when
	 *                           stratified or random sampling is selected)
	 */
	public Partitioner(SamplingNodeSettings settings, boolean iterationDependent) {
		this.settings = settings;
		this.iterationDependent = iterationDependent;
		reset();
	}

	/**
	 * Resets current iteratin and default random seed value
	 */
	public void reset() {
		iteration = 0;
		defaultSeed = System.nanoTime();
	}

	/**
	 * Partitions provided table into 2 table based on a sampling settings.
	 * 
	 * @param inTable Input table.
	 * @param exec    Execution context.
	 * @return The result of a partitioning.
	 * @throws CanceledExecutionException
	 */
	public BufferedDataTable[] partition(BufferedDataTable inTable, ExecutionContext exec)
			throws CanceledExecutionException {
		return partition(inTable, exec, false);
	}

	/**
	 * Partitions provided table into 2 table based on a sampling settings.
	 * 
	 * @param inTable  Input table.
	 * @param exec     Execution context.
	 * @param computeK
	 * @return The result of a partitioning.
	 * @throws CanceledExecutionException
	 */
	public BufferedDataTable[] partition(BufferedDataTable inTable, ExecutionContext exec, boolean computeK)
			throws CanceledExecutionException {
		IRowFilter filter = getRowFilter(inTable, exec, computeK);

		BufferedDataContainer matchContainer = exec.createDataContainer(inTable.getDataTableSpec());
		BufferedDataContainer missContainer = exec.createDataContainer(inTable.getDataTableSpec());

		BufferedDataContainer putAllContainer = null;

		long count = 0;
		long totalCount = inTable.size();
		for (DataRow row : inTable) {
			BufferedDataContainer curContainer = null;

			if (putAllContainer == null) {
				try {
					curContainer = filter.matches(row, count) ? matchContainer : missContainer;
				} catch (EndOfTableException e) {
					putAllContainer = missContainer;
				} catch (IncludeFromNowOn e) {
					putAllContainer = matchContainer;
				}
			}

			if (putAllContainer != null) {
				curContainer = putAllContainer;
			}

			curContainer.addRowToTable(row);// NOSONAR curContainer is never null
			count += 1;

			exec.checkCanceled();
			exec.setProgress((double) count / totalCount);
		}

		matchContainer.close();
		missContainer.close();
		iteration += 1;
		return new BufferedDataTable[] { matchContainer.getTable(), missContainer.getTable() };
	}

	/**
	 * Creates {@link IRowFilter} instance based on a sampling settings
	 * 
	 * @param inTable Input table.
	 * @param exec    Execution context.
	 * @return
	 * @throws CanceledExecutionException
	 */
	private IRowFilter getRowFilter(BufferedDataTable inTable, ExecutionContext exec, boolean computeK)
			throws CanceledExecutionException {
		Random rand = getRandomInstance();

		int rowCount;
		if (settings.countMethod() == CountMethods.Relative) {
			rowCount = (int) (settings.fraction() * inTable.size());
		} else {
			rowCount = settings.count();
		}
		if (computeK)
			rowCount = computeK(rowCount);

		switch (settings.samplingMethod()) {
		case First:
			return Sampler.createRangeFilter(rowCount);
		case Linear:
			return new LinearSamplingRowFilter(inTable.size(), rowCount);
		case Random:
			return Sampler.createSampleFilter(inTable, rowCount, rand, exec);
		case Stratified:
			return new StratifiedSamplingRowFilter(inTable, settings.classColumn(), rowCount, rand, exec);
		default:
			throw new UnsupportedOperationException("Unknown sampling method: " + settings.samplingMethod());
		}
	}

	/**
	 * Creates {@link Random} instance. Uses seed from sampling settings of default
	 * seed value. If iterationDependent flag is set current iteration is added to
	 * the seed value to produce different results on each iteration
	 */
	private Random getRandomInstance() {
		if (settings.samplingMethod() == SamplingMethods.Random
				|| settings.samplingMethod() == SamplingMethods.Stratified) {
			long seed = settings.seed() != null ? settings.seed() : defaultSeed;
			if (iterationDependent) {
				seed += iteration;
			}
			return new Random(seed);
		}
		return null;
	}

	protected int computeK(int rowCount) {
		if (rowCount < 10)
			throw new UnsupportedOperationException("Calibration size cannot be smaller than 10.");

		int div = rowCount < 100 ? 10 : 100;
		int k = (rowCount + 1) / div;

		return k * div - 1;
	}

}
