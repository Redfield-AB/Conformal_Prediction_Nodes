package se.redfield.cp.nodes;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class ConformalPredictorLoopStartNodeFactory extends NodeFactory<ConformalPredictorLoopStartNodeModel> {

	@Override
	public ConformalPredictorLoopStartNodeModel createNodeModel() {
		return new ConformalPredictorLoopStartNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<ConformalPredictorLoopStartNodeModel> createNodeView(int viewIndex,
			ConformalPredictorLoopStartNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new ConformalPredictorLoopStartNodeDialog();
	}

}
