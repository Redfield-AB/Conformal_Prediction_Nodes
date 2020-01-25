package se.redfield.cp.nodes;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

import se.redfield.cp.Calibrator;

public class ConformalPredictorCalibratorNodeModel extends AbstractConformalPredictorNodeModel {

	private static final String CALIBRATION_RANK_COLUMN_DEFAULT_NAME = "Rank";

	private final Calibrator calibrator = new Calibrator(this);

	protected ConformalPredictorCalibratorNodeModel() {
		super(1, 1);
	}

	public String getCalibrationRankColumnName() {
		return CALIBRATION_RANK_COLUMN_DEFAULT_NAME;
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec inSpec = inSpecs[0];
		validateSettings(inSpec);

		return new DataTableSpec[] { calibrator.createOutputSpec(inSpec) };
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		return new BufferedDataTable[] { calibrator.process(inData[0], exec) };
	}
}
