package se.redfield.cp.nodes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class ConformalPredictorNodeModel extends NodeModel {
	private static final NodeLogger LOGGER = NodeLogger.getLogger(ConformalPredictorCalibratorNodeModel.class);

	public static final int PORT_CALIBRATION_TABLE = 0;
	public static final int PORT_PREDICTION_TABLE = 1;

	private static final String KEY_COLUMN_NAME = "columnName";

	private final SettingsModelString columnNameSettings = createColumnNameSettingsModel();

	static SettingsModelString createColumnNameSettingsModel() {
		return new SettingsModelString(KEY_COLUMN_NAME, "");
	}

	protected ConformalPredictorNodeModel() {
		super(2, 1);
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {

		BufferedDataTable predictionTable = inData[PORT_PREDICTION_TABLE];
		Set<DataCell> values = predictionTable.getDataTableSpec().getColumnSpec(columnNameSettings.getStringValue())
				.getDomain().getValues();

		Map<String, List<Double>> probabilities = collectCalibrationProbabilities(inData[PORT_CALIBRATION_TABLE], exec);

		BufferedDataTable cur = predictionTable;
		for (DataCell v : values) {
			String val = v.toString();
			String pColumn = getProbabilityColumnName(val);

			BufferedDataTableSorter sorter = new BufferedDataTableSorter(cur, Arrays.asList(pColumn),
					new boolean[] { false });
			BufferedDataTable sorted = sorter.sort(exec);

			ColumnRearranger r = createRearranger(sorted.getDataTableSpec(), val, probabilities.get(val));
			cur = exec.createColumnRearrangeTable(sorted, r, exec);
		}

		return new BufferedDataTable[] { cur };
	}

	private Map<String, List<Double>> collectCalibrationProbabilities(BufferedDataTable calibrationTable,
			ExecutionContext exec) throws CanceledExecutionException {
		BufferedDataTableSorter sorter = new BufferedDataTableSorter(calibrationTable,
				Arrays.asList(columnNameSettings.getStringValue(),
						ConformalPredictorCalibratorNodeModel.P_COLUMN_DEFAULT_NAME),
				new boolean[] { true, false });
		BufferedDataTable sortedTable = sorter.sort(exec);

		int columpIndex = sortedTable.getDataTableSpec().findColumnIndex(columnNameSettings.getStringValue());
		int probabilityIndex = sortedTable.getDataTableSpec()
				.findColumnIndex(ConformalPredictorCalibratorNodeModel.P_COLUMN_DEFAULT_NAME);
		CloseableRowIterator iterator = sortedTable.iterator();

		String currentVal = null;
		List<Double> probabilities = new ArrayList<>();
		Map<String, List<Double>> result = new HashMap<>();

		while (iterator.hasNext()) {
			DataRow row = iterator.next();
			String val = row.getCell(columpIndex).toString();

			if (!val.equals(currentVal)) {
				if (currentVal != null) {
					result.put(currentVal, probabilities);
				}
				currentVal = val;
				probabilities = new ArrayList<>();
			}

			Double probability = ((DoubleValue) row.getCell(probabilityIndex)).getDoubleValue();
			probabilities.add(probability);
		}

		result.put(currentVal, probabilities);

		return result;
	}

	private String getProbabilityColumnName(String val) {
		return String.format(String.format("P (%s=%s)", columnNameSettings.getStringValue(), val));
	}

	private ColumnRearranger createRearranger(DataTableSpec spec, String value, List<Double> probabilities) {
		ColumnRearranger r = new ColumnRearranger(spec);
		int pColumnIndex = spec.findColumnIndex(getProbabilityColumnName(value));
		r.append(new ScoreCellFactory(value, pColumnIndex, probabilities));
		return r;
	}

	private class ScoreCellFactory extends AbstractCellFactory {

		private int pColumnIndex;
		private List<Double> probabilities;
		private int currentIndex;

		public ScoreCellFactory(String value, int pColumnIndex, List<Double> probabilities) {
			super(createScoreColumnsSpecs(value));
			this.pColumnIndex = pColumnIndex;
			this.probabilities = probabilities;
			this.currentIndex = 0;
		}

		@Override
		public DataCell[] getCells(DataRow row) {
			double p = ((DoubleValue) row.getCell(pColumnIndex)).getDoubleValue();
			int rank = getRank(p);
			double score = ((double) probabilities.size() - rank) / (probabilities.size() + 1);
			return new DataCell[] { new LongCell(rank), new DoubleCell(score) };
		}

		private int getRank(double p) {
			while (currentIndex < probabilities.size() && probabilities.get(currentIndex) >= p) {
				currentIndex++;
			}
			return currentIndex > 0 ? currentIndex - 1 : currentIndex;
		}
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		return new DataTableSpec[] { createOuputTableSpec(inSpecs[PORT_PREDICTION_TABLE]) };
	}

	private DataTableSpec createOuputTableSpec(DataTableSpec inPredictionTableSpecs) {
		List<DataColumnSpec> colls = new ArrayList<>();
		for (int i = 0; i < inPredictionTableSpecs.getNumColumns(); i++) {
			colls.add(inPredictionTableSpecs.getColumnSpec(i));
		}

		Set<DataCell> values = inPredictionTableSpecs.getColumnSpec(columnNameSettings.getStringValue()).getDomain()
				.getValues();
		for (DataCell v : values) {
			colls.addAll(Arrays.asList(createScoreColumnsSpecs(v.toString())));
		}
		return new DataTableSpec(colls.toArray(new DataColumnSpec[] {}));
	}

	private DataColumnSpec[] createScoreColumnsSpecs(String value) {
		DataColumnSpec indexCol = new DataColumnSpecCreator(String.format("Index (%s)", value), LongCell.TYPE)
				.createSpec();
		DataColumnSpec scoreCol = new DataColumnSpecCreator(String.format("Score (%s)", value), DoubleCell.TYPE)
				.createSpec();
		return new DataColumnSpec[] { indexCol, scoreCol };
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		columnNameSettings.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		columnNameSettings.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		columnNameSettings.loadSettingsFrom(settings);
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void reset() {
		// TODO Auto-generated method stub

	}

}
