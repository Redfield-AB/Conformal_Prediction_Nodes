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
package se.redfield.cp.core.scoring;

import java.util.function.ToDoubleFunction;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;

import se.redfield.cp.core.scoring.Scores.Metric;

/**
 * Class represents table column displaying score from the {@link Scores}
 * object.
 * 
 * @author Alexander Bondaletov
 *
 */
public class ScoreColumn {
	private String name;
	private ToDoubleFunction<Scores> getter;

	/**
	 * @param name   The column name.
	 * @param metric The metric.
	 */
	public ScoreColumn(String name, Metric metric) {
		this(name, metric, false);
	}

	/**
	 * @param name    The column name.
	 * @param metric  The metric.
	 * @param average Whether the average value for the metric should be taken.
	 */
	public ScoreColumn(String name, Metric metric, boolean average) {
		this(name, average ? s -> s.getAvg(metric) : s -> s.get(metric));
	}

	/**
	 * @param name   The column name.
	 * @param getter The function to extract value from the {@link Scores} object.
	 */
	public ScoreColumn(String name, ToDoubleFunction<Scores> getter) {
		this.name = name;
		this.getter = getter;
	}

	/**
	 * @return The column spec.
	 */
	public DataColumnSpec createSpec() {
		return new DataColumnSpecCreator(name, getType()).createSpec();
	}

	/**
	 * @param scores The scores object.
	 * @return The data cell.
	 */
	public DataCell createCell(Scores scores) {
		double value = getter.applyAsDouble(scores);
		return createCell(value);
	}

	protected DataType getType() {
		return DoubleCell.TYPE;
	}

	protected DataCell createCell(double value) {
		return new DoubleCell(value);
	}

	/**
	 * {@link ScoreColumn} that outputs values as a {@link LongCell}.
	 * 
	 * @author Alexander Bondaletov
	 *
	 */
	public static class LongScoreColumn extends ScoreColumn {
		/**
		 * @param name   The column name.
		 * @param metric The metric.
		 */
		public LongScoreColumn(String name, Metric metric) {
			super(name, metric);
		}

		/**
		 * @param name   The column name.
		 * @param getter The function to extract value from the {@link Scores} object.
		 */
		public LongScoreColumn(String name, ToDoubleFunction<Scores> getter) {
			super(name, getter);
		}

		@Override
		protected DataType getType() {
			return LongCell.TYPE;
		}

		@Override
		protected DataCell createCell(double value) {
			return new LongCell((long) value);
		}
	}
}
