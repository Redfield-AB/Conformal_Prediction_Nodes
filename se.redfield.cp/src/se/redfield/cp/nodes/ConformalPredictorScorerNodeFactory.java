package se.redfield.cp.nodes;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class ConformalPredictorScorerNodeFactory extends NodeFactory<ConformalPredictorScorerNodeModel> {

	@Override
	public ConformalPredictorScorerNodeModel createNodeModel() {
		return new ConformalPredictorScorerNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<ConformalPredictorScorerNodeModel> createNodeView(int viewIndex,
			ConformalPredictorScorerNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new ConformalPredictorScorerNodeDialog();
	}

}
