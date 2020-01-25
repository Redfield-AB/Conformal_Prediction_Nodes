package se.redfield.cp;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import se.redfield.cp.nodes.ConformalPredictorCalibratorNodeModel;

public class Calibrator {

	private ConformalPredictorCalibratorNodeModel model;

	public Calibrator(ConformalPredictorCalibratorNodeModel model) {
		this.model = model;
	}

	public DataTableSpec createOutputSpec(DataTableSpec inputTableSpec) {
		ColumnRearranger rearranger = new ColumnRearranger(inputTableSpec);
		if (!model.getKeepAllColumns()) {
			rearranger.keepOnly(model.getRequiredColumnNames(inputTableSpec));
		}
		rearranger.append(createPCellFactory(inputTableSpec));
		rearranger.append(createScoreCellFactory(inputTableSpec));
		return rearranger.createSpec();
	}

	public BufferedDataTable process(BufferedDataTable inCalibrationTable, ExecutionContext exec)
			throws CanceledExecutionException {
		ColumnRearranger appendProbabilityRearranger = new ColumnRearranger(inCalibrationTable.getDataTableSpec());

		if (!model.getKeepAllColumns()) {
			appendProbabilityRearranger.keepOnly(model.getRequiredColumnNames(inCalibrationTable.getDataTableSpec()));
		}
		appendProbabilityRearranger.append(createPCellFactory(inCalibrationTable.getDataTableSpec()));

		BufferedDataTable appendedProbabilityTable = exec.createColumnRearrangeTable(inCalibrationTable,
				appendProbabilityRearranger, exec);

		BufferedDataTableSorter sorter = new BufferedDataTableSorter(appendedProbabilityTable,
				Arrays.asList(model.getSelectedColumnName(), model.getCalibrationProbabilityColumnName()),
				new boolean[] { true, false });
		BufferedDataTable sortedTable = sorter.sort(exec);

		ColumnRearranger appendScoreRearranger = new ColumnRearranger(sortedTable.getDataTableSpec());
		appendScoreRearranger.append(createScoreCellFactory(sortedTable.getSpec()));

		return exec.createColumnRearrangeTable(sortedTable, appendScoreRearranger, exec);
	}

	private CellFactory createPCellFactory(DataTableSpec inputTableSpec) {
		int columnIndex = inputTableSpec.findColumnIndex(model.getSelectedColumnName());
		Map<String, Integer> probabilityColumns = inputTableSpec.getColumnSpec(columnIndex).getDomain().getValues()
				.stream().map(DataCell::toString).collect(Collectors.toMap(str -> str,
						str -> inputTableSpec.findColumnIndex(model.getProbabilityColumnName(str))));

		return new AbstractCellFactory(
				new DataColumnSpecCreator(model.getCalibrationProbabilityColumnName(), DoubleCell.TYPE).createSpec()) {

			@Override
			public DataCell[] getCells(DataRow row) {
				DataCell dataCell = row.getCell(columnIndex);
				Integer probabilityCol = probabilityColumns.get(dataCell.toString());

				return new DataCell[] { row.getCell(probabilityCol) };
			}
		};
	}

	private CellFactory createScoreCellFactory(DataTableSpec inputTableSpec) {
		int columnIndex = inputTableSpec.findColumnIndex(model.getSelectedColumnName());

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
