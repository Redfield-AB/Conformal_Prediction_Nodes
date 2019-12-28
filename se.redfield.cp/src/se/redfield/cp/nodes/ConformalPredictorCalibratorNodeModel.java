package se.redfield.cp.nodes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class ConformalPredictorCalibratorNodeModel extends NodeModel {

	/**
	 * The logger is used to print info/warning/error messages to the KNIME console
	 * and to the KNIME log file. Retrieve it via 'NodeLogger.getLogger' providing
	 * the class of this node model.
	 */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(ConformalPredictorCalibratorNodeModel.class);

	private static final String KEY_COLUMN_NAME = "columnName";

	public static final String P_COLUMN_DEFAULT_NAME = "Calibration P";
	private static final String SCORE_COLUMN_DEFAULT_NAME = "Calibration Score";

	private final SettingsModelString columnNameSettings = createColumnNameSettingsModel();

	/**
	 * Constructor for the node model.
	 */
	protected ConformalPredictorCalibratorNodeModel() {
		super(1, 1);
	}

	static SettingsModelString createColumnNameSettingsModel() {
		return new SettingsModelString(KEY_COLUMN_NAME, "");
	}

	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		LOGGER.info("This is an example info.");
		LOGGER.info("Column: " + columnNameSettings.getStringValue());
		BufferedDataTable inTable = inData[0];

		ColumnRearranger appendProbabilityRearranger = new ColumnRearranger(inTable.getDataTableSpec());
		appendProbabilityRearranger.append(createPCellFactory(inTable.getDataTableSpec()));

		BufferedDataTable appendedProbabilityTable = exec.createColumnRearrangeTable(inTable,
				appendProbabilityRearranger, exec);

		List<String> sortingColumns = new ArrayList<>();
		sortingColumns.add(columnNameSettings.getStringValue());
		sortingColumns.add(P_COLUMN_DEFAULT_NAME);
		BufferedDataTableSorter sorter = new BufferedDataTableSorter(appendedProbabilityTable, sortingColumns,
				new boolean[] { true, false });
		BufferedDataTable sortedTable = sorter.sort(exec);

		ColumnRearranger appendScoreRearranger = new ColumnRearranger(sortedTable.getDataTableSpec());
		appendScoreRearranger.append(createScoreCellFactory(inTable.getSpec()));

		BufferedDataTable appendedScoreTable = exec.createColumnRearrangeTable(sortedTable, appendScoreRearranger,
				exec);

		return new BufferedDataTable[] { appendedScoreTable };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		return new DataTableSpec[] { createOutputSpec(inSpecs[0]) };
	}

	/**
	 * Creates the output table spec from the input spec.
	 * 
	 * @param inputTableSpec
	 * @return
	 */
	private DataTableSpec createOutputSpec(DataTableSpec inputTableSpec) {
		ColumnRearranger rearranger = new ColumnRearranger(inputTableSpec);
		rearranger.append(createPCellFactory(inputTableSpec));
		rearranger.append(createScoreCellFactory(inputTableSpec));
		return rearranger.createSpec();
	}

	private CellFactory createPCellFactory(DataTableSpec inputTableSpec) {
		int columnIndex = inputTableSpec.findColumnIndex(columnNameSettings.getStringValue());
		Map<String, Integer> probabilityColumns = inputTableSpec.getColumnSpec(columnIndex).getDomain().getValues()
				.stream().map(c -> c.toString()).collect(Collectors.toMap(str -> str, str -> inputTableSpec
						.findColumnIndex(String.format("P (%s=%s)", columnNameSettings.getStringValue(), str))));

		return new AbstractCellFactory(new DataColumnSpecCreator(P_COLUMN_DEFAULT_NAME, DoubleCell.TYPE).createSpec()) {

			@Override
			public DataCell[] getCells(DataRow row) {
				DataCell dataCell = row.getCell(columnIndex);
				Integer probabilityCol = probabilityColumns.get(dataCell.toString());

				return new DataCell[] { row.getCell(probabilityCol) };
			}
		};
	}

	private CellFactory createScoreCellFactory(DataTableSpec inputTableSpec) {
		int columnIndex = inputTableSpec.findColumnIndex(columnNameSettings.getStringValue());
		return new AbstractCellFactory(
				new DataColumnSpecCreator(SCORE_COLUMN_DEFAULT_NAME, LongCell.TYPE).createSpec()) {

			private long counter = 0;
			private String prevValue = null;

			@Override
			public DataCell[] getCells(DataRow row) {
				String value = row.getCell(columnIndex).toString();
				if (prevValue == null || !prevValue.equals(value)) {
					counter = 0;
					prevValue = value;
				}

				return new DataCell[] { new LongCell(counter++) };
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		columnNameSettings.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		columnNameSettings.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		columnNameSettings.validateSettings(settings);
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		/*
		 * Advanced method, usually left empty. Everything that is handed to the output
		 * ports is loaded automatically (data returned by the execute method, models
		 * loaded in loadModelContent, and user settings set through loadSettingsFrom -
		 * is all taken care of). Only load the internals that need to be restored (e.g.
		 * data used by the views).
		 */
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		/*
		 * Advanced method, usually left empty. Everything written to the output ports
		 * is saved automatically (data returned by the execute method, models saved in
		 * the saveModelContent, and user settings saved through saveSettingsTo - is all
		 * taken care of). Save only the internals that need to be preserved (e.g. data
		 * used by the views).
		 */
	}

	@Override
	protected void reset() {
		/*
		 * Code executed on a reset of the node. Models built during execute are cleared
		 * and the data handled in loadInternals/saveInternals will be erased.
		 */
	}
}
