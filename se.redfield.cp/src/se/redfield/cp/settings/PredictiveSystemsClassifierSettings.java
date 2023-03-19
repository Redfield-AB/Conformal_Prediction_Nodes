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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.utils.PortDef;

public class PredictiveSystemsClassifierSettings {

	private static final String KEY_TARGET = "target";
	private static final String KEY_TARGET_COLUMN = "targetColumn";
	private static final String KEY_LOWER_PERCENTILES = "lowerPercentiles";
	private static final String KEY_UPPER_PERCENTILES = "upperPercentiles";
	private static final String KEY_HAS_TARGET = "hasTarget";
	private static final String KEY_HAS_TARGET_COLUMN = "hasTargetColumn";

	private final SettingsModelDouble target;
	private final SettingsModelString targetColumn;
	private final SettingsModelBoolean hasTarget;
	private final SettingsModelBoolean hasTargetColumn;

	private double[] lowerPercentiles;
	private double[] upperPercentiles;

	private final PortDef table;

	public PredictiveSystemsClassifierSettings(PortDef table) {
		this.table = table;
		target = new SettingsModelDouble(KEY_TARGET, 0);
		targetColumn = new SettingsModelString(KEY_TARGET_COLUMN, "");
		hasTarget = new SettingsModelBoolean(KEY_HAS_TARGET, false);
		hasTargetColumn = new SettingsModelBoolean(KEY_HAS_TARGET_COLUMN, false);
		lowerPercentiles = new double[0];
		upperPercentiles = new double[0];

		hasTarget.addChangeListener(e -> target.setEnabled(hasTarget.getBooleanValue()));
		hasTargetColumn.addChangeListener(e -> targetColumn.setEnabled(hasTargetColumn.getBooleanValue()));

		target.setEnabled(hasTarget.getBooleanValue());
		targetColumn.setEnabled(hasTargetColumn.getBooleanValue());
	}

	public PortDef getTable() {
		return table;
	}

	public SettingsModelDouble getTargetModel() {
		return target;
	}

	public double getTarget() {
		return target.getDoubleValue();
	}

	public SettingsModelString getTargetColumnModel() {
		return targetColumn;
	}

	public String getTargetColumn() {
		return targetColumn.getStringValue();
	}

	public SettingsModelBoolean getHasTargetModel() {
		return hasTarget;
	}

	public boolean hasTarget() {
		return hasTarget.getBooleanValue();
	}

	public SettingsModelBoolean getHasTargetColumnModel() {
		return hasTargetColumn;
	}

	public boolean hasTargetColumn() {
		return hasTargetColumn.getBooleanValue();
	}

	public double[] getLowerPercentiles() {
		return lowerPercentiles;
	}

	public void setLowerPercentiles(double[] lowerPercentiles) {
		this.lowerPercentiles = lowerPercentiles;
	}

	public double[] getUpperPercentiles() {
		return upperPercentiles;
	}

	public void setUpperPercentiles(double[] upperPercentiles) {
		this.upperPercentiles = upperPercentiles;
	}

	public void loadSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		target.loadSettingsFrom(settings);
		targetColumn.loadSettingsFrom(settings);
		hasTarget.loadSettingsFrom(settings);
		hasTargetColumn.loadSettingsFrom(settings);
		lowerPercentiles = settings.getDoubleArray(KEY_LOWER_PERCENTILES);
		upperPercentiles = settings.getDoubleArray(KEY_UPPER_PERCENTILES);
	}

	public void saveSettingsTo(NodeSettingsWO settings) {
		target.saveSettingsTo(settings);
		targetColumn.saveSettingsTo(settings);
		hasTarget.saveSettingsTo(settings);
		hasTargetColumn.saveSettingsTo(settings);
		settings.addDoubleArray(KEY_LOWER_PERCENTILES, lowerPercentiles);
		settings.addDoubleArray(KEY_UPPER_PERCENTILES, upperPercentiles);
	}

	public void validate() throws InvalidSettingsException {
		if (!hasTarget() && !hasTargetColumn() && lowerPercentiles.length == 0 && upperPercentiles.length == 0) {
			throw new InvalidSettingsException("No targets or percentiles selected");
		}

	}
}
