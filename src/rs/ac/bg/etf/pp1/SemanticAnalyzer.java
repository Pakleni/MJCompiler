package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

public class SemanticAnalyzer extends VisitorAdaptor {

	public static Struct boolType = new Struct(Struct.Bool);

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

	// region "Type"
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

	// region "Method"
	Struct tempMethodType = null;

	public void visit(MethodDecl methodDecl) {
		if (!returnFound && currentMethod.getType() != Tab.noType) {
			report_error("Semanticka greska na liniji " + methodDecl.getLine() + ": funcija " + currentMethod.getName()
					+ " nema return iskaz!", null);
		}

		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();

		// currentMethod.setLevel(parCounter);

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

	// region "Variable Declaration"
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
			if (!temporaryType.equals(Tab.charType)) {
				report_error("pogresan tip", constDeclListItem);
			}
		} else {
			value = ((ConstDeclItemBool) constDeclItem).getValue();
			if (!temporaryType.equals(boolType)) {
				report_error("pogresan tip", constDeclListItem);
			}
		}

		constDeclListItem.obj = Tab.insert(Obj.Con, constDeclListItem.getLabel(), temporaryType);
		constDeclListItem.obj.setAdr(value);
	}

	public void visit(VarDeclItemItem varDeclItemItem) {
		varDeclItemItem.obj = Tab.insert(Obj.Var, varDeclItemItem.getLabel(), temporaryType);

		int level = currentMethod == null ? 0 : 1;

		varDeclItemItem.obj.setLevel(level);
	}

	public void visit(VarDeclItemArray varDeclItemArray) {
		varDeclItemArray.obj = Tab.insert(Obj.Var, varDeclItemArray.getLabel(),
				new Struct(Struct.Array, temporaryType));

		int level = currentMethod == null ? 0 : 1;

		varDeclItemArray.obj.setLevel(level);
	}
	// endregion

	// region "Method Parameters"
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

	// region "Expr"
	public void visit(FactorNumber factorNumber) {
		factorNumber.struct = Tab.intType;
	}

	public void visit(FactorChar factorChar) {
		factorChar.struct = Tab.charType;
	}

	public void visit(FactorBool factorBool) {
		factorBool.struct = boolType;
	}

	public void visit(FactorNewArr factorNewArr) {
		Struct type = factorNewArr.getType().struct;
		factorNewArr.struct = new Struct(Struct.Array, type);
		if (!Tab.intType.equals(factorNewArr.getExpr().struct)) {
			report_error("Velicina alokacije mora biti int tip", factorNewArr);
		}
	}

	public void visit(FactorExpr factorExpr) {
		factorExpr.struct = factorExpr.getExpr().struct;
	}

	public void visit(FactorDesignator factorDesignator) {
		factorDesignator.struct = factorDesignator.getDesignator().obj.getType();
	}

	public void visit(FactorDesignatorFun factorDesignatorFun) {
		Designator designator = factorDesignatorFun.getDesignator();

		factorDesignatorFun.struct = designator.obj.getType();

		if (designator instanceof DesignatorIdent) {
			report_error("Ne znam kako da pristup ovome a[3]()", factorDesignatorFun);
			return;
		}
		String label = designator.obj.getName();

		checkParams(label, factorDesignatorFun);
	}

	public void visit(TermSingle termSingle) {
		termSingle.struct = termSingle.getFactor().struct;
	}

	public void visit(TermMultiple termMultiple) {
		termMultiple.struct = termMultiple.getTerm().struct;

		if (!termMultiple.struct.equals(termMultiple.getFactor().struct)) {
			report_error("Ne poklapaju se tipovi sa leve i desne strane mulop-a", termMultiple);
		}
	}

	public void visit(ExprPos exprPos) {
		exprPos.struct = exprPos.getTerm().struct;
	}

	public void visit(ExprNeg exprNeg) {
		exprNeg.struct = exprNeg.getTerm().struct;
	}

	public void visit(ExprMultiple exprMultiple) {
		exprMultiple.struct = exprMultiple.getExpr().struct;

		if (!exprMultiple.struct.equals(exprMultiple.getTerm().struct)) {
			report_error("Ne poklapaju se tipovi sa leve i desne strane addop-a", exprMultiple);
		}
	}
	// endregion

	// region "Designator"
	public Stack<List<Struct>> designatorParamStack = new Stack<>();

	public void visit(DesignatorIdent designatorIdent) {
		Designator designator = designatorIdent.getDesignator();
		Struct type = designator.obj.getType();

		if (type.getKind() != Struct.Array) {
			report_error("Nije niz", designatorIdent);
		}

		if (!Tab.intType.equals(designatorIdent.getExpr().struct)) {
			report_error("Velicina alokacije mora biti int tip", designatorIdent);
		}

		Struct elemType = type.getElemType();

		designatorIdent.obj = new Obj(Obj.Elem, designator.obj.getName(), elemType);
	}

	public void visit(DesignatorSingle designatorSingle) {
		String label = designatorSingle.getLabel();
		Obj obj = Tab.find(label);
		designatorSingle.obj = obj;

		if (Tab.noObj.equals(obj)) {
			report_error("Nije definisana promenljiva sa tim imenom", designatorSingle);
			return;
		}
	}

	public void visit(FunDesignatorPartEmpty funDesignatorPartEmpty) {
		designatorParamStack.push(new ArrayList<>());
	}

	public void visit(ActParsSingle actParsSingle) {
		designatorParamStack.push(new ArrayList<>());
		designatorParamStack.peek().add(actParsSingle.getExpr().struct);
	}

	public void visit(ActParsMultiple actParsMultiple) {
		designatorParamStack.peek().add(actParsMultiple.getExpr().struct);
	}
	// endregion

	// TODO: statement

	// TODO: conditions

	// region "Helpers"
	public void checkParams(String label, SyntaxNode syntaxNode) {
		Obj obj = Tab.find(label);

		if (Tab.noObj.equals(obj) || obj.getKind() != Obj.Meth) {
			report_error("Nije pravilno ime metode", syntaxNode);
			return;
		}

		Method meth = Method.globalna_lista.get(label);
		List<Struct> params = designatorParamStack.pop();

		if (params.size() > meth.size() || params.size() < meth.for_param_size) {
			report_error("Nije dobar broj parametara", syntaxNode);
			return;
		}

		Collection<Obj> coll = obj.getLocalSymbols();

		for (Obj sym : coll) {
			int i;

			if (sym.getFpPos() > 0) {
				i = sym.getFpPos() - 1;
			} else if (sym.getFpPos() < 0) {
				i = meth.for_param_size - sym.getFpPos() - 1;

				if (i >= params.size()) {
					return;
				}
			} else {
				return;
			}

			if (!sym.getType().equals(params.get(i))) {
				report_error("Parametar " + i + " se ne poklapa sa definicjom funkcije", syntaxNode);
			}
		}
	}
	// endregion

	public boolean passed() {
		return !errorDetected;
	}

}
