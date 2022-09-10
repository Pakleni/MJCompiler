package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.concepts.Obj;

public class MyTab extends Tab {

    public static Struct boolType = new Struct(Struct.Bool);

    public static void init() {
        Tab.init();
        Tab.currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));

        Obj lenObj = Tab.find("len");
        lenObj.getLocalSymbols().stream().forEach(x -> x.setFpPos(1));
        Method len = new Method();
        len.addParam(null);
        Method.globalna_lista.put("len", len);

        Obj chrObj = Tab.find("chr");
        chrObj.getLocalSymbols().stream().forEach(x -> x.setFpPos(1));
        Method chr = new Method();
        chr.addParam(null);
        Method.globalna_lista.put("chr", chr);

        Obj ordObj = Tab.find("ord");
        ordObj.getLocalSymbols().stream().forEach(x -> x.setFpPos(1));
        Method ord = new Method();
        ord.addParam(null);
        Method.globalna_lista.put("ord", ord);
    }
}
