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

import org.knime.base.node.preproc.sample.SamplingNodeSettings;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;

/**
 * The {@link SamplingNodeSettings} extended by validation methods.
 * 
 * @author Alexander Bondaletov
 *
 */
public class SamplingSettings extends SamplingNodeSettings {
	/**
	 * Validates settings stored in the provided {@link NodeSettingsRO}.
	 * 
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		SamplingSettings temp = new SamplingSettings();
		temp.loadSettingsFrom(settings, false);
		temp.validate();
	}

	private void validate() throws InvalidSettingsException {
		if (countMethod() == null) {
			throw new InvalidSettingsException("No sampling method selected");
		}

		switch (countMethod()) {
		case Absolute:
			if (count() < 0) {
				throw new InvalidSettingsException("Invalid count: " + count());
			}
			break;
		case Relative:
			if (fraction() < 0 || fraction() > 1) {
				throw new InvalidSettingsException("Invalid fraction: " + fraction());
			}
			break;
		default:
			throw new InvalidSettingsException("Unknown counting method: " + countMethod());
		}

		if (samplingMethod() == SamplingMethods.Stratified && classColumn() == null) {
			throw new InvalidSettingsException("Class column is not selected");
		}
	}

	/**
	 * Validates the settings against input table spec.
	 * 
	 * @param inSpec Input table spec
	 * @throws InvalidSettingsException
	 */
	public void validate(DataTableSpec inSpec) throws InvalidSettingsException {
		validate();

		if (samplingMethod() == SamplingMethods.Stratified && !inSpec.containsName(classColumn())) {
			throw new InvalidSettingsException(
					"Column '" + classColumn() + "' for stratified sampling " + "does not exist");
		}
	}
}
