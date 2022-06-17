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
import java.util.stream.Collectors;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableOperator;

import se.redfield.cp.Calibrator;
import se.redfield.cp.Predictor;
//import se.redfield.cp.nodes.ConformalPredictorClassifierNodeModel.ClassifierCellFactory;
import se.redfield.cp.utils.ColumnPatternExtractor;

/**
 * Conformal Classifier node. Assigns predicted classes to each row based on
 * it's P-values and selected Significance Level. Works with classes column
 * represented as Collection or String column
 *
 */
public class CompactConformalClassificationNodeModel extends AbstractConformalPredictorNodeModel {
	@SuppressWarnings("unused")
	private static final NodeLogger LOGGER = NodeLogger.getLogger(CompactConformalClassificationNodeModel.class);

	public static final int PORT_PREDICTION_TABLE = 1;
	public static final int PORT_CALIBRATION_TABLE = 0;

	private static final String KEY_ERROR_RATE = "errorRate";
	private static final String KEY_CLASSES_AS_STRING = "classesAsString";
	private static final String KEY_STRING_SEPARATOR = "stringSeparator";

	private static final double DEFAULT_ERROR_RATE = 0.05;
	private static final String DEFAULT_SEPARATOR = ";";
	public static final String DEFAULT_CLASSES_COLUMN_NAME = "Classes";

	private final SettingsModelDoubleBounded errorRateSettings = createErrorRateSettings();
	private final SettingsModelBoolean classesAsStringSettings = createClassesAsStringSettings();
	private final SettingsModelString stringSeparatorSettings = createStringSeparatorSettings();

	private Calibrator calibrator = new Calibrator(this);
	private Predictor predictor= new Predictor(this);
	private ColumnRearranger rearranger;
	private Map<String, Integer> scoreColumns;

	static SettingsModelDoubleBounded createErrorRateSettings() {
		return new SettingsModelDoubleBounded(KEY_ERROR_RATE, DEFAULT_ERROR_RATE, 0, 1);
	}

	static SettingsModelBoolean createClassesAsStringSettings() {
		return new SettingsModelBoolean(KEY_CLASSES_AS_STRING, false);
	}

	static SettingsModelString createStringSeparatorSettings() {
		return new SettingsModelString(KEY_STRING_SEPARATOR, DEFAULT_SEPARATOR);
	}

	protected CompactConformalClassificationNodeModel(boolean visibleTarget) {
		super(2, 1, visibleTarget);
	}

	public double getErrorRate() {
		return errorRateSettings.getDoubleValue();
	}

	public boolean getClassesAsString() {
		return classesAsStringSettings.getBooleanValue();
	}

	public String getStringSeparator() {
		return stringSeparatorSettings.getStringValue();
	}

	private String getScoreColumnPattern() {
		return ConformalPredictorLoopEndNodeModel.P_VALUE_COLUMN_REGEX;
	}

	private String getClassesColumnName() {
		return DEFAULT_CLASSES_COLUMN_NAME;
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		pushFlowVariableDouble(KEY_ERROR_RATE, getErrorRate());
		
		BufferedDataTable inCalibrationTable = inData[PORT_CALIBRATION_TABLE];
		BufferedDataTable inPredictionTable = inData[PORT_PREDICTION_TABLE];
		//Calibrate
		BufferedDataTable calibrationTable = calibrator.process(inCalibrationTable, exec);
	
		//predict
		ColumnRearranger r = predictor.createRearranger(inPredictionTable.getDataTableSpec(), calibrationTable, 
				exec.createSubExecutionContext(0.1));
		inPredictionTable = exec.createColumnRearrangeTable(inPredictionTable, r, exec.createSubProgress(0.9));

		r.append(new ClassifierCellFactory(scoreColumns));
		
		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inPredictionTable, rearranger, exec) };
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		validateSettings(inSpecs[PORT_CALIBRATION_TABLE]);
		validateTables(inSpecs[PORT_CALIBRATION_TABLE], inSpecs[PORT_PREDICTION_TABLE]);
		
		Set<DataCell> values = inSpecs[PORT_CALIBRATION_TABLE].getColumnSpec(getTargetColumnName()).getDomain()
				.getValues();
		scoreColumns = new ColumnPatternExtractor(getScoreColumnPattern()).match(
					predictor.createOuputTableSpec(inSpecs[PORT_PREDICTION_TABLE], values));
		validateSettings(scoreColumns);

		rearranger = createRearranger(predictor.createOuputTableSpec(inSpecs[PORT_PREDICTION_TABLE], values), scoreColumns);

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

		if (getClassesAsString() && getStringSeparator().isEmpty()) {
			throw new InvalidSettingsException("String separator is empty");
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
				if (score > getErrorRate()) {
					classes.add(e.getKey());
				}
			}

			DataCell result;
			if (classes.isEmpty()) {
				result = new MissingCell("No class asigned");
			} else {
				if (getClassesAsString()) {
					result = new StringCell(String.join(getStringSeparator(), classes));
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
		DataType type = getClassesAsString() ? StringCell.TYPE : SetCell.getCollectionType(StringCell.TYPE);
		return new DataColumnSpecCreator(getClassesColumnName(), type).createSpec();
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
		errorRateSettings.saveSettingsTo(settings);
		classesAsStringSettings.saveSettingsTo(settings);
		stringSeparatorSettings.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		errorRateSettings.validateSettings(settings);
		classesAsStringSettings.validateSettings(settings);
		stringSeparatorSettings.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		errorRateSettings.loadSettingsFrom(settings);
		classesAsStringSettings.loadSettingsFrom(settings);
		stringSeparatorSettings.loadSettingsFrom(settings);
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
