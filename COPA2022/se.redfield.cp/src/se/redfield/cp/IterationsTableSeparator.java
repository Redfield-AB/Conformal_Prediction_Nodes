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
package se.redfield.cp;

import java.io.Closeable;
import java.util.NoSuchElementException;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;


/**
 * Class used to split a table by iterations.<br/>
 * 
 * Takes a table containing "Iteration column" and produces sequence of tables,
 * where each new table corresponds to a single iteration.<br/>
 * 
 * Iterations doesn't have to be consequent numbers, but table has to be
 * grouped/sorted by the iteration value.
 *
 */
public class IterationsTableSeparator implements Closeable {

	private CloseableRowIterator iterator;
	private int iterationColumn;
	private DataTableSpec spec;

	private int iteration;
	private DataRow row;

	/**
	 * Creates new instance.
	 * 
	 * @param inTable             Input table with iteration column.
	 * @param iterationColumnName Iteration column name.
	 */
	public IterationsTableSeparator(BufferedDataTable inTable, String iterationColumnName) {
		iterator = inTable.iterator();
		iterationColumn = inTable.getSpec().findColumnIndex(iterationColumnName);
		spec = inTable.getSpec();
	}

	/**
	 * Gets the next segment corresponding to a single iteration.
	 * 
	 * @param exec Execution context.
	 * @return Table with the next segment.
	 * @throws NoSuchElementException If no next segment is available.
	 */
	public BufferedDataTable next(ExecutionContext exec) throws CanceledExecutionException {
		iteration = getNextIteration();

		BufferedDataContainer cont = exec.createDataContainer(spec);

		do {
			cont.addRowToTable(row);
			row = iterator.hasNext() ? iterator.next() : null;
			exec.checkCanceled();
		} while (row != null && getIteration(row) == iteration);

		cont.close();
		return cont.getTable();
	}

	/**
	 * Returns <code>true</code> if there are next segment available.
	 * 
	 */
	public boolean hasNext() {
		return row != null || iterator.hasNext();
	}

	/**
	 * Returns last iteration value.
	 * 
	 * @return Iteration value corresponding to the last segment returned by the
	 *         <code>next()</code> method.
	 */
	public int getLastIteration() {
		return iteration;
	}

	/**
	 * Returns next iteration value.
	 * 
	 * @return Iteration value corresponding to the next segment that will be
	 *         returned by the <code>next()</code> method.
	 * 
	 * @throws NoSuchElementException If no next segment is available.
	 */
	public int getNextIteration() {
		if (row == null) {
			row = iterator.next();
		}
		return getIteration(row);
	}

	/**
	 * Fetches iteration value from the {@link DataRow}
	 * 
	 * @param r Data row
	 * @return Iteration value
	 */
	private int getIteration(DataRow r) {
		return ((IntCell) r.getCell(iterationColumn)).getIntValue();
	}

	/**
	 * Closes internal {@link CloseableRowIterator} object.
	 */
	public void close() {
		if (iterator != null) {
			iterator.close();
		}
	}
}
