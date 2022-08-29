package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

	int printCallCount = 0;
	int varDeclCount = 0;
	Obj currentMethod = null;
	boolean returnFound = false;
	boolean errorDetected = false;
	int nVars;

	Logger log = Logger.getLogger(getClass());

	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		log.info(msg.toString());
	}

	public void visit(Program program) {
		nVars = Tab.currentScope.getnVars();
		Tab.chainLocalSymbols(program.getProgName().obj);
		Tab.closeScope();
	}

	public void visit(ProgName progName) {
		progName.obj = Tab.insert(Obj.Prog, progName.getValue(), Tab.noType);
		Tab.openScope();
	}

	// region "Type Visits"
	Struct temporaryType = null;

	public void visit(Type type) {
		Obj typeNode = Tab.find(type.getLabel());
		if (typeNode == Tab.noObj) {
			report_error("Nije pronadjen tip " + type.getLabel() + " u tabeli simbola", null);
			type.struct = Tab.noType;
		} else {
			if (Obj.Type == typeNode.getKind()) {
				type.struct = typeNode.getType();
			} else {
				report_error("Greska: Ime " + type.getLabel() + " ne predstavlja tip ", type);
				type.struct = Tab.noType;
			}
		}

		temporaryType = type.struct;
	}
	// endregion

	// region "Method Visits"
	Struct tempMethodType = null;

	public void visit(MethodDecl methodDecl) {
		if (!returnFound && currentMethod.getType() != Tab.noType) {
			report_error("Semanticka greska na liniji " + methodDecl.getLine() + ": funcija " + currentMethod.getName()
					+ " nema return iskaz!", null);
		}

		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();

		returnFound = false;
		currentMethod = null;
		optCounter = 0;
		parCounter = 0;
	}

	public void visit(MethodTypeName methodTypeName) {
		String name = methodTypeName.getValue();
		currentMethod = Tab.insert(Obj.Meth, name, tempMethodType);
		Method.globalna_lista.put(name, new Method());
		tempMethodType = null;
		methodTypeName.obj = currentMethod;
		Tab.openScope();
		report_info("Obradjuje se funkcija " + methodTypeName.getValue(), methodTypeName);
	}

	public void visit(MethodDeclTypeType methodDeclTypeType) {
		tempMethodType = methodDeclTypeType.getType().struct;
	}

	public void visit(MethodDeclTypeVoid methodDeclTypeVoid) {
		tempMethodType = Tab.noType;
	}

	public void visit(ReturnStatement returnStatement) {
		returnFound = true;
		Struct currMethType = currentMethod.getType();
		if (!currMethType.compatibleWith(Tab.noType)) {
			report_error("Greska na liniji " + returnStatement.getLine() + " : "
					+ "tip izraza u return naredbi ne slaze se sa tipom povratne vrednosti funkcije "
					+ currentMethod.getName(), null);
		}
	}

	public void visit(ReturnStatementValue returnStatementValue) {
		returnFound = true;
		Struct currMethType = currentMethod.getType();
		if (!currMethType.compatibleWith(returnStatementValue.getExpr().struct)) {
			report_error("Greska na liniji " + returnStatementValue.getLine() + " : "
					+ "tip izraza u return naredbi ne slaze se sa tipom povratne vrednosti funkcije "
					+ currentMethod.getName(), null);
		}
	}
	// endregion

	// region "Variable Visits"
	public void visit(ConstDeclListItem constDeclListItem) {

		ConstDeclItem constDeclItem = constDeclListItem.getConstDeclItem();
		int value;

		if (constDeclItem instanceof ConstDeclItemNumber) {
			value = ((ConstDeclItemNumber) constDeclItem).getValue();
			if (!temporaryType.equals(Tab.intType)) {
				report_error("pogresan tip", constDeclListItem);
			}
		} else if (constDeclItem instanceof ConstDeclItemChar) {
			value = ((ConstDeclItemChar) constDeclItem).getValue();
			if (!temporaryType.equals(Tab.intType)) {
				report_error("pogresan tip", constDeclListItem);
			}
		} else {
			value = ((ConstDeclItemBool) constDeclItem).getValue();
			if (!temporaryType.equals(Tab.intType)) {
				report_error("pogresan tip", constDeclListItem);
			}
		}

		constDeclListItem.obj = Tab.insert(Obj.Con, constDeclListItem.getLabel(), temporaryType);
		constDeclListItem.obj.setAdr(value);
	}

	public void visit(VarDeclItemItem varDeclItemItem) {
		varDeclItemItem.obj = Tab.insert(Obj.Var, varDeclItemItem.getLabel(), temporaryType);
	}

	public void visit(VarDeclItemArray varDeclItemArray) {
		varDeclItemArray.obj = Tab.insert(Obj.Var, varDeclItemArray.getLabel(),
				new Struct(Struct.Array, temporaryType));
	}
	// endregion

	// region "Parameter Visits"
	int optCounter = 0;
	int parCounter = 0;

	public void visit(OptArgs optArgs) {
		optCounter--;

		OptArgsItem optArgsItem = optArgs.getOptArgsItem();
		int value;

		if (optArgsItem instanceof OptArgItemNumber) {
			value = ((OptArgItemNumber) optArgsItem).getValue();
			if (!temporaryType.equals(Tab.intType)) {
				report_error("pogresan tip", optArgs);
			}
		} else if (optArgsItem instanceof OptArgItemChar) {
			value = ((OptArgItemChar) optArgsItem).getValue();
			if (!temporaryType.equals(Tab.intType)) {
				report_error("pogresan tip", optArgs);
			}
		} else {
			value = ((OptArgItemBool) optArgsItem).getValue();
			if (!temporaryType.equals(Tab.intType)) {
				report_error("pogresan tip", optArgs);
			}
		}

		optArgs.obj = Tab.insert(Obj.Var, optArgs.getLabel(), temporaryType);
		Method.globalna_lista.get(currentMethod.getName()).addParam(value);
		optArgs.obj.setFpPos(optCounter);
	}

	public void visit(FormParItem formParItem) {
		parCounter++;
		formParItem.obj = Tab.insert(Obj.Var, formParItem.getLabel(), temporaryType);
		formParItem.obj.setFpPos(parCounter);
		Method.globalna_lista.get(currentMethod.getName()).addParam(null);
	}

	public void visit(FormParArray formParArray) {
		parCounter++;
		formParArray.obj = Tab.insert(Obj.Var, formParArray.getLabel(),
				new Struct(Struct.Array, temporaryType));
		formParArray.obj.setFpPos(parCounter);
		Method.globalna_lista.get(currentMethod.getName()).addParam(null);
	}
	// endregion

	// TODO: expr

	// TODO: statement

	// TODO: conditions

	public boolean passed() {
		return !errorDetected;
	}

}
