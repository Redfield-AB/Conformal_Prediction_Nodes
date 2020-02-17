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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataTableSpec;

/**
 * Utility class used to extract column names from {@link DataTableSpec} based
 * on regex.
 *
 */
public class ColumnPatternExtractor {

	public static final String DEF_GROUP_NAME = "value";

	private Pattern pattern;
	private String groupName;

	/**
	 * Creates instance with the default group name
	 * 
	 * @param regex Regular expression.
	 */
	public ColumnPatternExtractor(String regex) {
		this(regex, DEF_GROUP_NAME);
	}

	/**
	 * Creates instance.
	 * 
	 * @param regex     Regular expression
	 * @param groupName Group name used to extract value from column name
	 */
	public ColumnPatternExtractor(String regex, String groupName) {
		this.pattern = Pattern.compile(regex);
		this.groupName = groupName;
	}

	/**
	 * Test each column from provided spec against regex. Extract value marked by
	 * groupName to associate with column
	 * 
	 * @param spec Table spec.
	 * @return Map consists of extracted values and column indexes that matched the
	 *         regex.
	 */
	public Map<String, Integer> match(DataTableSpec spec) {
		Map<String, Integer> result = new HashMap<>();
		String[] names = spec.getColumnNames();
		for (int i = 0; i < names.length; i++) {
			Matcher m = pattern.matcher(names[i]);
			if (m.matches()) {
				result.put(m.group(groupName), i);
			}
		}
		return result;
	}
}
