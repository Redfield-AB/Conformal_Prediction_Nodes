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
package se.redfield.cp.testing;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;

import se.redfield.cp.nodes.ConformalPredictorLoopEndNodeModel;
import se.redfield.cp.utils.ColumnPatternExtractor;

class ColumnPatternExctractorTest {

	@Test
	void test() {
		DataColumnSpec c1 = new DataColumnSpecCreator("P-value (1)", StringCell.TYPE).createSpec();
		DataColumnSpec c2 = new DataColumnSpecCreator("P-value(2)", StringCell.TYPE).createSpec();
		DataColumnSpec c3 = new DataColumnSpecCreator("P-value ()", StringCell.TYPE).createSpec();
		DataColumnSpec c4 = new DataColumnSpecCreator("P-value (4)", StringCell.TYPE).createSpec();
		DataColumnSpec c5 = new DataColumnSpecCreator("Some string", StringCell.TYPE).createSpec();
		DataTableSpec spec = new DataTableSpec(c1, c2, c3, c4, c5);

		ColumnPatternExtractor e = new ColumnPatternExtractor(ConformalPredictorLoopEndNodeModel.P_VALUE_COLUMN_REGEX);

		Map<String, Integer> match = e.match(spec);
		assertThat(match.size(), is(2));
		assertThat(match.get("1"), equalTo(0));
		assertThat(match.get("4"), equalTo(3));
	}

}
