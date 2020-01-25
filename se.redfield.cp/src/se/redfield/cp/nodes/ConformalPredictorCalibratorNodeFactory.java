package se.redfield.cp.nodes;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class ConformalPredictorCalibratorNodeFactory extends NodeFactory<ConformalPredictorCalibratorNodeModel> {

	@Override
	public ConformalPredictorCalibratorNodeModel createNodeModel() {
		return new ConformalPredictorCalibratorNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<ConformalPredictorCalibratorNodeModel> createNodeView(int viewIndex,
			ConformalPredictorCalibratorNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new ConformalPredictorNodeDialog();
	}

}
