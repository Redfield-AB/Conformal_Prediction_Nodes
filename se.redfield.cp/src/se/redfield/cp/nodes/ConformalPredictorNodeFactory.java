package se.redfield.cp.nodes;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class ConformalPredictorNodeFactory extends NodeFactory<ConformalPredictorNodeModel> {

	@Override
	public ConformalPredictorNodeModel createNodeModel() {
		return new ConformalPredictorNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<ConformalPredictorNodeModel> createNodeView(int viewIndex, ConformalPredictorNodeModel nodeModel) {
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
