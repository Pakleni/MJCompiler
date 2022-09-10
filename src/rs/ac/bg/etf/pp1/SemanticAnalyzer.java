package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

public class SemanticAnalyzer extends VisitorAdaptor {

	// region "Program"
	public void visit(Program program) {
		nVars = MyTab.currentScope.getnVars();
		MyTab.chainLocalSymbols(program.getProgName().obj);
		MyTab.closeScope();
	}

	public void visit(ProgName progName) {
		progName.obj = MyTab.insert(Obj.Prog, progName.getValue(), MyTab.noType);
		MyTab.openScope();
	}
	// endregion

	// region "Type"
	Struct temporaryType = null;

	public void visit(Type type) {
		Obj typeNode = MyTab.find(type.getLabel());
		if (typeNode == MyTab.noObj) {
			report_error("Nije pronadjen tip " + type.getLabel() + " u tabeli simbola", null);
			type.struct = MyTab.noType;
		} else {
			if (Obj.Type == typeNode.getKind()) {
				type.struct = typeNode.getType();
			} else {
				report_error("Greska: Ime " + type.getLabel() + " ne predstavlja tip ", type);
				type.struct = MyTab.noType;
			}
		}

		temporaryType = type.struct;
	}
	// endregion

	// region "Method"
	Struct tempMethodType = null;

	public void visit(MethodDecl methodDecl) {
		if (!returnFound && currentMethod.getType() != MyTab.noType) {
			report_error("Semanticka greska na liniji " + methodDecl.getLine() + ": funcija " + currentMethod.getName()
					+ " nema return iskaz!", null);
		}

		MyTab.chainLocalSymbols(currentMethod);
		MyTab.closeScope();

		// currentMethod.setLevel(parCounter);

		returnFound = false;
		currentMethod = null;
		optCounter = 0;
		parCounter = 0;
	}

	public void visit(MethodTypeName methodTypeName) {
		String name = methodTypeName.getValue();
		currentMethod = MyTab.insert(Obj.Meth, name, tempMethodType);
		Method.globalna_lista.put(name, new Method());
		tempMethodType = null;
		methodTypeName.obj = currentMethod;
		MyTab.openScope();
		report_info("Obradjuje se funkcija " + methodTypeName.getValue(), methodTypeName);
	}

	public void visit(MethodDeclTypeType methodDeclTypeType) {
		tempMethodType = methodDeclTypeType.getType().struct;
	}

	public void visit(MethodDeclTypeVoid methodDeclTypeVoid) {
		tempMethodType = MyTab.noType;
	}
	// endregion

	// region "Variable Declaration"
	public void visit(ConstDeclListItem constDeclListItem) {

		ConstDeclItem constDeclItem = constDeclListItem.getConstDeclItem();
		int value;

		if (constDeclItem instanceof ConstDeclItemNumber) {
			value = ((ConstDeclItemNumber) constDeclItem).getValue();
			if (!temporaryType.equals(MyTab.intType)) {
				report_error("pogresan tip", constDeclListItem);
			}
		} else if (constDeclItem instanceof ConstDeclItemChar) {
			value = ((ConstDeclItemChar) constDeclItem).getValue();
			if (!temporaryType.equals(MyTab.charType)) {
				report_error("pogresan tip", constDeclListItem);
			}
		} else {
			value = ((ConstDeclItemBool) constDeclItem).getValue();
			if (!temporaryType.equals(MyTab.boolType)) {
				report_error("pogresan tip", constDeclListItem);
			}
		}

		constDeclListItem.obj = MyTab.insert(Obj.Con, constDeclListItem.getLabel(), temporaryType);
		constDeclListItem.obj.setAdr(value);
	}

	public void visit(VarDeclItemItem varDeclItemItem) {
		varDeclItemItem.obj = MyTab.insert(Obj.Var, varDeclItemItem.getLabel(), temporaryType);

		int level = currentMethod == null ? 0 : 1;

		varDeclItemItem.obj.setLevel(level);
	}

	public void visit(VarDeclItemArray varDeclItemArray) {
		varDeclItemArray.obj = MyTab.insert(Obj.Var, varDeclItemArray.getLabel(),
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
			if (!temporaryType.equals(MyTab.intType)) {
				report_error("pogresan tip", optArgs);
			}
		} else if (optArgsItem instanceof OptArgItemChar) {
			value = ((OptArgItemChar) optArgsItem).getValue();
			if (!temporaryType.equals(MyTab.intType)) {
				report_error("pogresan tip", optArgs);
			}
		} else {
			value = ((OptArgItemBool) optArgsItem).getValue();
			if (!temporaryType.equals(MyTab.intType)) {
				report_error("pogresan tip", optArgs);
			}
		}

		optArgs.obj = MyTab.insert(Obj.Var, optArgs.getLabel(), temporaryType);
		Method.globalna_lista.get(currentMethod.getName()).addParam(value);
		optArgs.obj.setFpPos(optCounter);
	}

