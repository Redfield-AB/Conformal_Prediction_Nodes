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
 * Factory class for {@link ConformalPartitionNodeModel}.
 * 
 */
public class ConformalPartitionNodeFactory extends NodeFactory<ConformalPartitionNodeModel> {
    @Override
	public ConformalPartitionNodeModel createNodeModel() {
        return new ConformalPartitionNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new ConformalPartitionNodeDialog();
    }

	@Override
	public NodeView<ConformalPartitionNodeModel> createNodeView(int viewIndex, ConformalPartitionNodeModel nodeModel) {
		return null;
	}
}
