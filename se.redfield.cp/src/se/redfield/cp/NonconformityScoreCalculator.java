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
/**
 * 
 */
package se.redfield.cp;

/**
 * @author loftuw
 *
 */
public class NonconformityScoreCalculator {

	public static double[] absoluteErrorScore(double[] target, double[] prediction) 
	{
		assert(target.length == prediction.length);
		double[] absError = new double[target.length]; 
		for (int i = 0; i < target.length; i++)
		{
			absError[i] = Math.abs(target[i] - prediction[i]);
		}
		return absError;
	}
	
	public static double[] signedErrorScore(double[] target, double[] prediction) 
	{
		assert(target.length == prediction.length);
		double[] signedError = new double[target.length]; 
		for (int i = 0; i < target.length; i++)
		{
			signedError[i] = target[i] - prediction[i];
		}
		return signedError;
	}
}
