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

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import se.redfield.cp.settings.KeepColumnsSettings;

/**
 * Base class for classification and regression calibrators.
 * 
 * @author Alexander Bondaletov
 *
 */
public abstract class AbstractCalibrator {

	private KeepColumnsSettings keepColumnsSettings;

	protected AbstractCalibrator(KeepColumnsSettings keepColumnsSettings) {
		this.keepColumnsSettings = keepColumnsSettings;
	}

	/**
	 * Creates output calibration table spec.
	 * 
	 * @param inputTableSpec Input table spec.
	 * @return
	 */
	public DataTableSpec createOutputSpec(DataTableSpec inputTableSpec) {
		ColumnRearranger rearranger = createBaseRearranger(inputTableSpec);
		rearranger.append(createComputedColumn(inputTableSpec));
		rearranger.append(createRankColumn(inputTableSpec));
		return rearranger.createSpec();
	}

	private ColumnRearranger createBaseRearranger(DataTableSpec inputTableSpec) {
		ColumnRearranger rearranger = new ColumnRearranger(inputTableSpec);
		if (!keepColumnsSettings.getKeepAllColumns()) {
			rearranger.keepOnly(getRequiredColumnNames(inputTableSpec));
		}
		return rearranger;
	}

	/**
	 * Processes input table to create calibration table.
	 * 
	 * @param inCalibrationTable Input table.
	 * @param exec               Execution context.
	 * @return
	 * @throws CanceledExecutionException
	 */
	public BufferedDataTable process(BufferedDataTable inCalibrationTable, ExecutionContext exec)
			throws CanceledExecutionException {
		ColumnRearranger appendComputedColumnRearranger = createBaseRearranger(inCalibrationTable.getDataTableSpec());
		appendComputedColumnRearranger.append(createComputedColumn(inCalibrationTable.getDataTableSpec()));

		BufferedDataTable appendedComputedColumnTable = exec.createColumnRearrangeTable(inCalibrationTable,
				appendComputedColumnRearranger, exec.createSubProgress(0.25));

		BufferedDataTableSorter sorter = createSorter(appendedComputedColumnTable);
		BufferedDataTable sortedTable = sorter.sort(exec.createSubExecutionContext(0.5));

		ColumnRearranger appendRankRearranger = new ColumnRearranger(sortedTable.getDataTableSpec());
		appendRankRearranger.append(createRankColumn(sortedTable.getSpec()));

		return exec.createColumnRearrangeTable(sortedTable, appendRankRearranger, exec.createSubProgress(0.25));
	}

	protected abstract String[] getRequiredColumnNames(DataTableSpec inTableSpec);

	protected abstract CellFactory createComputedColumn(DataTableSpec inTableSpec);

	protected abstract CellFactory createRankColumn(DataTableSpec inTableSpec);

	protected abstract BufferedDataTableSorter createSorter(BufferedDataTable table);
}
