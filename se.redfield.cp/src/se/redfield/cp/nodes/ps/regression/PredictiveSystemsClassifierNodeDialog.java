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
package se.redfield.cp.nodes.ps.regression;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;

import se.redfield.cp.settings.ClassifierSettings;

/**
 * Node dialog for Classifier node
 *
 */
public class PredictiveSystemsClassifierNodeDialog extends DefaultNodeSettingsPane {

	private final ClassifierSettings settings = new ClassifierSettings();

	/**
	 * Creates new instance
	 */
	public PredictiveSystemsClassifierNodeDialog() {
		super();

		addDialogComponent(new DialogComponentNumber(settings.getErrorRateModel(), "Error rate (significance level)",
				0.01, createFlowVariableModel(settings.getErrorRateModel())));
		createNewGroup("Output");
		addDialogComponent(new DialogComponentBoolean(settings.getClassesAsStringModel(), "Output Classes as String"));
		addDialogComponent(new DialogComponentString(settings.getStringSeparatorModel(), "String separator"));
	}

}
