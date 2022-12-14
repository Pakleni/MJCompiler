package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import rs.etf.pp1.symboltable.visitors.DumpSymbolTableVisitor;

public class SemanticAnalyzer extends VisitorAdaptor {

	// region "Program"
	public void visit(Program program) {
		nVars = MyTab.currentScope.getnVars();
		MyTab.chainLocalSymbols(program.getProgName().obj);
		MyTab.closeScope();
		higherLevel();
		if (!validMainFound) {
			report_error("No valid main found", null);
		}
	}

	public void visit(ProgName progName) {
		progName.obj = MyTab.insert(Obj.Prog, progName.getValue(), MyTab.noType);
		MyTab.openScope();
		deeperLevel();
		insideDoWhile.add(false);
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
			return;
		}

		MyTab.chainLocalSymbols(currentMethod);
		MyTab.closeScope();
		higherLevel();

		if ("main".equalsIgnoreCase(currentMethod.getName()) && MyTab.noType.equals(currentMethod.getType())
				&& parCounter == 0
				&& optCounter == 0) {
			validMainFound = true;
		}
		returnFound = false;
		currentMethod = null;
		optCounter = 0;
		parCounter = 0;
	}

	public void visit(MethodTypeName methodTypeName) {
		String name = methodTypeName.getValue();
		addParamToLevel(name, methodTypeName);
		currentMethod = MyTab.insert(Obj.Meth, name, tempMethodType);
		Method.globalna_lista.put(name, new Method());
		tempMethodType = null;
		methodTypeName.obj = currentMethod;
		MyTab.openScope();
		deeperLevel();
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

		addParamToLevel(constDeclListItem.getLabel(), constDeclListItem);
		if (constDeclItem instanceof ConstDeclItemNumber) {
			value = ((ConstDeclItemNumber) constDeclItem).getValue();
			if (!temporaryType.equals(MyTab.intType)) {
				report_error("pogresan tip", constDeclListItem);
				return;
			}
		} else if (constDeclItem instanceof ConstDeclItemChar) {
			value = ((ConstDeclItemChar) constDeclItem).getValue();
			if (!temporaryType.equals(MyTab.charType)) {
				report_error("pogresan tip", constDeclListItem);
				return;
			}
		} else {
			value = ((ConstDeclItemBool) constDeclItem).getValue();
			if (!temporaryType.equals(MyTab.boolType)) {
				report_error("pogresan tip", constDeclListItem);
				return;
			}
		}

		constDeclListItem.obj = MyTab.insert(Obj.Con, constDeclListItem.getLabel(), temporaryType);
		constDeclListItem.obj.setAdr(value);
	}

	public void visit(VarDeclItemItem varDeclItemItem) {
		varDeclItemItem.obj = MyTab.insert(Obj.Var, varDeclItemItem.getLabel(), temporaryType);
		addParamToLevel(varDeclItemItem.getLabel(), varDeclItemItem);
	}

	public void visit(VarDeclItemArray varDeclItemArray) {
		varDeclItemArray.obj = MyTab.insert(Obj.Var, varDeclItemArray.getLabel(),
				new Struct(Struct.Array, temporaryType));
		addParamToLevel(varDeclItemArray.getLabel(), varDeclItemArray);
	}
	// endregion

	// region "Method Parameters"
	int optCounter = 0;
	int parCounter = 0;

	public void visit(OptArgs optArgs) {
		optCounter--;

		OptArgsItem optArgsItem = optArgs.getOptArgsItem();
		int value;

		addParamToLevel(optArgs.getLabel(), optArgs);
		if (optArgsItem instanceof OptArgItemNumber) {
			value = ((OptArgItemNumber) optArgsItem).getValue();
			if (!temporaryType.equals(MyTab.intType)) {
				report_error("pogresan tip", optArgs);
				return;
			}
		} else if (optArgsItem instanceof OptArgItemChar) {
			value = ((OptArgItemChar) optArgsItem).getValue();
			if (!temporaryType.equals(MyTab.intType)) {
				report_error("pogresan tip", optArgs);
				return;
			}
		} else {
			value = ((OptArgItemBool) optArgsItem).getValue();
			if (!temporaryType.equals(MyTab.intType)) {
				report_error("pogresan tip", optArgs);
				return;
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
		addParamToLevel(formParItem.getLabel(), formParItem);
	}

	public void visit(FormParArray formParArray) {
		parCounter++;
		formParArray.obj = MyTab.insert(Obj.Var, formParArray.getLabel(),
				new Struct(Struct.Array, temporaryType));
		formParArray.obj.setFpPos(parCounter);
		Method.globalna_lista.get(currentMethod.getName()).addParam(null);
		addParamToLevel(formParArray.getLabel(), formParArray);
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
			return;
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

		detect_usage("Upotreba funkcije kao faktor", designator.obj.getName(), factorDesignatorFun);

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
			return;
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
			return;
		}
	}

	public void visit(ExprMultiple exprMultiple) {
		exprMultiple.struct = exprMultiple.getExpr().struct;

		Struct type1 = exprMultiple.struct;
		Struct type2 = exprMultiple.getTerm().struct;

		if (!type1.equals(MyTab.intType) || !type2.equals(MyTab.intType)) {
			report_error("Expr i Term moraju biti tipa int", exprMultiple);
			return;
		}

	}
	// endregion

	// region "Designator"
	public Stack<List<Struct>> designatorParamStack = new Stack<>();

	public void visit(DesignatorIdent designatorIdent) {
		Designator designator = designatorIdent.getDesignator();
		Struct type = designator.obj.getType();

		detect_usage("pristup elementu niza", designator.obj.getName(), designatorIdent);

		if (type.getKind() != Struct.Array) {
			report_error("Nije niz", designatorIdent);
			return;
		}

		if (!MyTab.intType.equals(designatorIdent.getExpr().struct)) {
			report_error("Velicina alokacije mora biti int tip", designatorIdent);
			return;
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

		if (obj.getKind() == Obj.Var) {
			detect_usage(obj.getLevel() == 0 ? "upotreba globalne promenljive" : "upotreba lokalne promenljive", label,
					designatorSingle);
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
	Stack<Boolean> insideDoWhile = new Stack<>();

	public void visit(PrintStatement printStatement) {
		Struct type = printStatement.getExpr().struct;

		if (!(type.equals(MyTab.intType) || type.equals(MyTab.charType) || type.equals(MyTab.boolType))) {
			report_error("Printov Expr mora biti tipa int, char ili bool", printStatement);
			return;
		}
	}

	public void visit(PrintStatementValue printStatementValue) {
		Struct type = printStatementValue.getExpr().struct;

		if (!(type.equals(MyTab.intType) || type.equals(MyTab.charType) || type.equals(MyTab.boolType))) {
			report_error("Printov Expr mora biti tipa int, char ili bool", printStatementValue);
			return;
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
		if (designatorPartAssign.getDesignator().obj == null)
			return;

		int kind = designatorPartAssign.getDesignator().obj.getKind();

		DesignatorPartAssignPart dp = designatorPartAssign.getDesignatorPartAssignPart();

		if (dp instanceof DesignatorPartAssignPartError) {
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
			report_error("Designator mora oznacavati metodu", designatorPartFun);
			return;
		}

		detect_usage("Upotreba funkcije kao statement", designator.obj.getName(), designatorPartFun);

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
			return;
		}

		returnFound = true;
		Struct currMethType = currentMethod.getType();
		if (!currMethType.compatibleWith(MyTab.noType)) {
			report_error("Greska na liniji " + returnStatement.getLine() + " : "
					+ "tip izraza u return naredbi ne slaze se sa tipom povratne vrednosti funkcije "
					+ currentMethod.getName(), null);
			return;
		}
	}

	public void visit(ReturnStatementValue returnStatementValue) {
		if (currentMethod == null) {
			report_error("Ne sme postojati izvan tela (statickih) metoda, odnosno globalnih funkcija",
					returnStatementValue);
			return;
		}

		returnFound = true;
		Struct currMethType = currentMethod.getType();
		if (!currMethType.compatibleWith(returnStatementValue.getExpr().struct)) {
			report_error("Greska na liniji " + returnStatementValue.getLine() + " : "
					+ "tip izraza u return naredbi ne slaze se sa tipom povratne vrednosti funkcije "
					+ currentMethod.getName(), null);
			return;
		}
	}

	public void visit(ContinueStatement continueStatement) {
		if (!insideDoWhile.peek()) {
			report_error("Iskaz continue se moze koristiti samo unutar do-while petlje.", continueStatement);
			return;
		}
	}

	public void visit(BreakStatement breakStatement) {
		if (!insideDoWhile.peek()) {
			report_error("Iskaz break se moze koristiti samo unutar do-while petlje.", breakStatement);
			return;
		}
	}

	public void visit(DoWhileStatementStart doWhileStatementStart) {
		insideDoWhile.push(true);
	}

	public void visit(DoWhileStatement doWhileStatement) {
		insideDoWhile.pop();

		if (!doWhileStatement.getCondition().struct.equals(MyTab.boolType)) {
			report_error("Uslovni izraz Condition mora biti tipa bool", doWhileStatement);
			return;
		}
	}

	public void visit(IfStatement ifStatement) {
		if (ifStatement.getIfCondition().struct != null
				&& !ifStatement.getIfCondition().struct.equals(MyTab.boolType)) {
			report_error("Uslovni izraz Condition mora biti tipa bool", ifStatement);
			return;
		}
	}

	public void visit(IfElseStatement ifElseStatement) {
		if (!ifElseStatement.getIfCondition().struct.equals(MyTab.boolType)) {
			report_error("Uslovni izraz Condition mora biti tipa bool", ifElseStatement);
			return;
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

		if (condFact.struct != null && !condFact.struct.equals(MyTab.boolType)) {
			report_error("CondFact mora biti tipa boolean", condTermSingle);
			return;
		}

		condTermSingle.struct = MyTab.boolType;
	}

	public void visit(CondTermMultiple condTermMultiple) {
		CondFact condFact = condTermMultiple.getCondFact();
		CondTerm condTerm = condTermMultiple.getCondTerm();

		if (condFact.struct != null && !condFact.struct.equals(MyTab.boolType)
				|| !condTerm.struct.equals(MyTab.boolType)) {
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

		if (expr1.struct.getKind() == Struct.Array
				&& !(condFactTwo.getRelop() instanceof RelopNEQ || condFactTwo.getRelop() instanceof RelopEQEQ)) {
			report_error("Uz promenljive tipa niza, od relacionih operatora, mogu se koristiti samo != i ==",
					condFactTwo);
			return;
		}

		condFactTwo.struct = MyTab.boolType;
	}
	// endregion

	// region "Helpers"
	Obj currentMethod = null;
	boolean returnFound = false;
	boolean errorDetected = false;
	boolean validMainFound = false;
	int nVars;

	public Stack<List<String>> stackListString = new Stack<>();

	public void deeperLevel() {
		stackListString.push(new ArrayList<>());
	}

	public void higherLevel() {
		stackListString.pop();
	}

	public boolean addParamToLevel(String label, SyntaxNode syntaxNode) {
		List<String> lista = stackListString.peek();

		for (String l : lista) {
			if (l.equals(label)) {
				report_error("Parametar vec postoji na ovom nivou", syntaxNode);
				return false;
			}
		}

		lista.add(label);
		return true;
	}

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

	public void detect_usage(String message, String label, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		msg.append(", naziv: ").append(label);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(", na liniji ").append(line);
		Obj obj = MyTab.find(label);
		if (!MyTab.noObj.equals(obj)) {
			DumpSymbolTableVisitor visitor = new DumpSymbolTableVisitor();
			obj.accept(visitor);
			msg.append("\n").append(visitor.getOutput());
		}
		log.info(msg.toString());
	}

	public void checkParams(String label, SyntaxNode syntaxNode) {
		Obj obj = MyTab.find(label);

		if (MyTab.noObj.equals(obj) || obj.getKind() != Obj.Meth) {
			report_error("Nije pravilno ime metode", syntaxNode);
			return;
		}

		List<Struct> params = designatorParamStack.pop();

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

			detect_usage("upotreba formalnog parametra funkcije", sym.getName(), syntaxNode);

			if (sym.getType().getKind() == Struct.Array && sym.getType().getElemType().equals(MyTab.noType)) {
				return;
			}

			if (!sym.getType().equals(params.get(i))) {
				report_error("Parametar " + i + " se ne poklapa sa definicjom funkcije", syntaxNode);
			}
		}
	}

	public int getNVars() {
		return nVars;
	}

	public boolean passed() {
		return !errorDetected;
	}
	// endregion

	// region "Errors"
	public void visit(InitListElemError e) {
		errorDetected = true;
		report_error("Izvrsen oporavak definicije globalne promenljive do ;", e);
	}

	public void visit(MethodDeclArgsError e) {
		errorDetected = true;
		report_error("Izvrsen oporavak deklaracija formalnog parametra funkcije do , ili )", e);
	}

	public void visit(IfConditionError e) {
		errorDetected = true;
		report_error("Izvrsen oporavak logi??kog izraza unutar if konstrukcije do )", e);
	}

	public void visit(DesignatorPartAssignPartError e) {
		errorDetected = true;
		report_error("Izvrsen oporavak konstrukcija iskaza dodele do ;", e);
	}
	// endregion
}
