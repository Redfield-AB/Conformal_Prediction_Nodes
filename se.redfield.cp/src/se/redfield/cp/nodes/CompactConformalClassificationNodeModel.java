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
package se.redfield.cp.nodes;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableOperator;

import se.redfield.cp.Calibrator;
import se.redfield.cp.Predictor;
import se.redfield.cp.settings.CompactClassificationNodeSettigns;
//import se.redfield.cp.nodes.ConformalPredictorClassifierNodeModel.ClassifierCellFactory;
import se.redfield.cp.utils.ColumnPatternExtractor;
import se.redfield.cp.utils.PortDef;

/**
 * Conformal Classifier node. Assigns predicted classes to each row based on
 * it's P-values and selected Significance Level. Works with classes column
 * represented as Collection or String column
 *
 */
public class CompactConformalClassificationNodeModel extends NodeModel {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(CompactConformalClassificationNodeModel.class);

	public static final PortDef PORT_PREDICTION_TABLE = new PortDef(1, "Prediction table");
	public static final PortDef PORT_CALIBRATION_TABLE = new PortDef(0, "Calibration table");

	private CompactClassificationNodeSettigns settings = new CompactClassificationNodeSettigns();
	private Calibrator calibrator = new Calibrator(settings);
	private Predictor predictor = new Predictor(settings);
	private ColumnRearranger rearranger;

	protected CompactConformalClassificationNodeModel() {
		super(2, 1);
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		// pushFlowVariableDouble(KEY_ERROR_RATE, getErrorRate());

		BufferedDataTable inCalibrationTable = inData[PORT_CALIBRATION_TABLE.getIdx()];
		BufferedDataTable inPredictionTable = inData[PORT_PREDICTION_TABLE.getIdx()];
		// Calibrate
		BufferedDataTable calibrationTable = calibrator.process(inCalibrationTable, exec);

		// predict
		ColumnRearranger r = predictor.createRearranger(inPredictionTable.getDataTableSpec(), calibrationTable,
				exec.createSubExecutionContext(0.1));
		inPredictionTable = exec.createColumnRearrangeTable(inPredictionTable, r, exec.createSubProgress(0.9));

		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inPredictionTable, rearranger, exec) };
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		settings.validateSettings(inSpecs);

		Map<String, Integer> scoreColumns = new ColumnPatternExtractor(settings.getScoreColumnPattern())
				.match(predictor.createOuputTableSpec(inSpecs[PORT_CALIBRATION_TABLE.getIdx()],
						inSpecs[PORT_PREDICTION_TABLE.getIdx()]));
		validateSettings(scoreColumns);

		rearranger = createRearranger(predictor.createOuputTableSpec(inSpecs[PORT_CALIBRATION_TABLE.getIdx()],
				inSpecs[PORT_PREDICTION_TABLE.getIdx()]), scoreColumns);

		return new DataTableSpec[] { rearranger.createSpec() };
	}

	/**
	 * Validated settings.
	 * 
	 * @param scoreColumns Score columns collected from the input table.
	 * @throws InvalidSettingsException If scoreColumns is empty or if string
	 *                                  separator is empty in case String output
	 *                                  mode is selected
	 */
	private void validateSettings(Map<String, Integer> scoreColumns) throws InvalidSettingsException {
		if (scoreColumns.isEmpty()) {
			throw new InvalidSettingsException("No p-values columns found in provided table");
		}

	}

	/**
	 * Creates ColumnRearranger
	 * 
	 * @param inSpec       Input table spec
	 * @param scoreColumns Collected score columns
	 * @return rearranger
	 */
	private ColumnRearranger createRearranger(DataTableSpec inSpec, Map<String, Integer> scoreColumns) {
		ColumnRearranger r = new ColumnRearranger(inSpec);
		r.append(new ClassifierCellFactory(scoreColumns));
		return r;
	}

	/**
	 * CellFactory used to create Classes column. Collects all classes that has
	 * P-value greater than selected threshold.
	 *
	 */
	private class ClassifierCellFactory extends AbstractCellFactory {

		private Map<String, Integer> scoreColumns;

		public ClassifierCellFactory(Map<String, Integer> scoreColumns) {
			super(createClassColumnSpec());
			this.scoreColumns = scoreColumns;
		}

		@Override
		public DataCell[] getCells(DataRow row) {
			Set<String> classes = new HashSet<>();

			for (Entry<String, Integer> e : scoreColumns.entrySet()) {
				double score = ((DoubleValue) row.getCell(e.getValue())).getDoubleValue();
				if (score > settings.getErrorRate()) {
					classes.add(e.getKey());
				}
			}

			DataCell result;
			if (classes.isEmpty()) {
				result = new MissingCell("No class asigned");
			} else {
				if (settings.getClassesAsString()) {
					result = new StringCell(String.join(settings.getStringSeparator(), classes));
				} else {
					result = CollectionCellFactory
							.createSetCell(classes.stream().map(StringCell::new).collect(toList()));
				}
			}

			return new DataCell[] { result };
		}

	}

	/**
	 * Created {@link DataColumnSpec} from classes column
	 * 
	 * @return
	 */
	private DataColumnSpec createClassColumnSpec() {
		DataType type = settings.getClassesAsString() ? StringCell.TYPE : SetCell.getCollectionType(StringCell.TYPE);
		return new DataColumnSpecCreator(settings.getClassesColumnName(), type).createSpec();
	}

	@Override
	public StreamableOperator createStreamableOperator(PartitionInfo partitionInfo, PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		return rearranger.createStreamableFunction();
	}

	@Override
	public InputPortRole[] getInputPortRoles() {
		return new InputPortRole[] { InputPortRole.DISTRIBUTED_STREAMABLE };
	}

	@Override
	public OutputPortRole[] getOutputPortRoles() {
		return new OutputPortRole[] { OutputPortRole.DISTRIBUTED };
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		this.settings.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		this.settings.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		this.settings.loadSettingFrom(settings);
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no internals
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no internals
	}

	@Override
	protected void reset() {
		rearranger = null;
	}

}
