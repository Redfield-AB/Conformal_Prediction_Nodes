package se.redfield.cp.utils;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;

public class KnimeUtils {

	private static final String SEPARATOR = "_";

	public static RowKey createRowKey(RowKey base, int index) {
		return createRowKey(base, String.valueOf(index));
	}

	public static RowKey createRowKey(RowKey base, String suffix) {
		return new RowKey(base.getString() + SEPARATOR + suffix);
	}

	public static DataTableSpec createSpec(DataTableSpec base, DataColumnSpec... colums) {
		return new DataTableSpec(base, new DataTableSpec(colums));
	}
}
