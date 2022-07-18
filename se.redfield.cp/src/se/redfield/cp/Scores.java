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
package se.redfield.cp;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Class to collect scores for a different metrics.
 * 
 * @author Alexander Bondaletov
 *
 */
public class Scores {
	private Map<Metric, Double> metrics;

	/**
	 * Creates new instance.
	 */
	public Scores() {
		this.metrics = new EnumMap<>(Metric.class);
	}

	/**
	 * Sets the value for a given metric.
	 * 
	 * @param m     The metric.
	 * @param value The value.
	 */
	public void set(Metric m, double value) {
		this.metrics.put(m, value);
	}

	/**
	 * Increments current value for the given metric by 1. Sets the value to 1 if
	 * metric is not present.
	 * 
	 * @param m The metric.
	 */
	public void inc(Metric m) {
		metrics.compute(m, (k, v) -> v == null ? 1.0 : v + 1);
	}

	/**
	 * Adds given value to the given metric. If metric is not present then
	 * initializes it with the given value.
	 * 
	 * @param m     The metric.
	 * @param value The value.
	 */
	public void add(Metric m, double value) {
		metrics.compute(m, (k, v) -> v == null ? value : v + value);
	}

	/**
	 * Computes minimum between the present metric's value and the given value and
	 * stores it.
	 * 
	 * @param m     The metric.
	 * @param value The value.
	 */
	public void min(Metric m, double value) {
		metrics.compute(m, (k, v) -> v == null || value < v ? value : v);
	}

	/**
	 * Computes maximum between the present metric's value and the given value and
	 * stores it.
	 * 
	 * @param m     The metric.
	 * @param value The value.
	 */
	public void max(Metric m, double value) {
		metrics.compute(m, (k, v) -> v == null || value > v ? value : v);
	}

	/**
	 * @param m The metric.
	 * @return The value for the given metric.
	 */
	public double get(Metric m) {
		return metrics.getOrDefault(m, 0.0);
	}

	/**
	 * @param m The metric.
	 * @return The value for the given metric divided by count.
	 */
	public double getAvg(Metric m) {
		return get(m) / get(Metric.COUNT);
	}

	/**
	 * Sums given scores into one scores object.
	 * 
	 * @param scores The scores to combine.
	 * @return The combined scores.
	 */
	public static Scores sum(Collection<? extends Scores> scores) {
		Scores result = new Scores();

		for (Scores s : scores) {
			for (Metric m : s.metrics.keySet()) {
				result.add(m, s.get(m));
			}
		}

		return result;
	}

	/**
	 * The scores associated with a single class.
	 * 
	 * @author Alexander Bondaletov
	 *
	 */
	public static class ClassScores extends Scores {
		private String target;

		/**
		 * @param target The class value
		 */
		public ClassScores(String target) {
			super();
			this.target = target;
		}

		/**
		 * @return The class value.
		 */
		public String getTarget() {
			return target;
		}
	}

	@SuppressWarnings("javadoc")
	public enum Metric {
		COUNT, STRICT_MATCH, SOFT_MATCH, ERROR, SINGLE_CLASS, NULL_CLASS, SUM_OF_P_VALUES, UNCONFIDENCE, FUZZINESS,
		OBSERVED_UNCONFIDENCE, OBSERVED_FUZZINESS, NUMBER_OF_LABELS, MULTIPLE, EXCESS, OBSERVED_MULTIPLE,
		OBSERVED_EXCESS, VALID, INTERVAL_SIZE, MEDIAN_INTERVAL_SIZE, MAX_INTERVAL_SIZE, MIN_INTERVAL_SIZE
	}
}
