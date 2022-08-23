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
package se.redfield.cp.utils;

/**
 * The node input port definition.
 * 
 * @author Alexander Bondaletov
 *
 */
public class PortDef {

	private final int idx;
	private final String name;

	/**
	 * @param idx  The port index.
	 * @param name The port name.
	 */
	public PortDef(int idx, String name) {
		this.idx = idx;
		this.name = name;
	}

	/**
	 * @return The port index.
	 */
	public int getIdx() {
		return idx;
	}

	/**
	 * @return The port name.
	 */
	public String getName() {
		return name;
	}
}
