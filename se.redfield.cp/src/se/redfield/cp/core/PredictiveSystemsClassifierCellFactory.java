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

import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.AbstractCellFactory;

import se.redfield.cp.settings.PredictiveSystemsRegressionSettings;

/**
 * CellFactory used to create Classes column. Collects all classes that has
 * P-value greater than selected threshold.
 * 
 * @author Alexander Bondaletov
 *
 */
public class PredictiveSystemsClassifierCellFactory extends AbstractCellFactory {
	private PredictiveSystemsRegressionSettings settings;

	/**
	 * @param settings The classifier settings.
	 */
	public PredictiveSystemsClassifierCellFactory(PredictiveSystemsRegressionSettings settings) {
		super(createOutputColumnSpec(settings));
		this.settings = settings;
	}

	private static DataColumnSpec createOutputColumnSpec(PredictiveSystemsRegressionSettings settings) {
		// TODO see comments to getCells below.
		// DataType type = settings.getClassesAsString() ? StringCell.TYPE :
		// SetCell.getCollectionType(StringCell.TYPE);
		return null;// new DataColumnSpecCreator(settings.getClassesColumnName(),
					// type).createSpec();
	}

	@Override
	public DataCell[] getCells(DataRow row) {
		Set<String> classes = new HashSet<>();

		// TODO
		// if y-column is selected or fixed y-value assigned, then output p-values based
		// on
		// these

		// TODO
		// if lower and upper percentiles are provided, output one column per provided
		// percentile.

		DataCell result;
		// TODO
		result = new MissingCell("No class asigned");

		return new DataCell[] { result };
	}
}
