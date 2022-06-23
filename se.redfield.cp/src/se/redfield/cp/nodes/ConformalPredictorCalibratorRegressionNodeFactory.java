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

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory class for Calibrator node.
 *
 */
public class ConformalPredictorCalibratorRegressionNodeFactory extends NodeFactory<ConformalPredictorCalibratorRegressionNodeModel> {
	private static final boolean visibleTarget = true;
	
	@Override
	public ConformalPredictorCalibratorRegressionNodeModel createNodeModel() {
		return new ConformalPredictorCalibratorRegressionNodeModel(visibleTarget);
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<ConformalPredictorCalibratorRegressionNodeModel> createNodeView(int viewIndex,
			ConformalPredictorCalibratorRegressionNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new ConformalPredictorCalibratorRegressionNodeDialog(visibleTarget);
	}

}