package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {
	// region "Helpers"
	private int mainPc;

	public int getMainPc() {
		return mainPc;
	}

	void addOptPars(String label) {
		Method method = Method.globalna_lista.get(label);

		for (int i = actParNum; i < method.size(); i++) {
			Code.loadConst(method.getParam(i));
		}

		actParNum = 0;
	}

	void leaveFunction() {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	public void fixup(Stack<List<Integer>> st) {
		for (Integer i : st.peek()) {
			Code.fixup(i);
		}
		st.peek().clear();
	}
	// endregion

	// region "Program"
	public void visit(ProgName progName) {
		Obj chr = MyTab.find("chr");
		chr.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.put(Code.load_n);
		Code.loadConst(256);
		Code.put(Code.rem);
		leaveFunction();

		Obj len = MyTab.find("len");
		len.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.put(Code.load_n);
		Code.put(Code.arraylength);
		leaveFunction();

		Obj ord = MyTab.find("ord");
		ord.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.put(Code.load_n);
		leaveFunction();
	}
	// endregion

	// region "Method"
	public void visit(MethodTypeName methodTypeName) {
		if ("main".equalsIgnoreCase(methodTypeName.getValue())) {
			mainPc = Code.pc;
		}
		methodTypeName.obj.setAdr(Code.pc);

		Method method = Method.globalna_lista.get(methodTypeName.obj.getName());

		// Generate the entry.
		Code.put(Code.enter);
		Code.put(method.size());
		Code.put(methodTypeName.obj.getLocalSymbols().size());
	}

	public void visit(MethodDecl methodDecl) {
		leaveFunction();
	}

	// endregion

	// region "Expr"
	public void visit(FactorNumber factorNumber) {
		Code.loadConst(factorNumber.getN1());
	}

	public void visit(FactorChar factorChar) {
		Code.loadConst(factorChar.getC1());
	}

	public void visit(FactorBool factorBool) {
		Code.loadConst(factorBool.getB1());
	}

	public void visit(FactorNewArr factorNewArr) {
		Code.put(Code.newarray);

		if (factorNewArr.getType().struct.equals(MyTab.intType)) {
			Code.put(1);
		} else {
			Code.put(0);
		}

	}

	public void visit(FactorDesignatorFun factorDesignatorFun) {
		Obj functionObj = factorDesignatorFun.getDesignator().obj;
		addOptPars(functionObj.getName());

		int offset = functionObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}

	public void visit(ExprMultiple exprMultiple) {
		Addop addop = exprMultiple.getAddop();

		if (addop instanceof AddopPLUS) {
			Code.put(Code.add);
		} else {
			Code.put(Code.sub);
		}
	}

	public void visit(TermMultiple termMultiple) {
		Mulop mulop = termMultiple.getMulop();

		if (mulop instanceof MulopMOD) {
			Code.put(Code.rem);
		} else if (mulop instanceof MulopDIV) {
			Code.put(Code.div);
		} else {
			Code.put(Code.mul);
		}
	}

	public void visit(ExprNeg exprNeg) {
		Code.put(Code.neg);
	}

	// endregion

	// region "Function Parameters"
	int actParNum = 0;

	public void visit(ActParsMultiple actPars) {
		actParNum++;
	}

	public void visit(ActParsSingle actPars) {
		actParNum++;
	}
	// endregion

	// region "Designator"
	public void visit(DesignatorIdent designatorIdent) {
		Class<? extends SyntaxNode> parent = designatorIdent.getParent().getClass();

		if (FactorDesignator.class.equals(parent) ||
				DesignatorIdent.class.equals(parent)) {
			Code.load(designatorIdent.obj);
		}
	}

	public void visit(DesignatorSingle designator) {
		Class<? extends SyntaxNode> parent = designator.getParent().getClass();

		if (FactorDesignator.class.equals(parent) ||
				DesignatorIdent.class.equals(parent)) {
			Code.load(designator.obj);
		}
	}

	// endregion

	// region "Statements"
	public void visit(DesignatorPartAssign designatorPartAssign) {
		Code.store(designatorPartAssign.getDesignator().obj);
	}

	public void visit(DesignatorPartFun designatorPartFun) {
		Obj functionObj = designatorPartFun.getDesignator().obj;
		addOptPars(functionObj.getName());

		int offset = functionObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
		if (!MyTab.noType.equals(functionObj.getType())) {
			Code.put(Code.pop);
		}
	}

	public void visit(DesignatorPartInc designatorPartInc) {
		Code.load(designatorPartInc.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(designatorPartInc.getDesignator().obj);
	}

	public void visit(DesignatorPartDec designatorPartDec) {
		Code.load(designatorPartDec.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(designatorPartDec.getDesignator().obj);
	}

	public void visit(PrintStatement printStatement) {

		if (printStatement.getExpr().struct.equals(MyTab.charType)) {
			Code.put(Code.const_1);
			Code.put(Code.bprint);
		} else {
			Code.put(Code.const_5);
			Code.put(Code.print);
		}
	}

	public void visit(PrintStatementValue printStatementValue) {
		Code.loadConst(printStatementValue.getN2());
		if (printStatementValue.getExpr().struct.equals(MyTab.charType)) {
			Code.put(Code.bprint);
		} else {
			Code.put(Code.print);
		}
	}

	public void visit(ReadStatement readStatement) {
		if (readStatement.getDesignator().obj.getType().equals(MyTab.charType)) {
			Code.put(Code.bread);
		} else {
			Code.put(Code.read);
		}
		Code.store(readStatement.getDesignator().obj);
	}

	public void visit(ReturnStatementValue returnExpr) {
		leaveFunction();
	}

	public void visit(ReturnStatement returnNoExpr) {
		leaveFunction();
	}
	// endregion

	// region "Conditions"
	Stack<List<Integer>> trueStack = new Stack<>();
	Stack<List<Integer>> falseStack = new Stack<>();

	public void visit(CondFactTwo condFactTwo) {
		Relop relop = condFactTwo.getRelop();

		int opCode;

		if (relop instanceof RelopEQEQ) {
			opCode = Code.eq;
		} else if (relop instanceof RelopNEQ) {
			opCode = Code.ne;
		} else if (relop instanceof RelopGR) {
			opCode = Code.gt;
		} else if (relop instanceof RelopGREQ) {
			opCode = Code.ge;
		} else if (relop instanceof RelopLS) {
			opCode = Code.lt;
		} else {
			opCode = Code.le;
		}

		Code.putFalseJump(opCode, 0);
		falseStack.peek().add(Code.pc - 2);
	}

	public void visit(CondFactSingle condFactSingle) {
		Code.loadConst(0);
		Code.putFalseJump(Code.ne, 0);
		falseStack.peek().add(Code.pc - 2);
	}

	public void visit(OrOperator orOperator) {
		// ako je condition pre mene bio tacan, upasce ovde na stek
		Code.putJump(0);
		trueStack.peek().add(Code.pc - 2);
		// ako je condition pre mene bio netacan, skoci ovde
		fixup(falseStack);
	}
	// endregion

	// region "If Else"
	Stack<List<Integer>> skipElseStack = new Stack<>();

	public void visit(IfConditionActual ifConditionActual) {
		// na ovom PC pocinje code 1, svi true
		fixup(trueStack);
	}

	public void visit(ElseStart elseStart) {
		// na ovom PC zavrsava code 1
		// ako smo izvrsili code 1 preskoci else:
		Code.putJump(0);
		skipElseStack.peek().add(Code.pc - 2);

		// na ovom PC pocinje else
		// svi false condition-i treba da dodju ovde
		fixup(falseStack);
	}

	public void visit(IfStart ifStart) {
		trueStack.add(new ArrayList<>());
		falseStack.add(new ArrayList<>());
		skipElseStack.add(new ArrayList<>());
	}

	public void visit(IfStatement ifStatement) {

		// na ovom PC se izlazi iz ifa
		// svi false condition-i treba da dodju ovde
		fixup(falseStack);

		trueStack.pop();
		falseStack.pop();
		skipElseStack.pop();
	}

	public void visit(IfElseStatement ifElseStatement) {

		// svi skip else treba da dodju ovde
		fixup(skipElseStack);

		trueStack.pop();
		falseStack.pop();
		skipElseStack.pop();
	}
	// endregion

	// region "Do While"
	Stack<List<Integer>> breakStack = new Stack<>();
	Stack<List<Integer>> continueStack = new Stack<>();
	Stack<Integer> whileStartStack = new Stack<>();

	public void visit(ContinueStatement continueStatement) {
		Code.putJump(0);
		continueStack.peek().add(Code.pc - 2);
	}

	public void visit(BreakStatement breakStatement) {
		Code.putJump(0);
		breakStack.peek().add(Code.pc - 2);
	}

	public void visit(DoWhileStatementStart doWhileStatementStart) {
		trueStack.add(new ArrayList<>());
		falseStack.add(new ArrayList<>());
		continueStack.add(new ArrayList<>());
		breakStack.add(new ArrayList<>());

		whileStartStack.add(Code.pc);
	}

	public void visit(DoWhileWhile doWhileWhile) {
		fixup(continueStack);
	}

	public void visit(DoWhileStatement doWhileStatement) {
		fixup(trueStack);
		Code.putJump(whileStartStack.pop());
		fixup(falseStack);
		fixup(breakStack);

		trueStack.pop();
		falseStack.pop();
		continueStack.pop();
		breakStack.pop();
	}
	// endregion
}
