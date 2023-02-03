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

import se.redfield.cp.settings.PredictiveSystemsRegressionSettings;

/**
 * Node dialog for Classifier node
 *
 */
public class PredictiveSystemsClassifierNodeDialog extends DefaultNodeSettingsPane {

	private final PredictiveSystemsRegressionSettings settings = new PredictiveSystemsRegressionNodeSettings();

	/**
	 * Creates new instance
	 */
	public PredictiveSystemsClassifierNodeDialog() {
		super();

		// Instead of just an error rate, I think it would make more sense to provide
		// sets of lower and upper percentiles.
		// Compare with the example in 3.2 in the juptyer nootebook for crepes:
		// results = cps_mond_norm.predict(y_hat=y_hat_test,
		// sigmas=sigmas_test_knn,
		// bins=bins_test,
		// y=y_test,
		// lower_percentiles=[2.5, 5],
		// higher_percentiles=[95, 97.5])

		// We should also consider adding the option of either pointing out a y-column
		// or set a constant y-value to calculate the p-values:
		// The output of the `predict` method of a `ConformalPredictiveSystem` will
		// depend on how we specify the input.
		// If we provide specific target values (using the parameter `y`), the method
		// will output a p-value for each test instance, i.e.,
		// the probability that the true target is less than or equal to the provided
		// values. The method assumes that either one value is
		// provided for each test instance or that the same (single) value is provided
		// for all test instances.

	}

}
