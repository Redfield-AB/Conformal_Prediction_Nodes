package se.redfield.cp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import se.redfield.cp.nodes.ConformalPredictorNodeModel;

public class Calibrator {

	private ConformalPredictorNodeModel model;

	private BufferedDataTable outTable;
	private Map<String, List<Double>> calibrationProbabilities;

	public Calibrator(ConformalPredictorNodeModel model) {
		this.model = model;
	}

	public BufferedDataTable getOutTable() {
		return outTable;
	}

	public Map<String, List<Double>> getCalibrationProbabilities() {
		return calibrationProbabilities;
	}

	public DataTableSpec createOutputSpec(DataTableSpec inputTableSpec) {
		ColumnRearranger rearranger = new ColumnRearranger(inputTableSpec);
		rearranger.append(createPCellFactory(inputTableSpec));
		rearranger.append(createScoreCellFactory(inputTableSpec));
		return rearranger.createSpec();
	}

	public void process(BufferedDataTable inCalibrationTable, ExecutionContext exec) throws CanceledExecutionException {
		ColumnRearranger appendProbabilityRearranger = new ColumnRearranger(inCalibrationTable.getDataTableSpec());
		appendProbabilityRearranger.append(createPCellFactory(inCalibrationTable.getDataTableSpec()));
		BufferedDataTable appendedProbabilityTable = exec.createColumnRearrangeTable(inCalibrationTable,
				appendProbabilityRearranger, exec);

		BufferedDataTableSorter sorter = new BufferedDataTableSorter(appendedProbabilityTable,
				Arrays.asList(model.getSelectedColumnName(), model.getCalibrationProbabilityColumnName()),
				new boolean[] { true, false });
		BufferedDataTable sortedTable = sorter.sort(exec);

		this.calibrationProbabilities = new HashMap<>();

		ColumnRearranger appendScoreRearranger = new ColumnRearranger(sortedTable.getDataTableSpec());
		appendScoreRearranger.append(createScoreCellFactory(sortedTable.getSpec()));

		this.outTable = exec.createColumnRearrangeTable(sortedTable, appendScoreRearranger, exec);
	}

	private CellFactory createPCellFactory(DataTableSpec inputTableSpec) {
		int columnIndex = inputTableSpec.findColumnIndex(model.getSelectedColumnName());
		Map<String, Integer> probabilityColumns = inputTableSpec.getColumnSpec(columnIndex).getDomain().getValues()
				.stream().map(c -> c.toString()).collect(Collectors.toMap(str -> str,
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
		int probabilityIndex = inputTableSpec.findColumnIndex(model.getCalibrationProbabilityColumnName());

		return new AbstractCellFactory(
				new DataColumnSpecCreator(model.getCalibrationRankColumnName(), LongCell.TYPE).createSpec()) {

			private long counter = 0;
			private String prevValue = null;
			private List<Double> curProbabilities = null;

			@Override
			public DataCell[] getCells(DataRow row) {
				String value = row.getCell(columnIndex).toString();
				if (prevValue == null || !prevValue.equals(value)) {
					counter = 0;
					prevValue = value;

					curProbabilities = new ArrayList<>();
					calibrationProbabilities.put(value, curProbabilities);
				}

				Double probability = ((DoubleValue) row.getCell(probabilityIndex)).getDoubleValue();
				curProbabilities.add(probability);

				return new DataCell[] { new LongCell(counter++) };
			}
		};
	}
}
