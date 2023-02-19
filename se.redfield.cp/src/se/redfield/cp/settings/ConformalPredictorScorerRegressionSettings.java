/*
 * Copyright (c) 2022 Redfield AB.
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
package se.redfield.cp.settings;

import static se.redfield.cp.nodes.ConformalPredictorScorerRegressionNodeModel.PORT_INPUT_TABLE;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.nodes.ConformalPredictorScorerRegressionNodeModel;
import se.redfield.cp.utils.KnimeUtils;

/**
 * The node settings for the {@link ConformalPredictorScorerRegressionNodeModel}
 * node.
 * 
 * @author Alexander Bondaletov, Redfield SE
 *
 */
public class ConformalPredictorScorerRegressionSettings {
	private static final String KEY_TARGET_COLUMN = "targetColumn";
	private static final String KEY_ADDITIONAL_INFO = "additionalInfo";
	private static final String KEY_UPPERBOUND_COLUMN = "upperBound";
	private static final String KEY_LOWERBOUND_COLUMN = "lowerBound";
	private static final String KEY_HAS_UPPER_BOUND = "hasUpperBound";
	private static final String KEY_HAS_LOWER_BOUND = "hasLowerBound";

	private final SettingsModelString targetColumn;
	private final SettingsModelString upperboundColumn;
	private final SettingsModelString lowerboundColumn;
	private final SettingsModelBoolean additionalInfo;
	private final SettingsModelBoolean hasUpperBound;
	private final SettingsModelBoolean hasLowerBound;

	/**
	 * Creates new instance
	 */
	public ConformalPredictorScorerRegressionSettings() {
		targetColumn = new SettingsModelString(KEY_TARGET_COLUMN, "");
		upperboundColumn = new SettingsModelString(KEY_UPPERBOUND_COLUMN,
				PredictorRegressionSettings.PREDICTION_UPPER_COLUMN_DEFAULT_NAME);
		lowerboundColumn = new SettingsModelString(KEY_LOWERBOUND_COLUMN,
				PredictorRegressionSettings.PREDICTION_LOWER_COLUMN_DEFAULT_NAME);
		additionalInfo = new SettingsModelBoolean(KEY_ADDITIONAL_INFO, true);
		hasUpperBound = new SettingsModelBoolean(KEY_HAS_UPPER_BOUND, true);
		hasLowerBound = new SettingsModelBoolean(KEY_HAS_LOWER_BOUND, true);

		hasUpperBound.addChangeListener(e -> updateSettingsEnabled());
		hasLowerBound.addChangeListener(e -> updateSettingsEnabled());
	}

	private void updateSettingsEnabled() {
		upperboundColumn.setEnabled(hasUpperBound());
		lowerboundColumn.setEnabled(hasLowerBound());

		if (isOpenInterval()) {
			additionalInfo.setBooleanValue(false);
			additionalInfo.setEnabled(false);
		} else {
			additionalInfo.setEnabled(true);
		}
	}

	/**
	 * @return The target column model.
	 */
	public SettingsModelString getTargetColumnModel() {
		return targetColumn;
	}

	/**
	 * @return The target column.
	 */
	public String getTargetColumn() {
		return targetColumn.getStringValue();
	}

	/**
	 * @return The upper bound column model.
	 */
	public SettingsModelString getUpperboundColumnModel() {
		return upperboundColumn;
	}

	/**
	 * @return The upper bound column name.
	 */
	public String getUpperBoundColumnName() {
		return upperboundColumn.getStringValue();
	}

	/**
	 * @return The lower bound column model.
	 */
	public SettingsModelString getLowerboundColumnModel() {
		return lowerboundColumn;
	}

	/**
	 * @return The lower bound column.
	 */
	public String getLowerBoundColumnName() {
		return lowerboundColumn.getStringValue();
	}

	/**
	 * @return The additional info model.
	 */
	public SettingsModelBoolean getAdditionalInfoModel() {
		return additionalInfo;
	}

	/**
	 * @return Whether the scorer should output additional metrics.
	 */
	public boolean isAdditionalInfoMode() {
		return additionalInfo.getBooleanValue();
	}
	
	/**
	 * @return The hasUppserBound model.
	 */
	public SettingsModelBoolean getHasUpperBoundModel() {
		return hasUpperBound;
	}

	/**
	 * @return Whether the upper bound is specified.
	 */
	public boolean hasUpperBound() {
		return hasUpperBound.getBooleanValue();
	}

	/**
	 * @return The hasLowerBound model.
	 */
	public SettingsModelBoolean getHasLowerBoundModel() {
		return hasLowerBound;
	}

	/**
	 * @return Whether the lower bound is specified.
	 */
	public boolean hasLowerBound() {
		return hasLowerBound.getBooleanValue();
	}

	/**
	 * @return Whether the only one bound is specified.
	 */
	public boolean isOpenInterval() {
		return !hasUpperBound() || !hasLowerBound();
	}

	/**
	 * @param settings The settings
	 */
	public void saveSettingsTo(NodeSettingsWO settings) {
		targetColumn.saveSettingsTo(settings);
		upperboundColumn.saveSettingsTo(settings);
		lowerboundColumn.saveSettingsTo(settings);
		additionalInfo.saveSettingsTo(settings);
		hasUpperBound.saveSettingsTo(settings);
		hasLowerBound.saveSettingsTo(settings);
	}

	/**
	 * @param settings The settings
	 * @throws InvalidSettingsException
	 */
	public void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		targetColumn.loadSettingsFrom(settings);
		upperboundColumn.loadSettingsFrom(settings);
		lowerboundColumn.loadSettingsFrom(settings);
		additionalInfo.loadSettingsFrom(settings);

		if (settings.containsKey(KEY_HAS_UPPER_BOUND)) {
			hasUpperBound.loadSettingsFrom(settings);
		} else {
			hasUpperBound.setBooleanValue(true);
		}

		if (settings.containsKey(KEY_HAS_LOWER_BOUND)) {
			hasLowerBound.loadSettingsFrom(settings);
		} else {
			hasLowerBound.setBooleanValue(true);
		}
	}

	/**
	 * @param settings The settings
	 * @throws InvalidSettingsException
	 */
	public void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		ConformalPredictorScorerRegressionSettings temp = new ConformalPredictorScorerRegressionSettings();
		temp.loadValidatedSettingsFrom(settings);
		temp.validate();
	}

	private void validate() throws InvalidSettingsException {
		if (getTargetColumn().isEmpty()) {
			throw new InvalidSettingsException("Target column is not selected.");
		}
		if (hasUpperBound() && getUpperBoundColumnName().isEmpty()) {
			throw new InvalidSettingsException("Upper bound column is not selected");
		}
		if (hasLowerBound() && getLowerBoundColumnName().isEmpty()) {
			throw new InvalidSettingsException("Lower bound column is not selected");
		}
		if (!hasLowerBound() && !hasUpperBound()) {
			throw new InvalidSettingsException("At least one of the bounds has to be specified");
		}
	}

	/**
	 * Validates settings against input specs.
	 * 
	 * @param inSpecs Input specs.
	 * 
	 * @throws InvalidSettingsException
	 */
	public void validataSettings(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		validate();
		KnimeUtils.validateDoubleColumn(PORT_INPUT_TABLE, inSpecs, getTargetColumn(), "Target");
		KnimeUtils.validateDoubleColumn(PORT_INPUT_TABLE, inSpecs, getLowerBoundColumnName(), "Lower bound");
		KnimeUtils.validateDoubleColumn(PORT_INPUT_TABLE, inSpecs, getUpperBoundColumnName(), "Upper bound");
	}
}
