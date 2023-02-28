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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.DoubleCell;

import se.redfield.cp.settings.PredictiveSystemsClassifierSettings;
import se.redfield.cp.utils.KnimeUtils;


public class PredictiveSystemsClassifierCellFactory extends AbstractCellFactory {

	private final PredictiveSystemsClassifierSettings settings;
	private final int probabilityDistributionColumnIdx;
	private final int targetColumnIdx;

	private final Random random;

	/**
	 * @param settings The classifier settings.
	 */
	public PredictiveSystemsClassifierCellFactory(String probabilityDistributionColumn,
			PredictiveSystemsClassifierSettings settings, DataTableSpec inputTableSpec) {
		super(createOutputColumnSpec(settings));
		random = new Random();

		this.settings = settings;
		probabilityDistributionColumnIdx = inputTableSpec.findColumnIndex(probabilityDistributionColumn);

		if (settings.hasTargetColumn()) {
			targetColumnIdx = inputTableSpec.findColumnIndex(settings.getTargetColumn());
		} else {
			targetColumnIdx = -1;
		}
	}

	private static DataColumnSpec[] createOutputColumnSpec(PredictiveSystemsClassifierSettings settings) {
		List<DataColumnSpec> columns = new ArrayList<>();

		if (settings.hasTarget()) {
			columns.add(KnimeUtils.createDoubleColumn(String.format("P(cpds<%.2f)", settings.getTarget())));
		}

		if (settings.hasTargetColumn()) {
			columns.add(KnimeUtils.createDoubleColumn(String.format("P(cpds<[%s])", settings.getTargetColumn())));
		}

		for (double p : settings.getLowerPercentiles()) {
			columns.add(KnimeUtils.createDoubleColumn(String.format("%.1f Lower Percentile", p)));
		}

		for (double p : settings.getHigherPercentiles()) {
			columns.add(KnimeUtils.createDoubleColumn(String.format("%.1f Higher Percentile", p)));
		}

		return columns.toArray(new DataColumnSpec[] {});
	}

	@Override
	public DataCell[] getCells(DataRow row) {
		List<Double> probabilityDistribution = ((CollectionDataValue) row.getCell(probabilityDistributionColumnIdx))
				.stream() //
				.map(c -> ((DoubleValue) c).getDoubleValue()) //
				.collect(Collectors.toList());

		List<DataCell> result = new ArrayList<>();

		if (settings.hasTarget()) {
			result.add(new DoubleCell(getTargetValue(settings.getTarget(), probabilityDistribution)));
		}

		if (settings.hasTargetColumn()) {
			double target = ((DoubleValue) row.getCell(targetColumnIdx)).getDoubleValue();
			result.add(new DoubleCell(getTargetValue(target, probabilityDistribution)));
		}

		for (double d : settings.getLowerPercentiles()) {
			result.add(new DoubleCell(getLowerPercentileValue(d, probabilityDistribution)));
		}

		for (double d : settings.getHigherPercentiles()) {
			result.add(new DoubleCell(getHigherPercentileValue(d, probabilityDistribution)));
		}

		return result.toArray(new DataCell[] {});
	}

	private double getTargetValue(double target, List<Double> probabilities) {
		long count = probabilities.stream().filter(p -> p < target).count();
		double gamma = random.nextDouble();

		return (count + gamma) / (probabilities.size() + 1);
	}

	private double getLowerPercentileValue(double percentile, List<Double> probabilities) {
		int index = (int) Math.floor(percentile / 100 * (probabilities.size() + 1)) - 1;

		return getProbability(probabilities, index);
	}

	private double getHigherPercentileValue(double percentile, List<Double> probabilities) {
		int index = (int) Math.ceil(percentile / 100 * (probabilities.size() + 1)) - 1;

		return getProbability(probabilities, index);
	}

	private double getProbability(List<Double> probabilities, int index) {
		if (index < 0) {
			return Double.NEGATIVE_INFINITY;
		} else if (index > probabilities.size() - 1) {
			return Double.POSITIVE_INFINITY;
		} else {
			return probabilities.get(index);
		}
	}
}
