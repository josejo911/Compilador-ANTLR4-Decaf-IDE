package com.josejo911.semanticControl;

public class VarElement {
    private String varType;
    private String ID;
    private int NUM;
    private boolean isArray;
    private boolean isStruct;
    private String value;
    private int positionInStruct;

    private MethodElement context;

    public VarElement(String varType, String ID, MethodElement context) {
        this.context = context;
        this.ID = ID;
        this.varType = varType;
        isArray = false;

        if (varType.equals("int")) {
            value = "0";
        }
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean equals(VarElement ve) {
        if ((this.ID.equals(ve.ID)) && (this.context.equals(ve.context))) {
            return true;
        } else return false;
    }

    public void setNUM(String num) {
        this.NUM = Integer.parseInt(num);
        isArray = true;
    }

    public void setPositionInStruct(int positionInStruct) {
        this.positionInStruct = positionInStruct;
    }

    public int getPositionInStruct() { return this.positionInStruct; }
    public String getID() { return ID; }
    public String getVarType() { return varType; }
    public int getNUM() { return NUM; }
    public MethodElement getContext() { return context; }
    public boolean isArray() { return isArray; }
    public String getValue() { return value; }
    public boolean isStruct() { return isStruct; }
    public void setStruct(boolean struct) { isStruct = struct; }
}
