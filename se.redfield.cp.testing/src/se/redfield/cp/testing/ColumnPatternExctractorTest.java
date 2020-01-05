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

import se.redfield.cp.nodes.ConformalPredictorClassifierNodeModel;
import se.redfield.cp.utils.ColumnPatternExtractor;

class ColumnPatternExctractorTest {

	@Test
	void test() {
		DataColumnSpec c1 = new DataColumnSpecCreator("Score (1)", StringCell.TYPE).createSpec();
		DataColumnSpec c2 = new DataColumnSpecCreator("Score(2)", StringCell.TYPE).createSpec();
		DataColumnSpec c3 = new DataColumnSpecCreator("Score ()", StringCell.TYPE).createSpec();
		DataColumnSpec c4 = new DataColumnSpecCreator("Score (4)", StringCell.TYPE).createSpec();
		DataColumnSpec c5 = new DataColumnSpecCreator("Some string", StringCell.TYPE).createSpec();
		DataTableSpec spec = new DataTableSpec(c1, c2, c3, c4, c5);

		ColumnPatternExtractor e = new ColumnPatternExtractor(
				ConformalPredictorClassifierNodeModel.DEFAULT_SCORE_COLUMN_PATTERN);

		Map<String, Integer> match = e.match(spec);
		assertThat(match.size(), is(2));
		assertThat(match.get("1"), equalTo(0));
		assertThat(match.get("4"), equalTo(3));
	}

}
