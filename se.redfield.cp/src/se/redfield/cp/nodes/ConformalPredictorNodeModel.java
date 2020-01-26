package se.redfield.cp.nodes;

import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import se.redfield.cp.Predictor;

public class ConformalPredictorNodeModel extends AbstractConformalPredictorNodeModel {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(ConformalPredictorNodeModel.class);

	public static final int PORT_CALIBRATION_TABLE = 0;
	public static final int PORT_PREDICTION_TABLE = 1;

	private static final String PREDICTION_RANK_COLUMN_DEFAULT_FORMAT = "Rank (%s)";
	private static final String PREDICTION_SCORE_COLUMN_DEFAULT_FORMAT = "Score (%s)";

	private final Predictor predictor = new Predictor(this);

	protected ConformalPredictorNodeModel() {
		super(2, 1);
	}

	public String getPredictionRankColumnFormat() {
		return PREDICTION_RANK_COLUMN_DEFAULT_FORMAT;
	}

	public String getPredictionScoreColumnFormat() {
		return PREDICTION_SCORE_COLUMN_DEFAULT_FORMAT;
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		return new BufferedDataTable[] {
				predictor.process(inData[PORT_PREDICTION_TABLE], inData[PORT_CALIBRATION_TABLE], exec) };
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		validateSettings(inSpecs[PORT_PREDICTION_TABLE]);
		validateCalibrationTable(inSpecs[PORT_CALIBRATION_TABLE], inSpecs[PORT_PREDICTION_TABLE]);

		return new DataTableSpec[] { predictor.createOuputTableSpec(inSpecs[PORT_PREDICTION_TABLE]) };
	}

	private void validateCalibrationTable(DataTableSpec calibrationTableSpec, DataTableSpec predictionTableSpec)
			throws InvalidSettingsException {
		if (!calibrationTableSpec.containsName(getSelectedColumnName())) {
			throw new InvalidSettingsException(
					String.format("Class column '%s' is missing from the calibration table", getSelectedColumnName()));
		}

		if (!calibrationTableSpec.containsName(getCalibrationProbabilityColumnName())) {
			throw new InvalidSettingsException(
					String.format("Probability columns '%s' is missing from the calibration table",
							getCalibrationProbabilityColumnName()));
		}

		DataColumnSpec columnSpec = calibrationTableSpec.getColumnSpec(getSelectedColumnName());
		if (!columnSpec.getDomain().hasValues() || columnSpec.getDomain().getValues().isEmpty()) {
			throw new InvalidSettingsException(
					"Calibration table: insufficient domain information for column: " + getSelectedColumnName());
		}

		checkAllClassesPresent(calibrationTableSpec, predictionTableSpec);
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

}
