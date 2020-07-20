package com.josejo911.semanticControl;

import com.josejo911.antlr.decafBaseListener;
import com.josejo911.antlr.decafParser;
import org.antlr.v4.runtime.TokenStream;

import java.util.LinkedList;
import java.util.List;

/**
 * Author: Javier Jo
 * Version: 0.0.1
 * */
public class SemanticListener extends decafBaseListener {
    private decafParser parser;
    private boolean foundMain; // control for existance of 'main' method

    private List<String> semanticErrorsList; // list for semantic errors found
    private List<MethodElement> methodFirms; // a list for the found methods
    private List<VarElement> varList;        // a list for the variables
    private List<Operation> operationList;   // list for operations found in expressions
    List<Character> arithOperatorsList;

    private MethodElement currentMethodContext; // to check the current context of variable declarations and operations

    public SemanticListener(decafParser parser) {
        this.parser = parser;
        this.foundMain = false;
        this.semanticErrorsList = new LinkedList<>();
        this.methodFirms = new LinkedList<>();
        this.varList = new LinkedList<>();
        this.operationList = new LinkedList<>();

        this.arithOperatorsList = new LinkedList<>();
        arithOperatorsList.add('+');
        arithOperatorsList.add('-');
        arithOperatorsList.add('*');
        arithOperatorsList.add('/');
        arithOperatorsList.add('%');

        currentMethodContext = new MethodElement("void", "global", new LinkedList<>());
    }

    @Override
    public void enterExpression(decafParser.ExpressionContext ctx) {
        System.out.println(">>found expression!");
        System.out.println(ctx.getText());

        String operation = ctx.getText();

        if (!operation.contains("(")) {
            if (getNumberOfOperators(operation) == 1) {
                String operator = "";

                if (operation.contains("+")) { operator =  "+"; }
                if (operation.contains("-")) { operator =  "-"; }
                if (operation.contains("/")) { operator =  "/"; }
                if (operation.contains("*")) { operator =  "*"; }
                if (operation.contains("%")) { operator =  "%"; }

                String[] splits = operation.split("\\+|-|\\*|/|%");
                String[] splitsTypes = new String[splits.length];

                int i = 0;
                for (String str : splits) {
                    // check for variable type
                    for (VarElement ve : varList) {
                        if (ve.getID().equals(str) && ve.getContext().equals(currentMethodContext)) {
                            splitsTypes[i] = ve.getVarType();
                            break;
                        }
                    }

                    // check for method type
                    if (splitsTypes[i] == null) {
                        for (MethodElement me : methodFirms) {
                            if (me.getFirm().equals(str)) {
                                splitsTypes[i] = me.getType();
                                break;
                            }
                        }
                    }

                    // check for type of literal
                    if (splitsTypes[i] == null) {
                        if (str.equals("true") || str.equals("false")) {
                            // bool_literal
                            splitsTypes[i] = "boolean";
                        } else {
                            // int_literal
                            try {
                                int parse = Integer.parseInt(str);
                                splitsTypes[i] = "int";
                            } catch (Exception e) {
                                // its a char_literal
                                splitsTypes[i] = "char";
                            }
                        }
                    }
                    i++;
                }

                // now we have types and operator
                // method that returns type of operation
                String operationType = typeSystemOperations(operator, splitsTypes);
                if (operationType.equals("illegal")) {
                    semanticErrorsList.add("Illegal operation <i>" + operation + "</i>, <strong>"
                            + splitsTypes[0] + operator
                    + splitsTypes[1] + "</strong>");
                } else {
                    operationList.add(new Operation(operation, operationType));
                }

                // TODO replace type in operations list elements

            } else if (getNumberOfOperators(operation) > 1){
                operationList.add(new Operation(operation));
            } else if (getNumberOfOperators(operation) < 1) {
                String type = "";
                boolean isNotAVar = true;

                for (VarElement ve : varList) {
                    if (ve.getID().equals(operation)) {
                        isNotAVar = false;
                        break;
                    }
                }

                if (isNotAVar) {
                    if (operation.equals("true") || operation.equals("false")) {
                        // bool_literal
                        type = "boolean";
                    } else {
                        // int_literal
                        try {
                            int parse = Integer.parseInt(operation);
                            type = "int";
                        } catch (Exception e) {
                            // its a char_literal
                            type = "char";
                        }
                    }
                }

                if (!type.equals("")) {
                    // make replacement in list
                    List<Operation> tmp = new LinkedList<>();
                    //System.out.println("operationList antes " + operationList.toString());
                    for (Operation op : operationList) {
                        //System.out.println("entre a verificar la op " + op.toString());
                        String opOperation = op.getOperation();
                        String opType = op.getType();
                        if (opOperation.contains(operation)) {
                            //System.out.println("la operacion contiene la sub");
                            opOperation = opOperation.replace(operation, type);
                            //System.out.println("reemplazo " + opOperation);
                            Operation newOp = new Operation(opOperation, opType);
                            //System.out.println("Reemplazo a agregar " + newOp.toString());
                            tmp.add(newOp);
                        } else {
                            //System.out.println("No la contiene, copio lo anterior " + op.toString());
                            tmp.add(op);
                        }
                    }
                    //System.out.println("operationList antes de clear " + operationList.toString());
                    operationList.clear();
                    operationList.addAll(tmp);
                    //System.out.println("operationList despues de llenarla " + operationList.toString());
                }
            }
        } else {
            operationList.add(new Operation(operation));
        }
    }

