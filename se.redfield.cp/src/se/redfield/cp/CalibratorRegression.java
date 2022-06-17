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
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import se.redfield.cp.nodes.AbstractConformalPredictorRegressionNodeModel;
import se.redfield.cp.nodes.CompactConformalRegressionNodeModel;
import se.redfield.cp.nodes.ConformalPredictorCalibratorRegressionNodeModel;

/**
 * Class used by Conformal Calibrator Node to process input table into output
 * calibration table.
 *
 */
public class CalibratorRegression {

//	private ConformalPredictorCalibratorRegressionNodeModel model;
	private AbstractConformalPredictorRegressionNodeModel model;

	/**
	 * Creates instance
	 * 
	 * @param model
	 */
	public CalibratorRegression(ConformalPredictorCalibratorRegressionNodeModel model) {
		this.model = (AbstractConformalPredictorRegressionNodeModel) model;
	}

	/**
	 * Creates instance
	 * 
	 * @param model
	 */
	public CalibratorRegression(CompactConformalRegressionNodeModel model) {
		this.model = (AbstractConformalPredictorRegressionNodeModel) model;
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
		rearranger.append(createNonconformityCellFactory(inputTableSpec));
		rearranger.append(createScoreCellFactory(inputTableSpec));
		return rearranger.createSpec();
	}

	/**
	 * Processes input table to sorted alpha scores for calibration set. Alpha score 
	 * column (default nonconformity is the absolute error) is appended to table.
	 * 
	 * @param inCalibrationTable Input table.
	 * @param exec               Execution context.
	 * @return
	 * @throws CanceledExecutionException
	 */
	public BufferedDataTable process(BufferedDataTable inCalibrationTable, ExecutionContext exec)
			throws CanceledExecutionException {
		
		// collect targets and predictions and setup return table
		ColumnRearranger appendProbabilityRearranger = new ColumnRearranger(inCalibrationTable.getDataTableSpec());

		if (!model.getKeepAllColumns()) {
			appendProbabilityRearranger.keepOnly(model.getRequiredColumnNames(inCalibrationTable.getDataTableSpec()));
		}
		//changed to createNonConformityCellFactory
		appendProbabilityRearranger.append(createNonconformityCellFactory(inCalibrationTable.getDataTableSpec()));

		BufferedDataTable appendedProbabilityTable = exec.createColumnRearrangeTable(inCalibrationTable,
				appendProbabilityRearranger, exec.createSubProgress(0.25));

		BufferedDataTableSorter sorter = new BufferedDataTableSorter(appendedProbabilityTable,
				Arrays.asList(model.getCalibrationAlphaColumnName()),
				new boolean[] { false });
		BufferedDataTable sortedTable = sorter.sort(exec.createSubExecutionContext(0.5));

		ColumnRearranger appendScoreRearranger = new ColumnRearranger(sortedTable.getDataTableSpec());
		appendScoreRearranger.append(createScoreCellFactory(sortedTable.getSpec()));

		return exec.createColumnRearrangeTable(sortedTable, appendScoreRearranger, exec.createSubProgress(0.25));
	} 

	/**
	 * Creates cell factory that appends the nonconformity column to input table.
	 * 
	 * @param inputTableSpec Input table spec.
	 * @return
	 */
	private CellFactory createNonconformityCellFactory(DataTableSpec inputTableSpec) {
		int targetColumnIndex = inputTableSpec.findColumnIndex(model.getTargetColumnName()); // get target column
		int predictionColumnIndex = inputTableSpec.findColumnIndex(model.getPredictionColumnName()); // get prediction column
		int sigmaColumnIndex = inputTableSpec.findColumnIndex(model.getSigmaColumnName());

		return new AbstractCellFactory(
				new DataColumnSpecCreator(model.getCalibrationAlphaColumnName(), DoubleCell.TYPE).createSpec()) {

			@Override
			public DataCell[] getCells(DataRow row) {
				DataCell targetDataCell = row.getCell(targetColumnIndex);
				if (targetDataCell.isMissing()) {
					throw new MissingValueException((MissingValue) targetDataCell, "Target column contains missing values");
				}
				Double dTarget = getDoubleValueFromCell(targetDataCell, "Target column is not a numeric column");				

				DataCell predictionDataCell = row.getCell(predictionColumnIndex);
				if (predictionDataCell.isMissing()) {
					throw new MissingValueException((MissingValue) predictionDataCell, "Prediction column contains missing values");
				}
				Double dPrediction = getDoubleValueFromCell(predictionDataCell, "Prediction column is not double column");
				
				double nonconformityScore = 0;
				if (model.getNormalized()) {
					DataCell sigmaDataCell = row.getCell(sigmaColumnIndex);
					if (predictionDataCell.isMissing()) {
						throw new MissingValueException((MissingValue) sigmaDataCell, "Sigma column contains missing values");
					}
					Double dSigma = getDoubleValueFromCell(sigmaDataCell, "Sigma column is not double column");
					nonconformityScore = Math.abs(dTarget - dPrediction) / (dSigma + model.getBeta());
				} else
					nonconformityScore = Math.abs(dTarget - dPrediction);
				
				return new DataCell[] { new DoubleCell(nonconformityScore) };
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
		return new AbstractCellFactory(
				new DataColumnSpecCreator(model.getCalibrationRankColumnName(), LongCell.TYPE).createSpec()) {

			private long counter = 0;

			@Override
			public DataCell[] getCells(DataRow row) {				
				return new DataCell[] { new LongCell(counter++) };
			}
		};
	}
	
	private double getDoubleValueFromCell(DataCell cell, String errorMessage) {
		if (cell.getType().getCellClass().equals((DoubleCell.class))) {
			// Cast the cell as we know is must be a DoubleCell.
			return ((DoubleCell) cell).getDoubleValue();					
		} else if (cell.getType().getCellClass().equals((IntCell.class))) {
			// Cast the cell as we know is must be a IntCell.
			return (double) ((IntCell) cell).getIntValue();				
		} else if (cell.getType().getCellClass().equals((LongCell.class))) {
			// Cast the cell as we know is must be a LongCell.
			return ((LongCell) cell).getDoubleValue();				
		} else {
			throw new MissingValueException((MissingValue) cell, errorMessage);
		}
	}
}
