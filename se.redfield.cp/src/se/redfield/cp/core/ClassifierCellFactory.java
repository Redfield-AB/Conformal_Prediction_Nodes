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

import static java.util.stream.Collectors.toList;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.StringCell;

import se.redfield.cp.settings.ClassifierSettings;

/**
 * CellFactory used to create Classes column. Collects all classes that has
 * P-value greater than selected threshold.
 * 
 * @author Alexander Bondaletov
 *
 */
public class ClassifierCellFactory extends AbstractCellFactory {
	private ClassifierSettings settings;

	/**
	 * @param settings The classifier settings.
	 */
	public ClassifierCellFactory(ClassifierSettings settings) {
		super(createClassColumnSpec(settings));
		this.settings = settings;
	}

	private static DataColumnSpec createClassColumnSpec(ClassifierSettings settings) {
		DataType type = settings.getClassesAsString() ? StringCell.TYPE : SetCell.getCollectionType(StringCell.TYPE);
		return new DataColumnSpecCreator(settings.getClassesColumnName(), type).createSpec();
	}

	@Override
	public DataCell[] getCells(DataRow row) {
		Set<String> classes = new HashSet<>();

		for (Entry<String, Integer> e : settings.getScoreColumns().entrySet()) {
			double score = ((DoubleValue) row.getCell(e.getValue())).getDoubleValue();
			if (score > settings.getErrorRate()) {
				classes.add(e.getKey());
			}
		}

		DataCell result;
		if (classes.isEmpty()) {
			result = new MissingCell("No class asigned");
		} else {
			if (settings.getClassesAsString()) {
				result = new StringCell(String.join(settings.getStringSeparator(), classes));
			} else {
				result = CollectionCellFactory.createSetCell(classes.stream().map(StringCell::new).collect(toList()));
			}
		}

		return new DataCell[] { result };
	}
}
