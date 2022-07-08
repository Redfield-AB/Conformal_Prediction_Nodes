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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.RowKey;

/**
 * Utility class
 *
 */
public class KnimeUtils {

	private static final String SEPARATOR = "_";

	private KnimeUtils() {
	}

	/**
	 * Creates new {@link RowKey} by adding an index suffix to the existing one.
	 * 
	 * @param base  Source {@link RowKey}
	 * @param index Index to be added.
	 * @return New {@link RowKey}
	 */
	public static RowKey createRowKey(RowKey base, int index) {
		return createRowKey(base, String.valueOf(index));
	}

	/**
	 * Creates new {@link RowKey} by adding a suffix to the existing one.
	 * 
	 * @param base   Source {@link RowKey}
	 * @param suffix Suffix to be added.
	 * @return New {@link RowKey}
	 */
	public static RowKey createRowKey(RowKey base, String suffix) {
		return new RowKey(base.getString() + SEPARATOR + suffix);
	}

	/**
	 * Creates new {@link DataTableSpec} by appending provided columns to existing
	 * {@link DataTableSpec}
	 * 
	 * @param base   Original {@link DataTableSpec}
	 * @param colums Columns to be added to original {@link DataTableSpec}
	 * @return New {@link DataTableSpec}
	 */
	public static DataTableSpec createSpec(DataTableSpec base, DataColumnSpec... colums) {
		return new DataTableSpec(base, new DataTableSpec(colums));
	}

	/**
	 * Ensures that a cell is not a missing value. Throws
	 * {@link MissingValueException} in case it is.
	 * 
	 * @param cell    The data cell
	 * @param message The message provided to the {@link MissingValueException}.
	 * @return The cell, if it is not missing.
	 * @throws MissingValueException If the provided cell is a missing value.
	 */
	public static DataCell nonMissing(DataCell cell, String message) throws MissingValueException {
		if (cell.isMissing()) {
			throw new MissingValueException((MissingValue) cell, message);
		}
		return cell;
	}

	/**
	 * Gets the double value from the provided {@link DataCell}. Throws
	 * {@link MissingValueException} in case the cell is a missing value.
	 * 
	 * @param cell    The data cell.
	 * @param message The message provided to the {@link MissingValueException}.
	 * @return The double value contained in the cell.
	 * @throws MissingValueException If the provided cell is a missing value.
	 */
	public static double getDouble(DataCell cell, String message) throws MissingValueException {
		return ((DoubleValue) nonMissing(cell, message)).getDoubleValue();
	}
}
