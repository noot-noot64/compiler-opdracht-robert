package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Evaluator implements Transform {

    private IHANStack<HashMap<String, Literal>> variableValues;

    @Override
    public void apply(AST ast) {
        variableValues = new HANStack<>();
        variableValues.push(new HashMap<>());
        applyStylesheet(ast.root);
    }

    private void applyStylesheet(Stylesheet sheet) {
        ArrayList<ASTNode> toRemove = new ArrayList<>();
        for (ASTNode child : sheet.body) {
            if (child instanceof VariableAssignment) {
                evaluateVariableAssignment((VariableAssignment) child);
                toRemove.add(child);
            } else if (child instanceof Stylerule) {
                applyStylerule((Stylerule) child);
            }
        }
        sheet.body.removeAll(toRemove);
    }

    private void applyStylerule(Stylerule rule) {
        variableValues.push(new HashMap<>());
        rule.body = applyBody(rule.body);
        variableValues.pop();
    }

    private ArrayList<ASTNode> applyBody(ArrayList<ASTNode> body) {
        ArrayList<ASTNode> result = new ArrayList<>();
        for (ASTNode node : body) {
            if (node instanceof VariableAssignment) {
                evaluateVariableAssignment((VariableAssignment) node);
                // Consumed – not kept in the output tree
            } else if (node instanceof Declaration) {
                evaluateDeclaration((Declaration) node);
                result.add(node);
            } else if (node instanceof IfClause) {
                // TR02: replace the if-clause with the winning branch's contents
                result.addAll(evaluateIfClause((IfClause) node));
            } else {
                result.add(node);
            }
        }
        return result;
    }

    private void evaluateVariableAssignment(VariableAssignment va) {
        Literal value = evaluateExpression(va.expression);
        variableValues.peek().put(va.name.name, value);
    }

    private void evaluateDeclaration(Declaration decl) {
        decl.expression = evaluateExpression(decl.expression);
    }

    private ArrayList<ASTNode> evaluateIfClause(IfClause ifClause) {
        BoolLiteral condition = (BoolLiteral) evaluateExpression(ifClause.conditionalExpression);

        variableValues.push(new HashMap<>());
        ArrayList<ASTNode> result;
        if (condition.value) {
            result = applyBody(new ArrayList<>(ifClause.body));
        } else if (ifClause.elseClause != null) {
            result = applyBody(new ArrayList<>(ifClause.elseClause.body));
        } else {
            result = new ArrayList<>();
        }
        variableValues.pop();
        return result;
    }

    private Literal evaluateExpression(Expression expr) {
        if (expr instanceof Literal) {
            return (Literal) expr;
        }

        if (expr instanceof VariableReference) {
            return lookupVariable(((VariableReference) expr).name);
        }

        if (expr instanceof AddOperation) {
            Literal left  = evaluateExpression(((AddOperation) expr).lhs);
            Literal right = evaluateExpression(((AddOperation) expr).rhs);
            return add(left, right);
        }

        if (expr instanceof SubtractOperation) {
            Literal left  = evaluateExpression(((SubtractOperation) expr).lhs);
            Literal right = evaluateExpression(((SubtractOperation) expr).rhs);
            return subtract(left, right);
        }

        if (expr instanceof MultiplyOperation) {
            Literal left  = evaluateExpression(((MultiplyOperation) expr).lhs);
            Literal right = evaluateExpression(((MultiplyOperation) expr).rhs);
            return multiply(left, right);
        }

        throw new RuntimeException("Unknown expression: " + expr.getClass().getSimpleName());
    }

    private Literal lookupVariable(String name) {
        IHANStack<HashMap<String, Literal>> temp = new HANStack<>();
        Literal found = null;

        while (!variableValues.isEmpty()) {
            HashMap<String, Literal> scope = variableValues.pop();
            temp.push(scope);
            if (scope.containsKey(name)) {
                found = scope.get(name);
            }
        }
        while (!temp.isEmpty()) {
            variableValues.push(temp.pop());
        }
        if (found == null) throw new RuntimeException("Undefined variable: " + name);
        return found;
    }

    private Literal add(Literal left, Literal right) {
        if (left instanceof PixelLiteral)
            return new PixelLiteral(((PixelLiteral) left).value + ((PixelLiteral) right).value);
        if (left instanceof PercentageLiteral)
            return new PercentageLiteral(((PercentageLiteral) left).value + ((PercentageLiteral) right).value);
        if (left instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) left).value + ((ScalarLiteral) right).value);
        throw new RuntimeException("Cannot add: " + left.getClass().getSimpleName());
    }

    private Literal subtract(Literal left, Literal right) {
        if (left instanceof PixelLiteral)
            return new PixelLiteral(((PixelLiteral) left).value - ((PixelLiteral) right).value);
        if (left instanceof PercentageLiteral)
            return new PercentageLiteral(((PercentageLiteral) left).value - ((PercentageLiteral) right).value);
        if (left instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) left).value - ((ScalarLiteral) right).value);
        throw new RuntimeException("Cannot subtract: " + left.getClass().getSimpleName());
    }

    private Literal multiply(Literal left, Literal right) {
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) left).value * ((ScalarLiteral) right).value);

        if (left instanceof ScalarLiteral) {
            int s = ((ScalarLiteral) left).value;
            if (right instanceof PixelLiteral)      return new PixelLiteral(s * ((PixelLiteral) right).value);
            if (right instanceof PercentageLiteral) return new PercentageLiteral(s * ((PercentageLiteral) right).value);
        }
        if (right instanceof ScalarLiteral) {
            int s = ((ScalarLiteral) right).value;
            if (left instanceof PixelLiteral)       return new PixelLiteral(s * ((PixelLiteral) left).value);
            if (left instanceof PercentageLiteral)  return new PercentageLiteral(s * ((PercentageLiteral) left).value);
        }
        throw new RuntimeException("Cannot multiply: " + left.getClass().getSimpleName());
    }
}