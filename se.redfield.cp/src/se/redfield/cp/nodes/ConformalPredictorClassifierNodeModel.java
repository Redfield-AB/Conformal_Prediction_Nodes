package se.redfield.cp.nodes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;

import se.redfield.cp.utils.ColumnPatternExtractor;

public class ConformalPredictorClassifierNodeModel extends NodeModel {

	private static final String KEY_SCORE_THRESHOLD = "scoreThreshold";

	private static final double DEFAULT_SCORE_THRESHOLD = 0.8;
	public static final String DEFAULT_SCORE_COLUMN_PATTERN = "^Score \\((?<value>.+)\\)$";

	private static final SettingsModelDoubleBounded scoreThresholdSettings = createScoreThresholdSettings();

	static SettingsModelDoubleBounded createScoreThresholdSettings() {
		return new SettingsModelDoubleBounded(KEY_SCORE_THRESHOLD, DEFAULT_SCORE_THRESHOLD, 0, 1);
	}

	protected ConformalPredictorClassifierNodeModel() {
		super(1, 1);
	}

	private double getScoreThreshold() {
		return scoreThresholdSettings.getDoubleValue();
	}

	private String getScoreColumnPattern() {
		return DEFAULT_SCORE_COLUMN_PATTERN;
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		BufferedDataTable inTable = inData[0];
		ColumnRearranger rearranger = createRearranger(inTable.getDataTableSpec());
		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inTable, rearranger, exec) };
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		return new DataTableSpec[] { createOutTableSpec(inSpecs[0]) };
	}

	private ColumnRearranger createRearranger(DataTableSpec inSpec) {
		ColumnRearranger rearranger = new ColumnRearranger(inSpec);
		rearranger.append(new ClassifierCellFactory(inSpec));
		return rearranger;
	}

	private class ClassifierCellFactory extends AbstractCellFactory {

		private Map<String, Integer> scoreColumns;

		public ClassifierCellFactory(DataTableSpec inTableSpec) {
			super(createClassColumnSpec());
			ColumnPatternExtractor extractor = new ColumnPatternExtractor(getScoreColumnPattern());
			scoreColumns = extractor.match(inTableSpec);
		}

		@Override
		public DataCell[] getCells(DataRow row) {
			List<DataCell> classes = new ArrayList<>();
			for (Entry<String, Integer> e : scoreColumns.entrySet()) {
				double score = ((DoubleValue) row.getCell(e.getValue())).getDoubleValue();
				if (score > getScoreThreshold()) {
					classes.add(new StringCell(e.getKey()));
				}
			}
			return new DataCell[] { CollectionCellFactory.createSetCell(classes) };
		}

	}

	private DataTableSpec createOutTableSpec(DataTableSpec inSpec) {
		DataColumnSpec[] colls = new DataColumnSpec[inSpec.getNumColumns() + 1];
		for (int i = 0; i < colls.length - 1; i++) {
			colls[i] = inSpec.getColumnSpec(i);
		}
		colls[colls.length - 1] = createClassColumnSpec();
		return new DataTableSpec(colls);
	}

	private DataColumnSpec createClassColumnSpec() {
		return new DataColumnSpecCreator("Classes", SetCell.getCollectionType(StringCell.TYPE)).createSpec();
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		scoreThresholdSettings.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		scoreThresholdSettings.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		scoreThresholdSettings.loadSettingsFrom(settings);
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void reset() {
		// TODO Auto-generated method stub

	}

}
