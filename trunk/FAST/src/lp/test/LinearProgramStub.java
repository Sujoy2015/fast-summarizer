package lp.test;

import lp.optimization.GoalType;
import lp.optimization.OptimizationException;
import lp.optimization.RealPointValuePair;
import lp.optimization.linear.*;

import java.util.ArrayList;
import java.util.Collection;

public class LinearProgramStub {
	public static void main ( String[] args ) throws OptimizationException
	{
		LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] {2, 3 }, 0);
		Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
		constraints.add(new LinearConstraint(new double[] { 0, 1 }, Relationship.LEQ, 6));
		constraints.add(new LinearConstraint(new double[] { 1, 0 }, Relationship.LEQ, 12));
		constraints.add(new LinearConstraint(new double[] { 0, 1 }, Relationship.GEQ, 0));
		constraints.add(new LinearConstraint(new double[] { 1, 0 }, Relationship.GEQ, 0));

		// create and run the solver
		RealPointValuePair solution = new SimplexSolver().optimize(f, constraints, GoalType.MAXIMIZE, false);

		// get the solution
		double x = solution.getPoint()[0];
		double y = solution.getPoint()[1];
		double min = solution.getValue();
		
		System.out.println("Value is " + min);
	}

}
