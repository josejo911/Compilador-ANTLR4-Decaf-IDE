package com.josejo911.semanticControl;

import com.josejo911.antlr.decafParser;
import org.omg.CORBA.Object;

import java.util.LinkedList;
import java.util.List;

public class MethodElement {
    private String type;        // type
    private String firm;        // method name
    private String context;     // 'ambito' context of the method
    List<decafParser.ParameterContext> args = new LinkedList<>(); // list of args for the method

    public MethodElement(String type, String firm, List<decafParser.ParameterContext> args) {
        this.type = type;
        this.firm = firm;
        this.args = args;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public boolean equals(MethodElement me) {
        if ((me.firm.equals(this.firm))) {
            return true;
        } else return false;
    }

    public String getType() { return this.type; }
    public String getFirm() { return this.firm; }
    public String getContext() { return this.context; }
    public List<decafParser.ParameterContext> getArgs() { return this.args; }
}
