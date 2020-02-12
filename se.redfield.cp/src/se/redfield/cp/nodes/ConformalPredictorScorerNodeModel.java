package se.redfield.cp.nodes;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.Scorer;

public class ConformalPredictorScorerNodeModel extends NodeModel {

	private static final String KEY_TARGET_COLUMN = "targetColumn";
	private static final String KEY_CLASSES_COLUMN = "classesColumn";
	private static final String KEY_ADDITIONAL_INFO = "additionalInfo";

	private final SettingsModelString targetColumnSettings = createTargetColumnSettings();
	private final SettingsModelString classesColumnSettings = createClassesColumnSettings();
	private final SettingsModelString stringSeparatorSettings = ConformalPredictorClassifierNodeModel
			.createStringSeparatorSettings();
	private final SettingsModelBoolean additionalInforSettings = createAdditionalInfoSettings();

	private final Scorer scorer = new Scorer(this);

	static SettingsModelString createTargetColumnSettings() {
		return new SettingsModelString(KEY_TARGET_COLUMN, "");
	}

	static SettingsModelString createClassesColumnSettings() {
		return new SettingsModelString(KEY_CLASSES_COLUMN, "");
	}

	static SettingsModelBoolean createAdditionalInfoSettings() {
		return new SettingsModelBoolean(KEY_ADDITIONAL_INFO, true);
	}

	protected ConformalPredictorScorerNodeModel() {
		super(1, 1);
	}

	public String getTargetColumn() {
		return targetColumnSettings.getStringValue();
	}

	public String getClassesColumn() {
		return classesColumnSettings.getStringValue();
	}

	public String getStringSeparator() {
		return stringSeparatorSettings.getStringValue();
	}

	public boolean isAdditionalInfoMode() {
		return additionalInforSettings.getBooleanValue();
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		if (getTargetColumn().isEmpty() || getClassesColumn().isEmpty()) {
			attemptAutoconfig(inSpecs[0]);
		}
		validataSettings(inSpecs[0]);
		return new DataTableSpec[] { scorer.createOutputSpec() };
	}

	private void attemptAutoconfig(DataTableSpec spec) {
		if (getTargetColumn().isEmpty()) {
			// Selecting String column with the lowest number of different values
			int valuesNum = Integer.MAX_VALUE;
			for (DataColumnSpec c : spec) {
				if (c.getDomain().hasValues() && !c.getDomain().getValues().isEmpty()
						&& c.getDomain().getValues().size() < valuesNum) {
					valuesNum = c.getDomain().getValues().size();
					targetColumnSettings.setStringValue(c.getName());
				}
			}
		}
		if (getClassesColumn().isEmpty()) {
			classesColumnSettings.setStringValue(ConformalPredictorClassifierNodeModel.DEFAULT_CLASSES_COLUMN_NAME);
		}
	}

	private void validataSettings(DataTableSpec spec) throws InvalidSettingsException {
		if (getTargetColumn().isEmpty()) {
			throw new InvalidSettingsException("Target column is not selected.");
		}
		if (getClassesColumn().isEmpty()) {
			throw new InvalidSettingsException("Classes column is not selected.");
		}
		if (!spec.containsName(getTargetColumn())) {
			throw new InvalidSettingsException(
					"Selected target column '" + getTargetColumn() + "' is missing from the input table.");
		}
		if (!spec.containsName(getClassesColumn())) {
			throw new InvalidSettingsException(
					"Selected classes column '" + getClassesColumn() + "' is missing from the input table.");
		}

		DataColumnSpec classesColumn = spec.getColumnSpec(getClassesColumn());
		if (!classesColumn.getType().isCollectionType() && !classesColumn.getType().isCompatible(StringValue.class)) {
			throw new InvalidSettingsException("Classes column has unsupported data type: " + classesColumn.getType());
		}
		if (classesColumn.getType().isCompatible(StringValue.class) && getStringSeparator().isEmpty()) {
			throw new InvalidSettingsException("String separator is empty");
		}
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		return new BufferedDataTable[] { scorer.process(inData[0], exec) };
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		targetColumnSettings.saveSettingsTo(settings);
		classesColumnSettings.saveSettingsTo(settings);
		stringSeparatorSettings.saveSettingsTo(settings);
		additionalInforSettings.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		targetColumnSettings.validateSettings(settings);
		classesColumnSettings.validateSettings(settings);
		stringSeparatorSettings.validateSettings(settings);
		additionalInforSettings.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		targetColumnSettings.loadSettingsFrom(settings);
		classesColumnSettings.loadSettingsFrom(settings);
		stringSeparatorSettings.loadSettingsFrom(settings);
		additionalInforSettings.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {

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
}
