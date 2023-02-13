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
package se.redfield.cp.nodes.ps.classifier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.settings.PredictiveSystemsClassifierSettings;

/**
 * The node settings for the Predictive Systems Classifier node.
 * 
 * @author Alexander Bondaletov
 *
 */
public class PredictiveSystemsClassifierNodeSettings {

	private static final String KEY_PROBABILITY_DISTRIBUTION_COLUMN = "probabilityDistributionColumn";

	private final SettingsModelString probabilityDistributionColumn;
	private final PredictiveSystemsClassifierSettings classifierSettings;

	/**
	 * Creates new instance
	 */
	public PredictiveSystemsClassifierNodeSettings() {
		probabilityDistributionColumn = new SettingsModelString(KEY_PROBABILITY_DISTRIBUTION_COLUMN, "");
		classifierSettings = new PredictiveSystemsClassifierSettings(PredictiveSystemsClassifierNodeModel.INPUT_TABLE);
	}

	public SettingsModelString getProbabilityDistributionColumnModel() {
		return probabilityDistributionColumn;
	}

	public String getProbabilityDistributionColumn() {
		return probabilityDistributionColumn.getStringValue();
	}

	public PredictiveSystemsClassifierSettings getClassifierSettings() {
		return classifierSettings;
	}

	public void loadSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		probabilityDistributionColumn.loadSettingsFrom(settings);
		classifierSettings.loadSettingsFrom(settings);
	}

	public void saveSettingsTo(NodeSettingsWO settings) {
		probabilityDistributionColumn.saveSettingsTo(settings);
		classifierSettings.saveSettingsTo(settings);
	}

	public void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		PredictiveSystemsClassifierNodeSettings temp = new PredictiveSystemsClassifierNodeSettings();
		temp.loadSettingsFrom(settings);
		temp.validate();
	}

	public void validate() throws InvalidSettingsException {
		classifierSettings.validate();
	}
}
