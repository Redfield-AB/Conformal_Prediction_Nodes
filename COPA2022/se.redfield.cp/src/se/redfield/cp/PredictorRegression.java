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

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

//import se.redfield.cp.PredictorRegression.ScoreCellFactory;
import se.redfield.cp.nodes.ConformalPredictorRegressionNodeModel;

/**
 * Class used by Conformal Predictor node to process input table and calculate
 * Rank and P-value for each row based on the calibration table probabilities.
 *
 */
public class PredictorRegression {

	private ConformalPredictorRegressionNodeModel model;

	public PredictorRegression(ConformalPredictorRegressionNodeModel model) {
		this.model = model;
	}

	/**
	 * Creates output table spec.
	 * 
	 * @param inPredictionTableSpecs Input prediction table spec.
	 * @return
	 */
	public DataTableSpec createOuputTableSpec(DataTableSpec inCalibrationTableSpecs, DataTableSpec inPredictionTableSpecs) {
		ColumnRearranger r = new ColumnRearranger(inPredictionTableSpecs);
		if (!model.getKeepAllColumns()) {
			r.keepOnly(model.getRequiredColumnNames(inPredictionTableSpecs));
		}
		r.append(createPredictionLowerBoundCellFactory(inPredictionTableSpecs,null));
		r.append(createPredictionUpperBoundCellFactory(inPredictionTableSpecs,null));
		return r.createSpec();
	}

	/**
	 * Creates column rearranger that is used to process input prediction table.
	 * 
	 * @param predictionTableSpec Input prediction table spec.
	 * @param inCalibrationTable  Input calibration table.
	 * @param exec                Execution context.
	 * @return
	 * @throws CanceledExecutionException
	 */
	public ColumnRearranger createRearranger(DataTableSpec predictionTableSpec, BufferedDataTable inCalibrationTable,
			ExecutionContext exec) throws CanceledExecutionException {
		ColumnRearranger r = new ColumnRearranger(predictionTableSpec);

		if (!model.getKeepAllColumns()) {
			r.keepOnly(model.getRequiredColumnNames(predictionTableSpec));
		}

		r.append(createPredictionLowerBoundCellFactory(predictionTableSpec, inCalibrationTable));
		r.append(createPredictionUpperBoundCellFactory(predictionTableSpec, inCalibrationTable));
		
		return r;
	}
	
	private CellFactory createPredictionLowerBoundCellFactory(DataTableSpec inputTableSpec, BufferedDataTable inCalibrationTable) {
		return createPredictionIntervalCellFactory(inputTableSpec, inCalibrationTable, true);
	}
	
	private CellFactory createPredictionUpperBoundCellFactory(DataTableSpec inputTableSpec, BufferedDataTable inCalibrationTable) {
		return createPredictionIntervalCellFactory(inputTableSpec, inCalibrationTable, false);
	}

	/**
	 * Creates cell factory that appends the nonconformity column to input table.
	 * 
	 * @param inputTableSpec Input table spec.
	 * @return
	 */
	private CellFactory createPredictionIntervalCellFactory(DataTableSpec inputTableSpec, BufferedDataTable inCalibrationTable, boolean isLower) {
		int predictionColumnIndex = inputTableSpec.findColumnIndex(model.getPredictionColumnName()); // get prediction column	
		int sigmaColumnIndex = inputTableSpec.findColumnIndex(model.getSigmaColumnName());

		String columnName; 
		if (isLower) {
			columnName = model.getLowerBoundColumnName();
		} else {
			columnName = model.getUpperBoundColumnName();
		}
		
		return new AbstractCellFactory(
				new DataColumnSpecCreator(columnName, DoubleCell.TYPE).createSpec()) {

			@Override
			public DataCell[] getCells(DataRow row) {
				int alphaColumnIndex = 0;
				double alpha = 0;//Get the alpha from the (significant level)'th percentile among the calibration instances
				if (inCalibrationTable != null) {
					alphaColumnIndex = inCalibrationTable.getDataTableSpec().findColumnIndex(model.getCalibrationAlphaColumnName()); // get target column
					alpha = getAlpha(alphaColumnIndex, inCalibrationTable, model.getErrorRate());
				}			

				DataCell predictionDataCell = row.getCell(predictionColumnIndex);
				if (predictionDataCell.isMissing()) {
					throw new MissingValueException((MissingValue) predictionDataCell, "Prediction column contains missing values");
				}
				Double dPrediction = getDoubleValueFromCell(predictionDataCell, "Prediction column is not double column");
				
				double bound;

				if (model.getNormalized()) {
					DataCell sigmaDataCell = row.getCell(sigmaColumnIndex);
					if (predictionDataCell.isMissing()) {
						throw new MissingValueException((MissingValue) sigmaDataCell, "Sigma column contains missing values");
					}
					Double dSigma = getDoubleValueFromCell(sigmaDataCell, "Sigma column is not double column");					

					if (isLower) 
						bound = dPrediction - alpha*(dSigma + model.getBeta());
					else
						bound = dPrediction + alpha*(dSigma + model.getBeta());
				} else {
					if (isLower) 
						bound = dPrediction - alpha;
					else
						bound = dPrediction + alpha;
				}
				return new DataCell[] { new DoubleCell(bound) };
			}
		};
	}

	private double getAlpha(int alphaColumnIndex, BufferedDataTable inCalibrationTable, double errorRate) {
		List<Double> alphas = new ArrayList<Double>();
		CloseableRowIterator rowIterator = inCalibrationTable.iterator();
		while (rowIterator.hasNext()) {
			DataRow currentRow = rowIterator.next();
			DataCell cell = currentRow.getCell(alphaColumnIndex);
			//Since we created the alpha column as DoubleCells, it should be DoubleCell 
			if (cell.getType().getCellClass().equals((DoubleCell.class))) {
				DoubleCell doubleCell = (DoubleCell) cell;
				alphas.add(doubleCell.getDoubleValue());
			}
		}
		int index = (int) (alphas.size() * errorRate);		
		
		return alphas.get(index);
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
