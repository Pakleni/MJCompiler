package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.concepts.Obj;

public class MyTab extends Tab {

    public static Struct boolType = new Struct(Struct.Bool);

    public static void init() {
        Tab.init();
        Tab.currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
    }
}
