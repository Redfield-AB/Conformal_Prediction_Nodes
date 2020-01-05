package se.redfield.cp.nodes;

import java.io.File;
import java.io.IOException;

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
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.Calibrator;
import se.redfield.cp.Predictor;

public class ConformalPredictorNodeModel extends NodeModel {
	private static final NodeLogger LOGGER = NodeLogger.getLogger(ConformalPredictorCalibratorNodeModel.class);

	public static final int PORT_CALIBRATION_TABLE = 0;
	public static final int PORT_PREDICTION_TABLE = 1;

	private static final String KEY_COLUMN_NAME = "columnName";

	private static final String CALIBRATION_P_COLUMN_DEFAULT_NAME = "P";
	private static final String CALIBRATION_RANK_COLUMN_DEFAULT_NAME = "Rank";
	private static final String PREDICTION_RANK_COLUMN_DEFAULT_FORMAT = "Index (%s)";
	private static final String PREDICTION_SCORE_COLUMN_DEFAULT_FORMAT = "Score (%s)";

	private final SettingsModelString columnNameSettings = createColumnNameSettingsModel();

	private Calibrator calibrator;
	private Predictor predictor;

	static SettingsModelString createColumnNameSettingsModel() {
		return new SettingsModelString(KEY_COLUMN_NAME, "");
	}

	protected ConformalPredictorNodeModel() {
		super(2, 2);
	}

	public String getProbabilityColumnName(String val) {
		return String.format(String.format("P (%s=%s)", getSelectedColumnName(), val));
	}

	public String getSelectedColumnName() {
		return columnNameSettings.getStringValue();
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

		calibrator = new Calibrator(this);
		predictor = new Predictor(this, calibrator);

		return new DataTableSpec[] { calibrator.createOutputSpec(inSpecs[PORT_CALIBRATION_TABLE]),
				predictor.createOuputTableSpec(inSpecs[PORT_PREDICTION_TABLE]) };
	}

	private void attemptAutoconfig(DataTableSpec spec) {
		// TODO
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
