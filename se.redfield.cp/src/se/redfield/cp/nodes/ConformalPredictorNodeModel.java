package se.redfield.cp.nodes;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.Calibrator;
import se.redfield.cp.Predictor;

public class ConformalPredictorNodeModel extends NodeModel {
	private static final NodeLogger LOGGER = NodeLogger.getLogger(ConformalPredictorNodeModel.class);

	public static final int PORT_CALIBRATION_TABLE = 0;
	public static final int PORT_PREDICTION_TABLE = 1;

	private static final String KEY_COLUMN_NAME = "columnName";
	private static final String KEY_KEEP_ALL_COLUMNS = "keepAllColumns";

	private static final String CALIBRATION_P_COLUMN_DEFAULT_NAME = "P";
	private static final String CALIBRATION_RANK_COLUMN_DEFAULT_NAME = "Rank";
	private static final String PREDICTION_RANK_COLUMN_DEFAULT_FORMAT = "Rank (%s)";
	private static final String PREDICTION_SCORE_COLUMN_DEFAULT_FORMAT = "Score (%s)";

	private final SettingsModelString columnNameSettings = createColumnNameSettingsModel();
	private final SettingsModelBoolean keepAllColumnsSettings = createKeepAllColumnsSettingsModel();

	private Calibrator calibrator;
	private Predictor predictor;

	static SettingsModelString createColumnNameSettingsModel() {
		return new SettingsModelString(KEY_COLUMN_NAME, "");
	}

	static SettingsModelBoolean createKeepAllColumnsSettingsModel() {
		return new SettingsModelBoolean(KEY_KEEP_ALL_COLUMNS, false);
	}

	protected ConformalPredictorNodeModel() {
		super(2, 2);
	}

	public String getProbabilityColumnName(String val) {
		return getProbabilityColumnName(getSelectedColumnName(), val);
	}

	public String getProbabilityColumnName(String column, String val) {
		return String.format(String.format("P (%s=%s)", column, val));
	}

	public String getSelectedColumnName() {
		return columnNameSettings.getStringValue();
	}

	public boolean getKeepAllColumns() {
		return keepAllColumnsSettings.getBooleanValue();
	}

	public String getCalibrationProbabilityColumnName() {
		return CALIBRATION_P_COLUMN_DEFAULT_NAME;
	}

	public String getCalibrationRankColumnName() {
		return CALIBRATION_RANK_COLUMN_DEFAULT_NAME;
	}

	public String getPredictionRankColumnFormat() {
		return PREDICTION_RANK_COLUMN_DEFAULT_FORMAT;
	}

	public String getPredictionScoreColumnFormat() {
		return PREDICTION_SCORE_COLUMN_DEFAULT_FORMAT;
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		calibrator.process(inData[PORT_CALIBRATION_TABLE], exec);
		return new BufferedDataTable[] { calibrator.getOutTable(),
				predictor.process(inData[PORT_PREDICTION_TABLE], exec) };
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		if (getSelectedColumnName().isEmpty()) {
			attemptAutoconfig(inSpecs[PORT_CALIBRATION_TABLE]);
		}

		if (getSelectedColumnName().isEmpty()) {
			throw new InvalidSettingsException("Class column is not selected");
		}

		validateTableSpecs(inSpecs[PORT_CALIBRATION_TABLE], "Calibration table");
		validateTableSpecs(inSpecs[PORT_PREDICTION_TABLE], "Prediction table");
		checkAllClassesPresent(inSpecs[PORT_CALIBRATION_TABLE], inSpecs[PORT_PREDICTION_TABLE]);

		calibrator = new Calibrator(this);
		predictor = new Predictor(this, calibrator);

		return new DataTableSpec[] { calibrator.createOutputSpec(inSpecs[PORT_CALIBRATION_TABLE]),
				predictor.createOuputTableSpec(inSpecs[PORT_PREDICTION_TABLE]) };
	}

	private void validateTableSpecs(DataTableSpec spec, String tableName) throws InvalidSettingsException {
		try {
			validateTableSpecs(getSelectedColumnName(), spec);
		} catch (InvalidSettingsException e) {
			throw new InvalidSettingsException(tableName + " : " + e.getMessage());
		}
	}

	private void validateTableSpecs(String selectedColumn, DataTableSpec spec) throws InvalidSettingsException {
		DataColumnSpec columnSpec = spec.getColumnSpec(selectedColumn);
		if (!columnSpec.getDomain().hasValues()) {
			throw new InvalidSettingsException("Insufficient domain information for column: " + selectedColumn);
		}

		Set<DataCell> values = columnSpec.getDomain().getValues();
		for (DataCell cell : values) {
			String value = cell.toString();
			String pColumnName = getProbabilityColumnName(selectedColumn, value);
			if (!spec.containsName(pColumnName)) {
				throw new InvalidSettingsException("Probability column not found: " + pColumnName);
			}
		}
	}

	private void checkAllClassesPresent(DataTableSpec calibrationTableSpec, DataTableSpec predictionTableSpec)
			throws InvalidSettingsException {
		Set<String> calibrationClasses = calibrationTableSpec.getColumnSpec(getSelectedColumnName()).getDomain()
				.getValues().stream().map(DataCell::toString).collect(Collectors.toSet());
		Set<String> predictionValues = predictionTableSpec.getColumnSpec(getSelectedColumnName()).getDomain()
				.getValues().stream().map(DataCell::toString).collect(Collectors.toSet());

		for (String val : predictionValues) {
			if (!calibrationClasses.contains(val)) {
				throw new InvalidSettingsException(
						String.format("Class '%s' is missing in the calibration table", val));
			}
		}
	}

	private void attemptAutoconfig(DataTableSpec spec) {
		String[] columnNames = spec.getColumnNames();
		for (String column : columnNames) {
			try {
				validateTableSpecs(column, spec);
				columnNameSettings.setStringValue(column);
				setWarningMessage(String.format("Node autoconfigured with '%s' column", column));
			} catch (InvalidSettingsException e) {
				// ignore
			}
		}
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		columnNameSettings.saveSettingsTo(settings);
		keepAllColumnsSettings.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		columnNameSettings.validateSettings(settings);
		keepAllColumnsSettings.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		columnNameSettings.loadSettingsFrom(settings);
		keepAllColumnsSettings.loadSettingsFrom(settings);
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