    @Override
    public void exitStatement(decafParser.StatementContext ctx) {
        //TODO add verification for methods
        List<Operation> tmp2 = new LinkedList<>();
        tmp2.addAll(operationList);
        for (Operation op : operationList) {
            if (op.getType() != null) {
                List<Operation> tmp1 = new LinkedList<>();
                for (Operation opInterno : tmp2) {
                    if (opInterno.getOperation().contains(op.getOperation()) &&
                            !opInterno.getOperation().equals(op.getOperation())) {
                        // do replacement
                        String newOperation = opInterno.getOperation().replace(op.getOperation(), op.getType());
                        Operation newOp = new Operation(newOperation, opInterno.getType());
                        tmp1.add(newOp);
                    } else tmp1.add(opInterno);
                }
                tmp1.remove(op);
                tmp2.clear();
                tmp2.addAll(tmp1);
            }
        }
        operationList.clear();
        operationList.addAll(tmp2);

        // verify if expressions still have variables that can be changed by the type
        for (VarElement ve : varList) {
            List<Operation> tmp = new LinkedList<>();
            for (Operation op : operationList) {
                if (op.getOperation().contains(ve.getID())) {
                    String newOperation = op.getOperation().replace(ve.getID(), ve.getVarType());
                    Operation newOp = new Operation(newOperation, op.getType());
                    tmp.add(newOp);
                } else tmp.add(op);
            }
            operationList.clear();
            operationList.addAll(tmp);
        }

        System.out.println("printing" + operationList.toString());

        // reduce expressions in operations list
        boolean stop = false;
        while (!stop) {
            // find atomic expressions and give them a type
            List<Operation> tmp = new LinkedList<>();
            for (Operation op : operationList) {
                String operation = op.getOperation();
                if (getNumberOfOperators(operation) == 1 && op.getType()==null) {
                    String operator = "";

                    if (operation.contains("+")) { operator =  "+"; }
                    if (operation.contains("-")) { operator =  "-"; }
                    if (operation.contains("/")) { operator =  "/"; }
                    if (operation.contains("*")) { operator =  "*"; }
                    if (operation.contains("%")) { operator =  "%"; }

                    String[] splits = operation.replaceAll("\\(|\\)", "").split("\\+|-|\\*|/|%");
                    String typeOfOperation = typeSystemOperations(operator, splits);

                    if (typeOfOperation.equals("illegal")) {
                        semanticErrorsList.add("Expression <strong>" + operation + "</strong> is an invalid expression. <br>At <strong>"+
                        ctx.getText() + "</strong>");
                    }

                    Operation newOperation = new Operation(operation, typeOfOperation);
                    tmp.add(newOperation);
                } else tmp.add(op);
            }

            operationList.clear();
            operationList.addAll(tmp);

            // now make replacements
            tmp2 = new LinkedList<>();
            tmp2.addAll(operationList);
            for (Operation op : operationList) {
                if (op.getType() != null) {
                    List<Operation> tmp1 = new LinkedList<>();
                    for (Operation opInterno : tmp2) {
                        if (opInterno.getOperation().contains(op.getOperation()) &&
                                !opInterno.getOperation().equals(op.getOperation())) {
                            // do replacement
                            String newOperation = opInterno.getOperation().replace(op.getOperation(), op.getType());
                            Operation newOp = new Operation(newOperation, opInterno.getType());
                            tmp1.add(newOp);
                        } else tmp1.add(opInterno);
                    }
                    tmp1.remove(op);
                    tmp2.clear();
                    tmp2.addAll(tmp1);
                }
            }
            operationList.clear();
            operationList.addAll(tmp2);

            System.out.println("size: " + operationList.size());
            // check for flag
            if (operationList.size() == 0) {
                stop = true;
                System.out.println("last operation is: " + operationList.toString());
            }
        }
        System.out.println("printing after while " + operationList.toString());
    }

