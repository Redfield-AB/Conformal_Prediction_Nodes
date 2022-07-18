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
package se.redfield.cp.core.calibration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;

import se.redfield.cp.settings.CalibratorSettings;
import se.redfield.cp.settings.TargetSettings;
import se.redfield.cp.utils.KnimeUtils;

/**
 * Class used by Conformal Calibrator Node to process input table into output
 * calibration table.
 *
 */
public class Calibrator extends AbstractCalibrator {

	private CalibratorSettings settings;

	/**
	 * Creates instance
	 * 
	 * @param settings
	 */
	public Calibrator(CalibratorSettings settings) {
		super(settings.getKeepColumns());
		this.settings = settings;
	}

	@Override
	protected CellFactory createComputedColumn(DataTableSpec inTableSpec) {
		return createPCellFactory(inTableSpec);
	}

	/**
	 * Creates cell factory that appends P column to input table.
	 * 
	 * @param inputTableSpec Input table spec.
	 * @return
	 */
	private CellFactory createPCellFactory(DataTableSpec inputTableSpec) {
		TargetSettings targetSettings = settings.getTargetSettings();
		int columnIndex = inputTableSpec.findColumnIndex(targetSettings.getTargetColumn());
		Map<String, Integer> probabilityColumns = inputTableSpec.getColumnSpec(columnIndex).getDomain().getValues()
				.stream().map(DataCell::toString).collect(Collectors.toMap(str -> str,
						str -> inputTableSpec.findColumnIndex(targetSettings.getProbabilityColumnName(str))));

		return new AbstractCellFactory(
				new DataColumnSpecCreator(settings.getCalibrationProbabilityColumnName(), DoubleCell.TYPE)
						.createSpec()) {

			@Override
			public DataCell[] getCells(DataRow row) {
				DataCell dataCell = KnimeUtils.nonMissing(row.getCell(columnIndex),
						"Target column contains missing values");
				Integer probabilityCol = probabilityColumns.get(dataCell.toString());

				return new DataCell[] { row.getCell(probabilityCol) };
			}
		};
	}

	/**
	 * Creates cell factory that appends ranks column. Rank is an index row has
	 * inside each target's group sorted by probability column.
	 * 
	 * @param inputTableSpec Input table spec.
	 * @return
	 */
	@Override
	protected CellFactory createRankColumn(DataTableSpec inputTableSpec) {
		int columnIndex = inputTableSpec.findColumnIndex(settings.getTargetSettings().getTargetColumn());

		return new AbstractCellFactory(
				new DataColumnSpecCreator(settings.getCalibrationRankColumnName(), LongCell.TYPE).createSpec()) {

			private long counter = 0;
			private String prevValue = null;

			@Override
			public DataCell[] getCells(DataRow row) {
				String value = row.getCell(columnIndex).toString();
				if (prevValue == null || !prevValue.equals(value)) {
					counter = 0;
					prevValue = value;
				}

				return new DataCell[] { new LongCell(counter++) };
			}
		};
	}

	@Override
	protected String[] getRequiredColumnNames(DataTableSpec spec) {
		List<String> columns = spec.getColumnSpec(settings.getTargetSettings().getTargetColumn()).getDomain()
				.getValues().stream().map(c -> settings.getTargetSettings().getProbabilityColumnName(c.toString()))
				.collect(Collectors.toList());
		columns.add(settings.getTargetSettings().getTargetColumn());
		if (settings.getKeepColumns().getKeepIdColumn()) {
			columns.add(settings.getKeepColumns().getIdColumn());
		}
		return columns.toArray(new String[] {});
	}

	@Override
	protected BufferedDataTableSorter createSorter(BufferedDataTable table) {
		return new BufferedDataTableSorter(table, Arrays.asList(settings.getTargetSettings().getTargetColumn(),
				settings.getCalibrationProbabilityColumnName()), new boolean[] { true, false });
	}
}
