package rs.ac.bg.etf.pp1;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;

public class Method {
    public static Map<String, Method> globalna_lista = new HashMap<String, Method>();

    public int address;
    public int opt_param_size = 0;
    public int for_param_size = 0;

    private List<Integer> method_params = new LinkedList<Integer>();

    public void addParam(Integer value) {
        if (value == null) {
            for_param_size++;
        } else {
            opt_param_size++;
        }

        method_params.add(value);
    }

    public Integer getParam(int i) {
        return method_params.get(i);
    }

    public int size() {
        return method_params.size();
    };
}
