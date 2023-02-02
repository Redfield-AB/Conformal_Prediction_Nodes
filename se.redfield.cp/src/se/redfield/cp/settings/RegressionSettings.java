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
package se.redfield.cp.settings;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.utils.KnimeUtils;
import se.redfield.cp.utils.PortDef;

/**
 * The regression settings.
 * 
 * @author Alexander Bondaletov
 *
 */
public class RegressionSettings {
	private static final String KEY_SIGMA_COLUMN_NAME = "sigma";
	private static final String KEY_NORMALIZED = "normalized";
	private static final String KEY_BETA = "beta";

	private static final double DEFAULT_BETA = 0.25;

	private final SettingsModelString sigmaColumn;
	private final SettingsModelBoolean normalized;
	private final SettingsModelDoubleBounded beta;

	private final PortDef[] tables;

	/**
	 * @param tables The input tables containing Sigma column
	 */
	public RegressionSettings(PortDef... tables) {
		this.tables = tables;

		sigmaColumn = new SettingsModelString(KEY_SIGMA_COLUMN_NAME, "");
		normalized = new SettingsModelBoolean(KEY_NORMALIZED, false);
		beta = new SettingsModelDoubleBounded(KEY_BETA, DEFAULT_BETA, 0, 1);

		normalized.addChangeListener(e -> {
			sigmaColumn.setEnabled(getNormalized());
			beta.setEnabled(getNormalized());
		});
		sigmaColumn.setEnabled(getNormalized());
		beta.setEnabled(getNormalized());
	}

	/**
	 * @return The sigma column model.
	 */
	public SettingsModelString getSigmaColumnModel() {
		return sigmaColumn;
	}

	/**
	 * @return The sigma column name.
	 */
	public String getSigmaColumn() {
		return sigmaColumn.getStringValue();
	}

	/**
	 * @return The normalized model.
	 */
	public SettingsModelBoolean getNormalizedModel() {
		return normalized;
	}

	/**
	 * @return The normalized mode.
	 */
	public boolean getNormalized() {
		return normalized.getBooleanValue();
	}

	/**
	 * @return The beta settings model.
	 */
	public SettingsModelDoubleBounded getBetaModel() {
		return beta;
	}

	/**
	 * @return The beta value.
	 */
	public double getBeta() {
		return beta.getDoubleValue();
	}

	/**
	 * Loads settings from the provided {@link NodeSettingsRO}
	 * 
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void loadSettingFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		sigmaColumn.loadSettingsFrom(settings);
		normalized.loadSettingsFrom(settings);
		beta.loadSettingsFrom(settings);
	}

	/**
	 * Saves current settings into the given {@link NodeSettingsWO}.
	 * 
	 * @param settings
	 */
	public void saveSettingsTo(NodeSettingsWO settings) {
		sigmaColumn.saveSettingsTo(settings);
		normalized.saveSettingsTo(settings);
		beta.saveSettingsTo(settings);
	}

	/**
	 * Validates internal consistency of the current settings
	 * 
	 * @throws InvalidSettingsException
	 */
	public void validate() throws InvalidSettingsException {
		if (getNormalized() && getSigmaColumn().isEmpty()) {
			throw new InvalidSettingsException("Sigma column is not selected");
		}
	}

	/**
	 * Validates the settings against input table spec.
	 * 
	 * @param inSpecs Input specs
	 * @throws InvalidSettingsException
	 */
	public void validateSettings(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		if (getNormalized()) {
			for (PortDef table : tables) {
				KnimeUtils.validateDoubleColumn(table, inSpecs, getSigmaColumn(), "Sigma");
			}
		}

		validate();
	}

}