    public String typeSystemOperations(String operator, String[] types) {
        System.out.println("checked types for operations");
        // operations with ints
        if (types[0].equals("int") && types[1].equals("int")) {
            if ((operator.equals("+")) || (operator.equals("-"))
                    || (operator.equals("/")) ||(operator.equals("*"))
                    || (operator.equals("%"))){
                return "int";
            } else if ((operator.equals("<")) || (operator.equals(">"))
                    || (operator.equals("<=")) || (operator.equals(">="))) {
                return "boolean";
            } else if (operator.equals("=")) {
                return "void";
            } else if (operator.equals("==") || operator.equals("!=")) {
                return "boolean";
            } else return "illegal";
        } else if (types[0].equals("boolean") && types[1].equals("boolean")) {
            if ((operator.equals("&&")) || (operator.equals("||"))) {
                return "boolean";
            } else return "illegal";
        } else if (types[0].equals(types[1])) {
            if (operator.equals("=")) {
                return "void";
            } else if (operator.equals("==") || operator.equals("!=")) {
                return "boolean";
            } else return "illegal";
        } else return "illegal";
    }

    public int getNumberOfOperators(String operation) {
        int operators = 0;
        for (int i = 0; i < operation.length(); i++) {
            if (arithOperatorsList.contains(operation.charAt(i))) {
                operators++;
            }
        }
        return operators;
    }

    @Override
    public void enterStatement(decafParser.StatementContext ctx) {
        if (ctx.getText().contains("return")) {
            String[] lineSplit = ctx.getText().replace(";", "").split("return");
            String returnVal = lineSplit[lineSplit.length-1];

            if (currentMethodContext.getType().equals("void")) {
                // should not return nothing
                semanticErrorsList.add("Method <strong>" + currentMethodContext.getFirm() + "</strong> " +
                        "declared as void. Invalid return statement.");
            }
        }

        // location = expression type
        if (ctx.location() != null && ctx.expression() != null) {
            // check first if the assign is a method call
            // check if its a method_call
            String operation = ctx.expression().getText();
            String[] split = operation.split("\\(.\\)");
            String type = "";
            for (MethodElement me : methodFirms) {
                if (me.getFirm().equals(split[0])) {
                    type = me.getType();
                    break;
                }
            }

            if (!type.equals("")) {
                //get location type
                String locType = "";
                for (VarElement ve : varList) {
                    if (ve.getID().equals(ctx.location().getText())) {
                        locType = ve.getVarType();
                        break;
                    }
                }

                String[] types = new String[2];
                types[0] = locType;
                types[1] = type;

                System.out.println("type of locType " + locType);
                System.out.println("type of type " + type);

                String operationType = typeSystemOperations("=", types);
                if (operationType.equals("illegal")) {
                    semanticErrorsList.add("Illegal operation <i>" + ctx.location().getText() + "=" + operation +
                            "</i>, <strong>"
                            + types[0] + "="
                            + types[1] + "</strong>");
                }
            } else {
                // TODO didnt find a method but the expression can be an operation
            }
        }

        // TODO if
        if (ctx.getText().contains("if(")) {
            System.out.println("found if statement");
        }
    }

    @Override
    public void enterParameter(decafParser.ParameterContext ctx) {
        String parameterType = "";
        String ID = "";

        if (ctx.parameterType() != null) {
            parameterType = ctx.parameterType().getText();
        }

        if (ctx.ID() != null) {
            ID = ctx.ID().getText();
        }

        // Check if variable has already been declared in the same context
        VarElement newVar = new VarElement(parameterType, ID, currentMethodContext);

        boolean hasVarBeenDeclared = false;

        if (varList.isEmpty()) {
            hasVarBeenDeclared = true;
            varList.add(newVar);
        } else {
            for (VarElement listVar : varList) {
                if (listVar.equals(newVar)) {
                    hasVarBeenDeclared = true;
                    semanticErrorsList.add("Variable <strong>\"" + ID + "\"</strong> has " +
                            "already been declared in the context of <strong>" + currentMethodContext.getFirm() + "</strong>");
                    break;
                }
            }
        }

        if (!hasVarBeenDeclared) { varList.add(newVar); }

    }

    @Override
    public void enterLocation(decafParser.LocationContext ctx) {
        String ID = "";

        if (ctx.ID() != null) {
            ID = ctx.ID().getText();
        }

        // verify if ID exits. If not, the location is invalid, since the variable does not exist.
        boolean isLocationDefined = false;
        // location can be a variable
        for (VarElement listVar : varList) {
            if ((listVar.getID().equals(ID)) && (listVar.getContext().equals(currentMethodContext))) {
                // the location exists
                isLocationDefined = true;
                break;
            } else if ((listVar.getID().equals(ID)) && (listVar.getContext().getFirm().equals("global"))) {
                // the location refers to a global variable
                isLocationDefined = true;
                break;
            }
        }

        if (!isLocationDefined) {
            semanticErrorsList.add("Variable <strong>\"" + ID + "\"</strong> is not defined. ");
        }
    }

