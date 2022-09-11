package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {

	private int mainPc;

	public int getMainPc() {
		return mainPc;
	}

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

	@Override
	public void visit(DesignatorPartAssign designatorPartAssign) {
		Code.store(designatorPartAssign.getDesignator().obj);
	}

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
		Code.put(0);
	}

	@Override
	public void visit(DesignatorIdent designatorIdent) {
		SyntaxNode parent = designatorIdent.getParent();
		if (DesignatorPartAssign.class != parent.getClass() && DesignatorPartFun.class != parent.getClass()
				&& FactorDesignatorFun.class != parent.getClass()) {
			Code.load(designatorIdent.obj);
		}
	}

	@Override
	public void visit(DesignatorSingle designator) {
		SyntaxNode parent = designator.getParent();
		if (DesignatorPartAssign.class != parent.getClass() && DesignatorPartFun.class != parent.getClass()
				&& FactorDesignatorFun.class != parent.getClass()) {
			Code.load(designator.obj);
		}
	}

	int actParNum = 0;

	@Override
	public void visit(ActParsMultiple actPars) {
		actParNum++;
	}

	@Override
	public void visit(ActParsSingle actPars) {
		actParNum++;
	}

	void addOptPars(String label) {
		Method method = Method.globalna_lista.get(label);

		for (int i = actParNum; i < method.size(); i++) {
			Code.loadConst(method.getParam(i));
		}

		actParNum = 0;
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
	public void visit(ExprMultiple exprMultiple) {
		Addop addop = exprMultiple.getAddop();

		if (addop instanceof AddopPLUS) {
			Code.put(Code.add);
		} else {
			Code.put(Code.sub);
		}
	}
}
