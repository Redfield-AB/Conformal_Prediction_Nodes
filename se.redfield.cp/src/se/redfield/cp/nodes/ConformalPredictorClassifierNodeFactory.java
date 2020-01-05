package se.redfield.cp.nodes;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class ConformalPredictorClassifierNodeFactory extends NodeFactory<ConformalPredictorClassifierNodeModel> {

	@Override
	public ConformalPredictorClassifierNodeModel createNodeModel() {
		return new ConformalPredictorClassifierNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<ConformalPredictorClassifierNodeModel> createNodeView(int viewIndex,
			ConformalPredictorClassifierNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new ConformalPredictorClassifierNodeDialog();
	}

}
