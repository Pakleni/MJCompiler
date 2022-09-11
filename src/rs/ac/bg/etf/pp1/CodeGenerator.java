package rs.ac.bg.etf.pp1;

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
	// endregion

	// region "Method"
	@Override
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

	@Override
	public void visit(MethodDecl methodDecl) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	// endregion

	// region "Expr"
	@Override
	public void visit(FactorNumber factorNumber) {
		Code.loadConst(factorNumber.getN1());
	}

	@Override
	public void visit(FactorChar factorChar) {
		Code.loadConst(factorChar.getC1());
	}

	@Override
	public void visit(FactorBool factorBool) {
		Code.loadConst(factorBool.getB1());
	}

	@Override
	public void visit(FactorNewArr factorNewArr) {
		Code.put(Code.newarray);

		if (factorNewArr.getType().struct.equals(MyTab.intType)) {
			Code.put(1);
		} else {
			Code.put(0);
		}

	}

	@Override
	public void visit(FactorDesignatorFun factorDesignatorFun) {
		Obj functionObj = factorDesignatorFun.getDesignator().obj;
		addOptPars(functionObj.getName());

		int offset = functionObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}

	@Override
	public void visit(ExprMultiple exprMultiple) {
		Addop addop = exprMultiple.getAddop();

		if (addop instanceof AddopPLUS) {
			Code.put(Code.add);
		} else {
			Code.put(Code.sub);
		}
	}

	@Override
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

	@Override
	public void visit(ExprNeg exprNeg) {
		Code.put(Code.neg);
	}

	// endregion

	// region "Function Parameters"
	int actParNum = 0;

	@Override
	public void visit(ActParsMultiple actPars) {
		actParNum++;
	}

	@Override
	public void visit(ActParsSingle actPars) {
		actParNum++;
	}
	// endregion

	// region "Designator"
	@Override
	public void visit(DesignatorIdent designatorIdent) {
		Class<? extends SyntaxNode> parent = designatorIdent.getParent().getClass();

		if (FactorDesignator.class.equals(parent) ||
				DesignatorIdent.class.equals(parent)) {
			Code.load(designatorIdent.obj);
		}
	}

	@Override
	public void visit(DesignatorSingle designator) {
		Class<? extends SyntaxNode> parent = designator.getParent().getClass();

		if (FactorDesignator.class.equals(parent) ||
				DesignatorIdent.class.equals(parent)) {
			Code.load(designator.obj);
		}
	}

	// endregion

	// region "Statements"
	@Override
	public void visit(DesignatorPartAssign designatorPartAssign) {
		Code.store(designatorPartAssign.getDesignator().obj);
	}

	@Override
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

	@Override
	public void visit(DesignatorPartInc designatorPartInc) {
		Code.put(Code.inc);
		Code.put(designatorPartInc.getDesignator().obj.getAdr());
		Code.put(1);
	}

	@Override
	public void visit(DesignatorPartDec designatorPartDec) {
		Code.put(Code.inc);
		Code.put(designatorPartDec.getDesignator().obj.getAdr());
		Code.put(-1);
	}

	@Override
	public void visit(PrintStatement printStatement) {
		Code.put(Code.const_5);
		Code.put(Code.print);
	}

	@Override
	public void visit(PrintStatementValue printStatementValue) {
		Code.loadConst(printStatementValue.getN2());
		Code.put(Code.print);
	}

	@Override
	public void visit(ReadStatement readStatement) {
		Code.put(Code.read);
		Code.store(readStatement.getDesignator().obj);
	}

	@Override
	public void visit(ReturnStatementValue returnExpr) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	@Override
	public void visit(ReturnStatement returnNoExpr) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	// endregion

	// region "Conditions"
	@Override
	public void visit(CondFactSingle condFactSingle) {
	}

	@Override
	public void visit(CondFactTwo condFactTwo) {
		Relop relop = condFactTwo.getRelop();

		if (relop instanceof RelopEQEQ) {
			// Code.eq;
		} else if (relop instanceof RelopNEQ) {
			// Code.ne;
		} else if (relop instanceof RelopGR) {
			// Code.gt;
		} else if (relop instanceof RelopGREQ) {
			// Code.ge;
		} else if (relop instanceof RelopLS) {
			// Code.lt;
		} else {
			// Code.le;
		}
	}

	@Override
	public void visit(CondTermMultiple condTermMultiple) {
	}

	@Override
	public void visit(ConditionMultiple conditionMultiple) {
	}
	// endregion

}
