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
package se.redfield.cp.utils;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;

public class ValidationUtil {
	private ValidationUtil() {
	}

	public static void validateDoubleColumn(PortDef table, DataTableSpec[] specs, String column, String title)
			throws InvalidSettingsException {
		validateColumnExists(table, specs, column, title);

		if (!specs[table.getIdx()].getColumnSpec(column).getType().isCompatible(DoubleValue.class)) {
			throw new InvalidSettingsException(
					String.format("%s: Selected %s column '%s' must be numeric.", table.getName(), title, column));
		}
	}

	public static void validateColumnExists(PortDef table, DataTableSpec[] specs, String column, String title)
			throws InvalidSettingsException {
		if (!specs[table.getIdx()].containsName(column)) {
			throw new InvalidSettingsException(
					String.format("Selected %s column '%s' is missing from the %s.", title, column, table.getName()));
		}
	}
}
