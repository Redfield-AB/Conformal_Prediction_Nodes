package se.redfield.cp.nodes;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class ConformalPredictorLoopEndNodeFactory extends NodeFactory<ConformalPredictorLoopEndNodeModel> {

	@Override
	public ConformalPredictorLoopEndNodeModel createNodeModel() {
		return new ConformalPredictorLoopEndNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<ConformalPredictorLoopEndNodeModel> createNodeView(int viewIndex,
			ConformalPredictorLoopEndNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new ConformalPredictorLoopEndNodeDialog();
	}

}
