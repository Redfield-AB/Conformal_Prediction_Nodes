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

import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * Node dialog for Conformal Predictor node.
 *
 */
public class ConformalPredictorNodeDialog extends AbstractConformalPredictorNodeDialog {

	public ConformalPredictorNodeDialog() {
		super(ConformalPredictorNodeModel.PORT_PREDICTION_TABLE);

		SettingsModelBoolean includeRank = ConformalPredictorNodeModel.createIncludeRankSettings();

		addDialogComponent(new DialogComponentBoolean(includeRank, "Include Rank column"));
	}
}
