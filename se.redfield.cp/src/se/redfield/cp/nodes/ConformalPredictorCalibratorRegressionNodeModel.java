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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

import se.redfield.cp.CalibratorRegression;

/**
 * Conformal Calibrator node. Assign ranks to each row bases on prediction
 * probability.
 *
 */
public class ConformalPredictorCalibratorRegressionNodeModel extends AbstractConformalPredictorRegressionNodeModel {

	private final CalibratorRegression calibrator = new CalibratorRegression(this);

	protected ConformalPredictorCalibratorRegressionNodeModel() {
		super(1, 1);
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec inSpec = inSpecs[0];
		validateSettings(inSpec);

		return new DataTableSpec[] { calibrator.createOutputSpec(inSpec) };
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		return new BufferedDataTable[] { calibrator.process(inData[0], exec) };
	}
}