	public void visit(FormParItem formParItem) {
		parCounter++;
		formParItem.obj = MyTab.insert(Obj.Var, formParItem.getLabel(), temporaryType);
		formParItem.obj.setFpPos(parCounter);
		Method.globalna_lista.get(currentMethod.getName()).addParam(null);
	}

	public void visit(FormParArray formParArray) {
		parCounter++;
		formParArray.obj = MyTab.insert(Obj.Var, formParArray.getLabel(),
				new Struct(Struct.Array, temporaryType));
		formParArray.obj.setFpPos(parCounter);
		Method.globalna_lista.get(currentMethod.getName()).addParam(null);
	}
	// endregion

	// region "Expr"
	public void visit(FactorNumber factorNumber) {
		factorNumber.struct = MyTab.intType;
	}

	public void visit(FactorChar factorChar) {
		factorChar.struct = MyTab.charType;
	}

	public void visit(FactorBool factorBool) {
		factorBool.struct = MyTab.boolType;
	}

	public void visit(FactorNewArr factorNewArr) {
		Struct type = factorNewArr.getType().struct;
		factorNewArr.struct = new Struct(Struct.Array, type);
		if (!MyTab.intType.equals(factorNewArr.getExpr().struct)) {
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
			report_error("Designator mora biti ime funkcije", factorDesignatorFun);
			return;
		}

		if (designator.obj.getKind() != Obj.Meth) {
			report_error("Designator mora biti ime funkcije", factorDesignatorFun);
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

		Struct type1 = termMultiple.struct;
		Struct type2 = termMultiple.getFactor().struct;

		if (!type1.equals(MyTab.intType) || !type2.equals(MyTab.intType)) {
			report_error("Term i Factor moraju biti tipa int", termMultiple);
		}
	}

	public void visit(ExprPos exprPos) {
		exprPos.struct = exprPos.getTerm().struct;
	}

	public void visit(ExprNeg exprNeg) {
		exprNeg.struct = exprNeg.getTerm().struct;

		Struct type = exprNeg.struct;

		if (!type.equals(MyTab.intType)) {
			report_error("Expr mora biti tipa int", exprNeg);
		}
	}

	public void visit(ExprMultiple exprMultiple) {
		exprMultiple.struct = exprMultiple.getExpr().struct;

		Struct type1 = exprMultiple.struct;
		Struct type2 = exprMultiple.getTerm().struct;

		if (!type1.equals(MyTab.intType) || !type2.equals(MyTab.intType)) {
			report_error("Expr i Term moraju biti tipa int", exprMultiple);
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

		if (!MyTab.intType.equals(designatorIdent.getExpr().struct)) {
			report_error("Velicina alokacije mora biti int tip", designatorIdent);
		}

		Struct elemType = type.getElemType();

		designatorIdent.obj = new Obj(Obj.Elem, designator.obj.getName(), elemType);
	}

	public void visit(DesignatorSingle designatorSingle) {
		String label = designatorSingle.getLabel();
		Obj obj = MyTab.find(label);
		designatorSingle.obj = obj;

		if (MyTab.noObj.equals(obj)) {
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

	// region "Statement"
	boolean insideDoWhile = false;

	public void visit(PrintStatement printStatement) {
		Struct type = printStatement.getExpr().struct;

		if (!(type.equals(MyTab.intType) || type.equals(MyTab.charType) || type.equals(MyTab.boolType))) {
			report_error("Expr mora biti tipa int, char ili bool", printStatement);
		}
	}

	public void visit(PrintStatementValue printStatementValue) {
		Struct type = printStatementValue.getExpr().struct;

		if (!(type.equals(MyTab.intType) || type.equals(MyTab.charType) || type.equals(MyTab.boolType))) {
			report_error("Expr mora biti tipa int, char ili bool", printStatementValue);
		}
	}

	public void visit(DesignatorPartInc designatorPartInc) {

		int kind = designatorPartInc.getDesignator().obj.getKind();
		Struct type = designatorPartInc.getDesignator().obj.getType();

		if (kind != Obj.Elem && kind != Obj.Var) {
			report_error("Designator mora oznacavati promenljivu, element niza", designatorPartInc);
			return;
		}

		if (!type.equals(MyTab.intType)) {
			report_error("Designator mora biti tipa int", designatorPartInc);
			return;
		}
	}

	public void visit(DesignatorPartDec designatorPartDec) {

		int kind = designatorPartDec.getDesignator().obj.getKind();
		Struct type = designatorPartDec.getDesignator().obj.getType();

		if (kind != Obj.Elem && kind != Obj.Var) {
			report_error("Designator mora oznacavati promenljivu, element niza", designatorPartDec);
			return;
		}

		if (!type.equals(MyTab.intType)) {
			report_error("Designator mora biti tipa int", designatorPartDec);
			return;
		}

	}

	public void visit(DesignatorPartAssign designatorPartAssign) {
		int kind = designatorPartAssign.getDesignator().obj.getKind();

		DesignatorPartAssignPart dp = designatorPartAssign.getDesignatorPartAssignPart();

		if (dp instanceof DesignatorPartAssignPartError) {
			report_error("Oporavak", designatorPartAssign);
			return;
		}
		Struct type1 = designatorPartAssign.getDesignator().obj.getType();
		Struct type2 = ((DesignatorPartAssignPartExpr) dp).getExpr().struct;

		if (kind != Obj.Elem && kind != Obj.Var) {
			report_error("Designator mora oznacavati promenljivu, element niza", designatorPartAssign);
			return;
		}

		if (!type2.assignableTo(type1)) {
			report_error("Tip neterminala Expr mora biti kompatibilan pri dodeli sa tipom neterminala Designator",
					designatorPartAssign);
			return;
		}
	}

	public void visit(DesignatorPartFun designatorPartFun) {
		Designator designator = designatorPartFun.getDesignator();

		int kind = designator.obj.getKind();

		if (kind != Obj.Meth) {
			report_error("Designator mora oznacavati promenljivu, element niza", designatorPartFun);
			return;
		}

		String label = designator.obj.getName();

		checkParams(label, designatorPartFun);
	}

	public void visit(ReadStatement readStatement) {
		int kind = readStatement.getDesignator().obj.getKind();
		Struct type = readStatement.getDesignator().obj.getType();

		if (kind != Obj.Elem && kind != Obj.Var) {
			report_error("Designator mora oznacavati promenljivu, element niza", readStatement);
			return;
		}

		if (!(type.equals(MyTab.intType) || type.equals(MyTab.charType) || type.equals(MyTab.boolType))) {
			report_error("Designator mora biti tipa int, char ili bool", readStatement);
			return;
		}
	}

	public void visit(ReturnStatement returnStatement) {
		if (currentMethod == null) {
			report_error("Ne sme postojati izvan tela (statickih) metoda, odnosno globalnih funkcija", returnStatement);
		}

		returnFound = true;
		Struct currMethType = currentMethod.getType();
		if (!currMethType.compatibleWith(MyTab.noType)) {
			report_error("Greska na liniji " + returnStatement.getLine() + " : "
					+ "tip izraza u return naredbi ne slaze se sa tipom povratne vrednosti funkcije "
					+ currentMethod.getName(), null);
		}
	}

	public void visit(ReturnStatementValue returnStatementValue) {
		if (currentMethod == null) {
			report_error("Ne sme postojati izvan tela (statickih) metoda, odnosno globalnih funkcija",
					returnStatementValue);
		}

		returnFound = true;
		Struct currMethType = currentMethod.getType();
		if (!currMethType.compatibleWith(returnStatementValue.getExpr().struct)) {
			report_error("Greska na liniji " + returnStatementValue.getLine() + " : "
					+ "tip izraza u return naredbi ne slaze se sa tipom povratne vrednosti funkcije "
					+ currentMethod.getName(), null);
		}
	}

	public void visit(ContinueStatement continueStatement) {
		if (!insideDoWhile) {
			report_error("Iskaz continue se moze koristiti samo unutar do-while petlje.", continueStatement);
		}
	}

	public void visit(BreakStatement breakStatement) {
		if (!insideDoWhile) {
			report_error("Iskaz break se moze koristiti samo unutar do-while petlje.", breakStatement);
		}
	}

	public void visit(DoWhileStatementStart doWhileStatementStart) {
		insideDoWhile = true;
	}

	public void visit(DoWhileStatement doWhileStatement) {
		insideDoWhile = false;

		if (!doWhileStatement.getCondition().struct.equals(MyTab.boolType)) {
			report_error("Uslovni izraz Condition mora biti tipa bool", doWhileStatement);
		}
	}

	public void visit(IfStatement ifStatement) {
		if (!ifStatement.getIfCondition().struct.equals(MyTab.boolType)) {
			report_error("Uslovni izraz Condition mora biti tipa bool", ifStatement);
		}
	}

	public void visit(IfElseStatement ifElseStatement) {
		if (!ifElseStatement.getIfCondition().struct.equals(MyTab.boolType)) {
			report_error("Uslovni izraz Condition mora biti tipa bool", ifElseStatement);
		}
	}
	// endregion

	// region "Conditions"
	public void visit(IfConditionActual ifConditionActual) {
		ifConditionActual.struct = ifConditionActual.getCondition().struct;
	}

	public void visit(ConditionSingle conditionSingle) {
		CondTerm condTerm = conditionSingle.getCondTerm();

		if (!condTerm.struct.equals(MyTab.boolType)) {
			report_error("CondTerm mora biti tipa boolean", conditionSingle);
			return;
		}

		conditionSingle.struct = MyTab.boolType;
	}

	public void visit(ConditionMultiple conditionMultiple) {
		CondTerm condTerm = conditionMultiple.getCondTerm();
		Condition cond = conditionMultiple.getCondition();

		if (!condTerm.struct.equals(MyTab.boolType) || !cond.struct.equals(MyTab.boolType)) {
			report_error("Condition i CondTerm moraju biti tipa boolean", conditionMultiple);
			return;
		}

		conditionMultiple.struct = MyTab.boolType;
	}

	public void visit(CondTermSingle condTermSingle) {
		CondFact condFact = condTermSingle.getCondFact();

		if (!condFact.struct.equals(MyTab.boolType)) {
			report_error("CondFact mora biti tipa boolean", condTermSingle);
			return;
		}

		condTermSingle.struct = MyTab.boolType;
	}

	public void visit(CondTermMultiple condTermMultiple) {
		CondFact condFact = condTermMultiple.getCondFact();
		CondTerm condTerm = condTermMultiple.getCondTerm();

		if (!condFact.struct.equals(MyTab.boolType) || !condTerm.struct.equals(MyTab.boolType)) {
			report_error("CondTerm i CondFact moraju biti tipa boolean", condTermMultiple);
			return;
		}

		condTermMultiple.struct = MyTab.boolType;
	}

	public void visit(CondFactSingle condFactSingle) {
		Expr expr = condFactSingle.getExpr();

		if (!expr.struct.equals(MyTab.boolType)) {
			report_error("Expr mora biti tipa boolean", expr);
			return;
		}

		condFactSingle.struct = MyTab.boolType;
	}

	public void visit(CondFactTwo condFactTwo) {
		Expr expr1 = condFactTwo.getExpr();
		Expr expr2 = condFactTwo.getExpr1();

		if (!expr1.struct.compatibleWith(expr2.struct)) {
			report_error("Oba expr-a moraju biti kompatibilni", condFactTwo);
			return;
		}

		condFactTwo.struct = MyTab.boolType;
	}
	// endregion

	// region "Helpers"
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

	public void checkParams(String label, SyntaxNode syntaxNode) {
		Obj obj = MyTab.find(label);

		if (MyTab.noObj.equals(obj) || obj.getKind() != Obj.Meth) {
			report_error("Nije pravilno ime metode", syntaxNode);
			return;
		}

		List<Struct> params = designatorParamStack.pop();

		if (label.equals("len")) {
			if (params.size() != 1) {
				report_error("len mora imati tacno 1 parametar", syntaxNode);
				return;
			}

			if (params.get(0).getKind() != Struct.Array) {
				report_error("len mora imati parametar tipa niz", syntaxNode);
			}
			return;
		}
		if (label.equals("ord")) {
			if (params.size() != 1) {
				report_error("ord mora imati tacno 1 parametar", syntaxNode);
				return;
			}

			if (!params.get(0).equals(MyTab.charType)) {
				report_error("ord mora imati parametar tipa char", syntaxNode);
			}
			return;
		}
		if (label.equals("chr")) {
			if (params.size() != 1) {
				report_error("chr mora imati tacno 1 parametar", syntaxNode);
				return;
			}

			if (!params.get(0).equals(MyTab.intType)) {
				report_error("chr mora imati parametar tipa int", syntaxNode);
			}
			return;
		}

		Method meth = Method.globalna_lista.get(label);

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

	public boolean passed() {
		return !errorDetected;
	}
	// endregion
}
