package nl.han.ica.icss.generator;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.selectors.*;

public class Generator {

	public String generate(AST ast) {
		StringBuilder sb = new StringBuilder();
		for (ASTNode child : ast.root.body) {
			if (child instanceof Stylerule) {
				sb.append(generateStylerule((Stylerule) child));
			}
		}
		return sb.toString();
	}

	private String generateStylerule(Stylerule rule) {
		StringBuilder sb = new StringBuilder();

		for (Selector sel : rule.selectors) {
			sb.append(generateSelector(sel));
			break;
		}
		sb.append(" {\n");

		for (ASTNode node : rule.body) {
			if (node instanceof Declaration) {
				sb.append("  ");
				sb.append(generateDeclaration((Declaration) node));
				sb.append("\n");
			}
		}

		sb.append("}\n");
		return sb.toString();
	}

	private String generateSelector(Selector sel) {
		if (sel instanceof TagSelector)   return ((TagSelector) sel).tag;
		if (sel instanceof IdSelector)    return ((IdSelector) sel).id;
		if (sel instanceof ClassSelector) return ((ClassSelector) sel).cls;
		return "";
	}

	// GE01: property: value;
	private String generateDeclaration(Declaration decl) {
		return decl.property.name + ": " + literalToString(decl.expression) + ";";
	}

	private String literalToString(Expression expr) {
		if (expr instanceof PixelLiteral)      return ((PixelLiteral) expr).value + "px";
		if (expr instanceof PercentageLiteral) return ((PercentageLiteral) expr).value + "%";
		if (expr instanceof ColorLiteral)      return ((ColorLiteral) expr).value;
		if (expr instanceof ScalarLiteral)     return String.valueOf(((ScalarLiteral) expr).value);
		if (expr instanceof BoolLiteral)       return ((BoolLiteral) expr).value ? "TRUE" : "FALSE";
		return "";
	}
}