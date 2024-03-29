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
package se.redfield.cp.nodes;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.knime.core.data.DataCell;
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

import se.redfield.cp.core.scoring.Scorer;
import se.redfield.cp.settings.ClassifierSettings;
import se.redfield.cp.settings.PredictorSettings;

/**
 * Conformal Scorer Node. Used to evaluate data generated by Conformal
 * Classifier Node. Collects the following metrics for each row:
 * <ul>
 * <li>Exact match � number of correct predictions that belong to one class, and
 * not belong to any mixed class.</li>
 * <li>Soft match - number of correct predictions that belong to one of the
 * mixed classes.</li>
 * <li>Total match � Exact_match + Soft_match.</li>
 * <li>Error � number of predictions that does not match real target class.</li>
 * <li>Total � total number of records that belongs to the current target
 * class.</li>
 * <li>Efficiency = Exact_match/(Exact_match + Error)</li>
 * <li>Validity = Total_match/Total</li>
 * </ul>
 * 
 *
 */
public class ConformalPredictorScorerNodeModel extends NodeModel {

	private static final String KEY_TARGET_COLUMN = "targetColumn";
	private static final String KEY_CLASSES_COLUMN = "classesColumn";
	private static final String KEY_STRING_SEPARATOR = "stringSeparator";
	private static final String KEY_ADDITIONAL_INFO = "additionalInfo";
	private static final String KEY_EFFICIENCY_METRICS = "additionalEfficiencyMetrics";

	private final SettingsModelString targetColumnSettings = createTargetColumnSettings();
	private final SettingsModelString classesColumnSettings = createClassesColumnSettings();
	private final SettingsModelString stringSeparatorSettings = createStringSeparatorSettings();
	private final SettingsModelBoolean additionalInfoSettings = createAdditionalInfoSettings();
	private final SettingsModelBoolean additionalEfficiencyMetricsSettings = createAdditionalEfficiencyMetricsSettings();

	private final Scorer scorer = new Scorer(this);

	static SettingsModelString createTargetColumnSettings() {
		return new SettingsModelString(KEY_TARGET_COLUMN, "");
	}

	static SettingsModelString createClassesColumnSettings() {
		return new SettingsModelString(KEY_CLASSES_COLUMN, "");
	}

	static SettingsModelString createStringSeparatorSettings() {
		return new SettingsModelString(KEY_STRING_SEPARATOR, ClassifierSettings.DEFAULT_SEPARATOR);
	}

	static SettingsModelBoolean createAdditionalInfoSettings() {
		return new SettingsModelBoolean(KEY_ADDITIONAL_INFO, true);
	}

	static SettingsModelBoolean createAdditionalEfficiencyMetricsSettings() {
		return new SettingsModelBoolean(KEY_EFFICIENCY_METRICS, true);
	}

	protected ConformalPredictorScorerNodeModel() {
		super(1, 1);
	}

	/**
	 * @return The target column
	 */
	public String getTargetColumn() {
		return targetColumnSettings.getStringValue();
	}

	/**
	 * @return The classes column
	 */
	public String getClassesColumn() {
		return classesColumnSettings.getStringValue();
	}

	/**
	 * @return The string separator
	 */
	public String getStringSeparator() {
		return stringSeparatorSettings.getStringValue();
	}

	/**
	 * @return Whether additional info mode is enabled
	 */
	public boolean isAdditionalInfoMode() {
		return additionalInfoSettings.getBooleanValue();
	}

	/**
	 * @return Whether additional efficiency metrics mode is enabled
	 */
	public boolean isAdditionalEfficiencyMetricsMode() {
		return additionalEfficiencyMetricsSettings.getBooleanValue();
	}

	/**
	 * Returns probability column name for a given value for a selected target
	 * column.
	 * 
	 * @param val target's value
	 * @return probability column name
	 */
	public String getProbabilityColumnName(String val) {
		return String.format(String.format(PredictorSettings.PREDICTION_P_VALUE_COLUMN_DEFAULT_FORMAT, val));
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		if (getTargetColumn().isEmpty() || getClassesColumn().isEmpty()) {
			attemptAutoconfig(inSpecs[0]);
		}
		validataSettings(inSpecs[0]);
		return new DataTableSpec[] { scorer.createOutputSpec() };
	}

	/**
	 * Attempts autoconfig by selecting column with the lowest amount of different
	 * values as a target column
	 */
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
			classesColumnSettings.setStringValue(ClassifierSettings.DEFAULT_CLASSES_COLUMN_NAME);
		}
	}

	/**
	 * Validates settings against input table spec.
	 * 
	 * @param spec Input table spec.
	 * @throws InvalidSettingsException
	 */
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

		validatePValueSettings(spec);

		DataColumnSpec classesColumn = spec.getColumnSpec(getClassesColumn());
		if (!classesColumn.getType().isCollectionType() && !classesColumn.getType().isCompatible(StringValue.class)) {
			throw new InvalidSettingsException("Classes column has unsupported data type: " + classesColumn.getType());
		}
		if (classesColumn.getType().isCompatible(StringValue.class) && getStringSeparator().isEmpty()) {
			throw new InvalidSettingsException("String separator is empty");
		}
	}

	protected void validatePValueSettings(DataTableSpec spec) throws InvalidSettingsException {
		if (isAdditionalEfficiencyMetricsMode()) {
			String selectedColumn = getTargetColumn();
			DataColumnSpec columnSpec = spec.getColumnSpec(selectedColumn);
			if (!columnSpec.getDomain().hasValues() || columnSpec.getDomain().getValues().isEmpty()) {
				throw new InvalidSettingsException("Insufficient domain information for column: " + selectedColumn);
			}

			Set<DataCell> values = columnSpec.getDomain().getValues();
			for (DataCell cell : values) {
				String value = cell.toString();
				if (!spec.containsName(getProbabilityColumnName(value))) {
					throw new InvalidSettingsException("Probability column not found for class:" + value);
				}
			}
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
		additionalInfoSettings.saveSettingsTo(settings);
		additionalEfficiencyMetricsSettings.saveSettingsTo(settings);
		additionalEfficiencyMetricsSettings.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		targetColumnSettings.validateSettings(settings);
		classesColumnSettings.validateSettings(settings);
		stringSeparatorSettings.validateSettings(settings);
		additionalInfoSettings.validateSettings(settings);
		additionalEfficiencyMetricsSettings.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		targetColumnSettings.loadSettingsFrom(settings);
		classesColumnSettings.loadSettingsFrom(settings);
		stringSeparatorSettings.loadSettingsFrom(settings);
		additionalInfoSettings.loadSettingsFrom(settings);
		additionalEfficiencyMetricsSettings.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
		// nothing to reset
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no internals
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no internals
	}
}
