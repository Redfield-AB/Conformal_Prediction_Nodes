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
package se.redfield.cp;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import se.redfield.cp.nodes.AbstractConformalPredictorNodeModel;
import se.redfield.cp.nodes.ConformalPredictorCalibratorNodeModel;

/**
 * Class used by Conformal Calibrator Node to process input table into output
 * calibration table.
 *
 */
public class Calibrator {

	private AbstractConformalPredictorNodeModel model;

	/**
	 * Creates instance
	 * 
	 * @param model
	 */
	public Calibrator(AbstractConformalPredictorNodeModel model) {
		this.model = model;
	}

	/**
	 * Creates output calibration table spec.
	 * 
	 * @param inputTableSpec Input table spec.
	 * @return
	 */
	public DataTableSpec createOutputSpec(DataTableSpec inputTableSpec) {
		ColumnRearranger rearranger = new ColumnRearranger(inputTableSpec);
		if (!model.getKeepAllColumns()) {
			rearranger.keepOnly(model.getRequiredColumnNames(inputTableSpec));
		}
		rearranger.append(createPCellFactory(inputTableSpec));
		rearranger.append(createScoreCellFactory(inputTableSpec));
		return rearranger.createSpec();
	}

	/**
	 * Processes input table to create calibration table. P column (probability for
	 * a target class) is appended to table and then ranks are assigned based on P
	 * column value.
	 * 
	 * @param inCalibrationTable Input table.
	 * @param exec               Execution context.
	 * @return
	 * @throws CanceledExecutionException
	 */
	public BufferedDataTable process(BufferedDataTable inCalibrationTable, ExecutionContext exec)
			throws CanceledExecutionException {
		ColumnRearranger appendProbabilityRearranger = new ColumnRearranger(inCalibrationTable.getDataTableSpec());

		if (!model.getKeepAllColumns()) {
			appendProbabilityRearranger.keepOnly(model.getRequiredColumnNames(inCalibrationTable.getDataTableSpec()));
		}
		appendProbabilityRearranger.append(createPCellFactory(inCalibrationTable.getDataTableSpec()));

		BufferedDataTable appendedProbabilityTable = exec.createColumnRearrangeTable(inCalibrationTable,
				appendProbabilityRearranger, exec.createSubProgress(0.25));

		BufferedDataTableSorter sorter = new BufferedDataTableSorter(appendedProbabilityTable,
				Arrays.asList(model.getTargetColumnName(), model.getCalibrationProbabilityColumnName()),
				new boolean[] { true, false });
		BufferedDataTable sortedTable = sorter.sort(exec.createSubExecutionContext(0.5));

		ColumnRearranger appendScoreRearranger = new ColumnRearranger(sortedTable.getDataTableSpec());
		appendScoreRearranger.append(createScoreCellFactory(sortedTable.getSpec()));

		return exec.createColumnRearrangeTable(sortedTable, appendScoreRearranger, exec.createSubProgress(0.25));
	}

	/**
	 * Creates cell factory that appends P column to input table.
	 * 
	 * @param inputTableSpec Input table spec.
	 * @return
	 */
	private CellFactory createPCellFactory(DataTableSpec inputTableSpec) {
		int columnIndex = inputTableSpec.findColumnIndex(model.getTargetColumnName());
		Map<String, Integer> probabilityColumns = inputTableSpec.getColumnSpec(columnIndex).getDomain().getValues()
				.stream().map(DataCell::toString).collect(Collectors.toMap(str -> str,
						str -> inputTableSpec.findColumnIndex(model.getProbabilityColumnName(str))));

		return new AbstractCellFactory(
				new DataColumnSpecCreator(model.getCalibrationProbabilityColumnName(), DoubleCell.TYPE).createSpec()) {

			@Override
			public DataCell[] getCells(DataRow row) {
				DataCell dataCell = row.getCell(columnIndex);
				if (dataCell.isMissing()) {
					throw new MissingValueException((MissingValue) dataCell, "Target column contains missing values");
				}
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
	private CellFactory createScoreCellFactory(DataTableSpec inputTableSpec) {
		int columnIndex = inputTableSpec.findColumnIndex(model.getTargetColumnName());

		return new AbstractCellFactory(
				new DataColumnSpecCreator(model.getCalibrationRankColumnName(), LongCell.TYPE).createSpec()) {

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
}
