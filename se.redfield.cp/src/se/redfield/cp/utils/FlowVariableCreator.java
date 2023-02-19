/*
 * Copyright (c) 2022 Redfield AB.
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
package se.redfield.cp.utils;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.defaultnodesettings.SettingsModelFlowVariableCompatible;

/**
 * Interface that provides a way to create and register flow variables.
 * 
 * @author Alexander Bondaletov, Redfield SE
 *
 */
@FunctionalInterface
public interface FlowVariableCreator {
	/**
	 * Create model and register a new variable for a specific settings object.
	 *
	 * @param dc settings object of corresponding DialogComponent
	 * @return new FlowVariableModel which is already registered
	 */
	FlowVariableModel createFlowVariableModel(final SettingsModelFlowVariableCompatible dc);
}
