package se.redfield.cp;

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

public class Partitioner {

	private final SamplingNodeSettings settings;
	private final boolean iterationDependent;

	private int iteration;
	private long defaultSeed;

	public Partitioner(SamplingNodeSettings settings, boolean iterationDependent) {
		this.settings = settings;
		this.iterationDependent = iterationDependent;
		reset();
	}

	public void reset() {
		iteration = 0;
		defaultSeed = System.nanoTime();
	}

	public BufferedDataTable[] partition(BufferedDataTable inTable, ExecutionContext exec)
			throws CanceledExecutionException {
		IRowFilter filter = getRowFilter(inTable, exec);

		BufferedDataContainer matchContainer = exec.createDataContainer(inTable.getDataTableSpec());
		BufferedDataContainer missContainer = exec.createDataContainer(inTable.getDataTableSpec());

		BufferedDataContainer putAllContainer = null;

		long count = 0;
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

			curContainer.addRowToTable(row);
			count += 1;
		}

		matchContainer.close();
		missContainer.close();
		iteration += 1;
		return new BufferedDataTable[] { matchContainer.getTable(), missContainer.getTable() };
	}

	private IRowFilter getRowFilter(BufferedDataTable inTable, ExecutionContext exec)
			throws CanceledExecutionException {
		Random rand = getRandomInstance();

		int rowCount;
		if (settings.countMethod() == CountMethods.Relative) {
			rowCount = (int) (settings.fraction() * inTable.size());
		} else {
			rowCount = settings.count();
		}

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
}
