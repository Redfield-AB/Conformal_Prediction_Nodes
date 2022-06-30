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

public class TargetSettings {

	private static final String KEY_TARGET_COLUMN_NAME = "targetColumn";
	private static final String KEY_PROBABILITY_COLUMN_FORMAT = "probabilityColumnFormat";

	private static final String DEFAULT_FORMAT = "P (%1$s=%2$s)";

	private final SettingsModelString targetColumn;
	private final SettingsModelString probabilityFormat;

	private final PortDef targetColumnTable;
	private final PortDef[] probabilityColumnsTables;

	public TargetSettings(PortDef targetColumnTable, PortDef... probabilityColumnsTables) {
		this.targetColumnTable = targetColumnTable;
		this.probabilityColumnsTables = probabilityColumnsTables;

		targetColumn = new SettingsModelString(KEY_TARGET_COLUMN_NAME, "");
		probabilityFormat = new SettingsModelString(KEY_PROBABILITY_COLUMN_FORMAT, DEFAULT_FORMAT);
	}

	public SettingsModelString getTargetColumnModel() {
		return targetColumn;
	}

	public String getTargetColumn() {
		return targetColumn.getStringValue();
	}

	public SettingsModelString getProbabilityFormatModel() {
		return probabilityFormat;
	}

	public String getProbabilityFormat() {
		return probabilityFormat.getStringValue();
	}

	public String getProbabilityColumnName(String value) {
		return String.format(String.format(getProbabilityFormat(), getTargetColumn(), value));
	}

	public PortDef getTargetColumnTable() {
		return targetColumnTable;
	}

	public PortDef[] getProbabilityColumnsTables() {
		return probabilityColumnsTables;
	}

	public void loadSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		targetColumn.loadSettingsFrom(settings);
		if (settings.containsKey(KEY_PROBABILITY_COLUMN_FORMAT)) {
			probabilityFormat.loadSettingsFrom(settings);
		}
	}

	public void saveSettingsTo(NodeSettingsWO settings) {
		targetColumn.saveSettingsTo(settings);
		probabilityFormat.saveSettingsTo(settings);
	}

	public void validate() throws InvalidSettingsException {
		if (getTargetColumn().isEmpty()) {
			throw new InvalidSettingsException("Class column is not selected");
		}

		if (getProbabilityFormat().isEmpty()) {
			throw new InvalidSettingsException("Probability column format is not specified");
		}
	}

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
