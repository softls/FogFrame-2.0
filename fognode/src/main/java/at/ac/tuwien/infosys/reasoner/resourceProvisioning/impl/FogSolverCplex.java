package at.ac.tuwien.infosys.reasoner.resourceProvisioning.impl;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import net.sf.javailp.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by olena on 11/22/17.
 */
public class FogSolverCplex extends SolverCPLEX {
    /*
* (non-Javadoc)
*
* @see net.sf.javailp.Solver#solve(net.sf.javailp.Problem)
*/
    @Override
    public Result solve(Problem problem) {
        Map<IloNumVar, Object> numToVar = new HashMap<IloNumVar, Object>();
        Map<Object, IloNumVar> varToNum = new HashMap<Object, IloNumVar>();

        try {
            IloCplex cplex = new IloCplex();

            initWithParameters(cplex);

            for (Object variable : problem.getVariables()) {
                VarType varType = problem.getVarType(variable);
                Number lowerBound = problem.getVarLowerBound(variable);
                Number upperBound = problem.getVarUpperBound(variable);

                double lb = (lowerBound != null ? lowerBound.doubleValue() : Double.NEGATIVE_INFINITY);
                double ub = (upperBound != null ? upperBound.doubleValue() : Double.POSITIVE_INFINITY);

                final IloNumVarType type;
                switch (varType) {
                    case BOOL:
                        type = IloNumVarType.Bool;
                        break;
                    case INT:
                        type = IloNumVarType.Int;
                        break;
                    default: // REAL
                        type = IloNumVarType.Float;
                        break;
                }

                IloNumVar num = cplex.numVar(lb, ub, type);

                numToVar.put(num, variable);
                varToNum.put(variable, num);
            }

            for (Constraint constraint : problem.getConstraints()) {
                IloLinearNumExpr lin = cplex.linearNumExpr();
                Linear linear = constraint.getLhs();
                convert(linear, lin, varToNum);

                double rhs = constraint.getRhs().doubleValue();

                switch (constraint.getOperator()) {
                    case LE:
                        cplex.addLe(lin, rhs);
                        break;
                    case GE:
                        cplex.addGe(lin, rhs);
                        break;
                    default: // EQ
                        cplex.addEq(lin, rhs);
                }
            }

            if (problem.getObjective() != null) {
                IloLinearNumExpr lin = cplex.linearNumExpr();
                Linear objective = problem.getObjective();
                convert(objective, lin, varToNum);

                if (problem.getOptType() == OptType.MIN) {
                    cplex.addMinimize(lin);
                } else {
                    cplex.addMaximize(lin);
                }
            }

            for (Hook hook : hooks) {
                hook.call(cplex, varToNum);
            }
            cplex.setParam(IloCplex.BooleanParam.PreInd, false);
            if (!cplex.solve()) {

                cplex.end();
                return null;
            }

            final Result result;
            if (problem.getObjective() != null) {
                Linear objective = problem.getObjective();
                result = new ResultImpl(objective);
            } else {
                result = new ResultImpl();
            }

            for (Map.Entry<Object, IloNumVar> entry : varToNum.entrySet()) {
                Object variable = entry.getKey();
                IloNumVar num = entry.getValue();
                VarType varType = problem.getVarType(variable);

                double value = cplex.getValue(num);
                if (varType.isInt()) {
                    int v = (int) Math.round(value);
                    result.putPrimalValue(variable, v);
                } else {
                    result.putPrimalValue(variable, value);
                }
            }

            cplex.end();

            return result;

        } catch (IloException e) {
            e.printStackTrace();
        }

        return null;
    }
}
