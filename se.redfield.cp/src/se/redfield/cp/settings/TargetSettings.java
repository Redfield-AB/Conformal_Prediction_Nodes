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

import java.util.Set;
import java.util.function.Consumer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import se.redfield.cp.utils.PortDef;

/**
 * Target settings used by classification nodes. Consist of target column and
 * probability columns format.
 * 
 * @author Alexander Bondaletov
 *
 */
public class TargetSettings {

	private static final String KEY_TARGET_COLUMN_NAME = "targetColumn";
	private static final String KEY_PROBABILITY_COLUMN_FORMAT = "probabilityColumnFormat";

	private static final String DEFAULT_FORMAT = "P (%1$s=%2$s)";

	private final SettingsModelString targetColumn;
	private final SettingsModelString probabilityFormat;

	private final PortDef targetColumnTable;
	private final PortDef[] probabilityColumnsTables;

	/**
	 * @param targetColumnTable        The table with target column.
	 * @param probabilityColumnsTables The tables with probability columns.
	 */
	public TargetSettings(PortDef targetColumnTable, PortDef... probabilityColumnsTables) {
		this.targetColumnTable = targetColumnTable;
		this.probabilityColumnsTables = probabilityColumnsTables;

		targetColumn = new SettingsModelString(KEY_TARGET_COLUMN_NAME, "");
		probabilityFormat = new SettingsModelString(KEY_PROBABILITY_COLUMN_FORMAT, DEFAULT_FORMAT);
	}

	/**
	 * @return The target column settings model.
	 */
	public SettingsModelString getTargetColumnModel() {
		return targetColumn;
	}

	/**
	 * @return The target column name.
	 */
	public String getTargetColumn() {
		return targetColumn.getStringValue();
	}

	/**
	 * @return The probability columns format settings model.
	 */
	public SettingsModelString getProbabilityFormatModel() {
		return probabilityFormat;
	}

	/**
	 * @return The format string to construct probability column name for a
	 *         particular class.
	 */
	public String getProbabilityFormat() {
		return probabilityFormat.getStringValue();
	}

	/**
	 * @param value The class value
	 * @return The probability column name for a given value.
	 */
	public String getProbabilityColumnName(String value) {
		return String.format(String.format(getProbabilityFormat(), getTargetColumn(), value));
	}

	/**
	 * @return The table containing target column.
	 */
	public PortDef getTargetColumnTable() {
		return targetColumnTable;
	}

	/**
	 * @return The tables containing probability columns.
	 */
	public PortDef[] getProbabilityColumnsTables() {
		return probabilityColumnsTables;
	}

	/**
	 * Loads settings from the provided {@link NodeSettingsRO}
	 * 
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void loadSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		targetColumn.loadSettingsFrom(settings);
		if (settings.containsKey(KEY_PROBABILITY_COLUMN_FORMAT)) {
			probabilityFormat.loadSettingsFrom(settings);
		}
	}

	/**
	 * Saves current settings into the given {@link NodeSettingsWO}.
	 * 
	 * @param settings
	 */
	public void saveSettingsTo(NodeSettingsWO settings) {
		targetColumn.saveSettingsTo(settings);
		probabilityFormat.saveSettingsTo(settings);
	}

	/**
	 * Validates internal consistency of the current settings
	 * 
	 * @throws InvalidSettingsException
	 */
	public void validate() throws InvalidSettingsException {
		if (getTargetColumn().isEmpty()) {
			throw new InvalidSettingsException("Class column is not selected");
		}

		if (getProbabilityFormat().isEmpty()) {
			throw new InvalidSettingsException("Probability column format is not specified");
		}
	}

	/**
	 * Validates the settings against input table spec. Makes an attempt to
	 * auto-configure settings if the target column is not specified.
	 * 
	 * @param inSpecs     Input specs
	 * @param msgConsumer Warning message consumer
	 * @throws InvalidSettingsException
	 */
	public void validateSettings(DataTableSpec[] inSpecs, Consumer<String> msgConsumer)
			throws InvalidSettingsException {
		if (getTargetColumn().isEmpty()) {
			attemptAutoconfig(inSpecs, msgConsumer);
		}

		DataColumnSpec columnSpec = inSpecs[targetColumnTable.getIdx()].getColumnSpec(getTargetColumn());
		if (!columnSpec.getDomain().hasValues() || columnSpec.getDomain().getValues().isEmpty()) {
			throw new InvalidSettingsException(
					targetColumnTable.getName() + ": Insufficient domain information for column: " + getTargetColumn());
		}

		Set<DataCell> values = columnSpec.getDomain().getValues();

		for (PortDef table : probabilityColumnsTables) {
			for (DataCell cell : values) {
				String value = cell.toString();
				String pColumnName = getProbabilityColumnName(value);
				if (!inSpecs[table.getIdx()].containsName(pColumnName)) {
					throw new InvalidSettingsException(
							table.getName() + ": Probability column not found: " + pColumnName);
				}
			}
		}

		validate();
	}

	private void attemptAutoconfig(DataTableSpec[] inSpecs, Consumer<String> msgConsumer) {
		String[] columnNames = inSpecs[targetColumnTable.getIdx()].getColumnNames();
		for (String column : columnNames) {
			try {
				targetColumn.setStringValue(column);
				validateSettings(inSpecs, msgConsumer);
				msgConsumer.accept(String.format("Node autoconfigured with '%s' column", column));
				return;
			} catch (InvalidSettingsException e) {
				targetColumn.setStringValue("");
			}
		}
	}
}
