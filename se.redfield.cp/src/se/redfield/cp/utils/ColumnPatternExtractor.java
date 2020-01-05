package se.redfield.cp.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataTableSpec;

public class ColumnPatternExtractor {

	public static final String DEF_GROUP_NAME = "value";

	private Pattern pattern;
	private String groupName;

	public ColumnPatternExtractor(String regex) {
		this(regex, DEF_GROUP_NAME);
	}

	public ColumnPatternExtractor(String regex, String groupName) {
		this.pattern = Pattern.compile(regex);
		this.groupName = groupName;
	}

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