    @Override
    public void enterVarDeclaration(decafParser.VarDeclarationContext ctx) {
        String varType = "";    // type
        String ID = "";         // name of var
        String num = "";        // if var is array, num is the number of elements

        if (ctx.varType() != null) {
            varType = ctx.varType().getText();
        }

        if (ctx.ID() != null) {
            ID = ctx.ID().getText();
        }

        if (ctx.NUM() != null) {
            num = ctx.NUM().getText();
        }

        // Check if variable has already been declared in the same context
        VarElement newVar = new VarElement(varType, ID, currentMethodContext);
        if (!num.equals("")) {
            newVar.setNUM(num);
            if (Integer.parseInt(num) == 0) {
                semanticErrorsList.add("<strong>\"" + ID + "\"</strong> length " +
                        "has to be bigger than 0.");
            }
        }

        boolean hasVarBeenDeclared = false;

        if (varList.isEmpty()) {
            hasVarBeenDeclared = true;
            varList.add(newVar);
        } else {
            for (VarElement listVar : varList) {
                if (listVar.equals(newVar)) {
                    hasVarBeenDeclared = true;
                    semanticErrorsList.add("Variable <strong>\"" + ID + "\"</strong> has " +
                            "already been declared in the context of <strong>" + currentMethodContext.getFirm() + "</strong>");
                    break;
                }
            }
        }

        if (!hasVarBeenDeclared) { varList.add(newVar); }

    }

    @Override
    public void enterMethodCall(decafParser.MethodCallContext ctx) {
        String firm = ""; // method firm
        if (ctx.ID() != null) {
            firm = ctx.ID().getText();
        }

        // verify if method call is valid, this means if the method has been correctly declared before
        boolean isMethodDeclared = false;
        for (MethodElement listMethod : methodFirms) {
            if (listMethod.getFirm().equals(firm)) { isMethodDeclared = true; break; }
        }

        if (!isMethodDeclared) { semanticErrorsList.add("Can't make call to undeclared method " + firm); }
    }

    @Override
    public void enterMethodDeclaration(decafParser.MethodDeclarationContext ctx) {
        // gather values
        TokenStream tokens = parser.getTokenStream();
        String type = ""; // method type
        String firm = ""; // method firm
        List<decafParser.ParameterContext> args = new LinkedList<>(); // args
        if (ctx.methodType()!= null) {
            type = tokens.getText(ctx.methodType());
        }

        if (ctx.ID() != null) {
            firm = ctx.ID().getText();
        }

        if (!ctx.parameter().isEmpty()) {
            args = ctx.parameter();
        }

        // existance of 'main' method
        if ((type.equals("void")) && firm.equals("main") && args.isEmpty()) {
            if (!foundMain) {
                foundMain = true;
                currentMethodContext = new MethodElement(type, firm, args);
                methodFirms.add(currentMethodContext); // add method to method list
                System.out.println(ctx.getRuleContext().toString());
            } else {
                semanticErrorsList.add("Method " + firm + " with arguments: " + args.toString() + " has" +
                        "already been declared.");
            }
        }
        if ((!type.equals("void")) && firm.equals("main") && args.isEmpty()) {
            foundMain = true;
            semanticErrorsList.add("'main' method must have a 'void' return type.");
        }
        if ((type.equals("void")) && firm.equals("main") && !args.isEmpty()) {
            foundMain = true;
            semanticErrorsList.add("'main' method can't have arguments.");
        }

        // rules for other methods that are not 'main'
        if (!firm.equals("main")) {
            MethodElement newMethod = new MethodElement(type, firm, args);
            currentMethodContext = newMethod;
            //System.out.println("nuevo metodo " + firm);
            //System.out.println(methodFirms.toString());

            boolean foundMethod = false;

            for (MethodElement listMethod : methodFirms) {
                if (listMethod.equals(newMethod)) {
                    semanticErrorsList.add("Method " + firm + " has " +
                            "already been declared.");
                    foundMethod = true;
                    break;
                }
            }

            if (!foundMethod) {
                methodFirms.add(newMethod);
            }

//            if (!methodFirms.contains(newMethod)) { methodFirms.add(newMethod); } else {
//                semanticErrorsList.add("Method " + firm + " with arguments: " + args.toString() + " has " +
//                        "already been declared.");
//            }
        }
    }

    public List<String> getSemanticErrorsList() {
        // existance of 'main' method
        if (!foundMain) { semanticErrorsList.add("No 'main' method declared."); }

        return semanticErrorsList;
    }
}
