package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.ArrayList;
import java.util.HashMap;

public class Checker {

    private IHANStack<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        variableTypes = new HANStack<>();
        variableTypes.push(new HashMap<>());
        checkStylesheet(ast.root);
    }

    // Main entry point for checking the AST
    private void checkStylesheet(Stylesheet sheet) {
        for (ASTNode child : sheet.body) {
            if (child instanceof VariableAssignment) {
                checkVariableAssignment((VariableAssignment) child);
            } else if (child instanceof Stylerule) {
                checkStylerule((Stylerule) child);
            }
        }
    }

    private void checkStylerule(Stylerule rule) {
        // Create new scope block
        variableTypes.push(new HashMap<>());
        checkBody(rule.body);
        variableTypes.pop();
    }

    private void checkBody(ArrayList<ASTNode> body) {
        for (ASTNode node : body) {
            if (node instanceof VariableAssignment) {
                checkVariableAssignment((VariableAssignment) node);
            } else if (node instanceof Declaration) {
                checkDeclaration((Declaration) node);
            } else if (node instanceof IfClause) {
                checkIfClause((IfClause) node);
            }
        }
    }

    private void checkVariableAssignment(VariableAssignment va) {
        ExpressionType type = getType(va.expression);
        variableTypes.peek().put(va.name.name, type);
    }

    // Validate css property values
    private void checkDeclaration(Declaration decl) {
        ExpressionType type = getType(decl.expression);
        String propertyName = decl.property.name;

        switch (propertyName) {
            case "color":
            case "background-color":
                if (type != ExpressionType.COLOR) {
                    decl.setError("Property '" + propertyName + "' requires a color value.");
                }
                break;
            case "width":
            case "height":
                if (type != ExpressionType.PIXEL && type != ExpressionType.PERCENTAGE) {
                    decl.setError("Property '" + propertyName + "' requires a pixel or percentage value.");
                }
                break;
            default:
                decl.setError("Unknown property: " + propertyName);
        }
    }

    private void checkIfClause(IfClause ifClause) {
        ExpressionType condType = getType(ifClause.conditionalExpression);
        if (condType != ExpressionType.BOOL) {
            ifClause.setError("If-clause condition must be a boolean.");
        }

        variableTypes.push(new HashMap<>());
        checkBody(ifClause.body);
        variableTypes.pop();

        if (ifClause.elseClause != null) {
            variableTypes.push(new HashMap<>());
            checkBody(ifClause.elseClause.body);
            variableTypes.pop();
        }
    }

    // Find the type of expressions and expressions elements
    private ExpressionType getType(Expression expr) {
        if (expr instanceof BoolLiteral)       return ExpressionType.BOOL;
        if (expr instanceof ColorLiteral)      return ExpressionType.COLOR;
        if (expr instanceof PixelLiteral)      return ExpressionType.PIXEL;
        if (expr instanceof PercentageLiteral) return ExpressionType.PERCENTAGE;
        if (expr instanceof ScalarLiteral)     return ExpressionType.SCALAR;

        if (expr instanceof VariableReference) {
            String varName = ((VariableReference) expr).name;
            ExpressionType type = lookupVariable(varName);
            if (type == null) {
                expr.setError("Variable '" + varName + "' is not defined.");
                return ExpressionType.UNDEFINED;
            }
            return type;
        }

        if (expr instanceof AddOperation)      return checkAddSub((Operation) expr);
        if (expr instanceof SubtractOperation) return checkAddSub((Operation) expr);
        if (expr instanceof MultiplyOperation) return checkMultiply((MultiplyOperation) expr);

        return ExpressionType.UNDEFINED;
    }

    // Logic for checking addition and subtraction
    private ExpressionType checkAddSub(Operation op) {
        ExpressionType left  = getType(op.lhs);
        ExpressionType right = getType(op.rhs);

        if (left == ExpressionType.COLOR || right == ExpressionType.COLOR) {
            op.setError("Colors cannot be used in arithmetic operations.");
            return ExpressionType.UNDEFINED;
        }
        if (left != right) {
            op.setError("Type mismatch in operation: " + left + " vs " + right);
            return ExpressionType.UNDEFINED;
        }
        return left;
    }

    // Logic for checking multiplication operations
    private ExpressionType checkMultiply(MultiplyOperation op) {
        ExpressionType left  = getType(op.lhs);
        ExpressionType right = getType(op.rhs);

        if (left == ExpressionType.COLOR || right == ExpressionType.COLOR) {
            op.setError("Colors cannot be used in multiplication.");
            return ExpressionType.UNDEFINED;
        }
        if (left != ExpressionType.SCALAR && right != ExpressionType.SCALAR) {
            op.setError("Multiplication requires at least one scalar operand.");
            return ExpressionType.UNDEFINED;
        }
        return (left == ExpressionType.SCALAR) ? right : left;
    }

    // Find variable across scopes
    private ExpressionType lookupVariable(String name) {
        IHANStack<HashMap<String, ExpressionType>> temp = new HANStack<>();
        ExpressionType found = null;

        while (!variableTypes.isEmpty()) {
            HashMap<String, ExpressionType> scope = variableTypes.pop();
            temp.push(scope);
            if (scope.containsKey(name)) {
                found = scope.get(name);
            }
        }

        while (!temp.isEmpty()) {
            variableTypes.push(temp.pop());
        }
        return found;
    }
}