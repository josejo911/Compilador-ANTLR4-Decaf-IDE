package com.josejo911.semanticControl;

import com.josejo911.antlr.decafBaseListener;
import com.josejo911.antlr.decafParser;
import org.antlr.v4.runtime.TokenStream;

import java.io.*;
import java.util.*;

public class SemanticListener extends decafBaseListener {
    private decafParser parser;
    private boolean foundMain; // control for existance of 'main' method

    private List<String> semanticErrorsList; // list for semantic errors found
    private List<MethodElement> methodFirms; // a list for the found methods
    private List<VarElement> varList;        // a list for the variables
    private List<Operation> operationList, tmpOpList;   // list for operations found in expressions
    List<String> arithOperatorsList;

    private MethodElement currentMethodContext; // to check the current context of variable declarations and operations

    private String tacIndent;
    private List<String> branchVariables;
    private int branchVariablesCount;

    /** Temporary variables stuff **/
    private int tempVarsCount;
    private HashMap<String, String> tempVarsValues;         // Hashmap linking <expression, tempVar>
    private String currentLocation;
    private List<decafParser.ExpressionContext> evaluatedExpressions;

    private int popParamsSize;
    private boolean writeToTACSignal;
    private List<String> outTAC;
    private String returnTAC;
    private int positionInStructCount;
    private String methodCallLocation;

    public SemanticListener(decafParser parser) {
        this.parser = parser;
        this.foundMain = false;
        this.semanticErrorsList = new LinkedList<>();
        this.methodFirms = new LinkedList<>();
        this.varList = new LinkedList<>();
        this.operationList = new LinkedList<>();
        this.tmpOpList = new LinkedList<>();
        this.evaluatedExpressions = new LinkedList<>();
        this.tempVarsValues = new HashMap<>();

        this.arithOperatorsList = new LinkedList<>();
        arithOperatorsList.add("+");
        arithOperatorsList.add("-");
        arithOperatorsList.add("*");
        arithOperatorsList.add("/");
        arithOperatorsList.add("%");
        arithOperatorsList.add("&&");
        arithOperatorsList.add("||");
        arithOperatorsList.add("==");
        arithOperatorsList.add("!=");
        arithOperatorsList.add("<");
        arithOperatorsList.add(">");
        arithOperatorsList.add("<=");
        arithOperatorsList.add(">=");

        currentMethodContext = new MethodElement("void", "global", new LinkedList<>());

        tacIndent = "";
        branchVariables = new LinkedList<>();
        tempVarsCount = 0;
        currentLocation = "_top_null";
        writeToTACSignal = false;
        outTAC = new ArrayList<>();
    }

    public void operateExpression(decafParser.ExpressionContext ctx) {
        System.out.println(">>found expression!");
        System.out.println(ctx.getText());

        String operation = ctx.getText();

        // TODO check this for the future in case something goes wrong
        // evaluate if operation is possible methodcall
        if (operation.matches("(.)*[(](.)*[)]")) {
            // it has methodcall form
            String type = "";
            String[] split = operation.split("\\(");
            for (MethodElement me : methodFirms) {
                if (me.getFirm().equals(split[0])) {
                    type = me.getType();
                }
            }

            operationList.add(new Operation(type, type));
        } else {
            if (!operation.contains("(")) {
                if (getNumberOfOperators(operation) == 1) {
                    String operator = "";

                    if (operation.contains("+")) { operator =  "+"; }
                    if (operation.contains("-")) { operator =  "-"; }
                    if (operation.contains("/")) { operator =  "/"; }
                    if (operation.contains("*")) { operator =  "*"; }
                    if (operation.contains("%")) { operator =  "%"; }
                    if (operation.contains("&&")) { operator =  "&&"; }
                    if (operation.contains("||")) { operator =  "||"; }
                    if (operation.contains("==")) { operator =  "=="; }
                    if (operation.contains("!=")) { operator =  "!="; }
                    if (operation.contains("<")) { operator =  "<"; }
                    if (operation.contains("<=")) { operator =  "<="; }
                    if (operation.contains(">")) { operator =  ">"; }
                    if (operation.contains(">=")) { operator =  ">="; }

                    //String[] splits = operation.split("\\+|-|\\*|/|%|&&|\\|\\||==|!=|<|<=|>|>=");
                    String[] splits = splitter(operation, operator);
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
                                    // check if is array
                                    if (str.matches("(.)*(\\[([0-9])\\])")) {
                                        String[] internSplit = str.split("\\[");

                                        for (VarElement ve : varList) {
                                            if (ve.getID().equals(internSplit[0]) && ve.getContext().equals(currentMethodContext)) {
                                                splitsTypes[i] = ve.getVarType();
                                                break;
                                            }
                                        }
                                    } else {
                                        // its a char_literal
                                        splitsTypes[i] = "char";
                                    }
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
                        tmpOpList.add(new Operation(operation, operationType));
                    }

                    // TODO replace type in operations list elements

                } else if (getNumberOfOperators(operation) > 1){
                    operationList.add(new Operation(operation));
                    tmpOpList.add(new Operation(operation));
                } else if (getNumberOfOperators(operation) < 1) {
                    //assignTemporals();

                    String type = "";
                    boolean isNotAVar = true;

                    for (VarElement ve : varList) {
                        if (ve.getID().equals(operation)) {
                            isNotAVar = false;
                            type = ve.getVarType();
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
                        if (operationList.size() == 0) {
                            Operation newOp = new Operation(operation, type);
                            operationList.add(newOp);
                            tmpOpList.add(newOp); // TODO check if this does not fuck anything
                        } else if (tmpOpList.size() == 0) {
                            Operation newOp = new Operation(operation, type);
                            tmpOpList.add(newOp);
                        }
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
                    } else {
                        Operation newOp = new Operation(operation, type);
                        operationList.add(newOp);
                    }
                } else if (getNumberOfOperators(operation) == 0) {
                    System.out.println("no operadores");
                }
            } else {
                System.out.println(operationList.toString());
                operationList.add(new Operation(operation));
            }
        }
    }

    @Override
    public void enterExpression(decafParser.ExpressionContext ctx) {
        if (!evaluatedExpressions.contains(ctx)) {
            if (ctx.methodCall() != null) {
                System.out.println("is method");
            } else {
                operateExpression(ctx);
            }
        }
    }

    public String getTypeOfExpression(String ctx) {
        String finalType = "";
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
                if (tmp1.size() != 1) {
                    tmp1.remove(op);
                }
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
                if (op.getOperation().contains(ve.getID()) && ve.getContext().equals(currentMethodContext)
                        && (!op.getOperation().matches("(boolean||int||char)"))) {
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
                    if (operation.contains("&&")) { operator =  "&&"; }
                    if (operation.contains("||")) { operator =  "||"; }
                    if (operation.contains("==")) { operator =  "=="; }
                    if (operation.contains("!=")) { operator =  "!="; }
                    if (operation.contains("<")) { operator =  "<"; }
                    if (operation.contains("<=")) { operator =  "<="; }
                    if (operation.contains(">")) { operator =  ">"; }
                    if (operation.contains(">=")) { operator =  ">="; }

                    String[] splits = operation.replaceAll("\\(|\\)", "").split("\\+|-|\\*|/|%|&&|\\|\\||==|!=|<|<=|>|>=");
                    String typeOfOperation = typeSystemOperations(operator, splits);

                    if (typeOfOperation.equals("illegal")) {
                        semanticErrorsList.add("Expression <strong>" + operation + "</strong> is an invalid expression. <br>At <strong>"+
                                ctx + "</strong>");
                    }

                    Operation newOperation = new Operation(operation, typeOfOperation);
                    tmp.add(newOperation);
                } else {
                    if (operation.matches("\\((boolean||int||char)\\)")) {
                        Operation newOperation =
                                new Operation(operation, operation.replace("(", "").replace(")",""));
                        tmp.add(newOperation);
                    }else tmp.add(op);
                }
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
                    if (operationList.size() == 1) {
                        System.out.println("tmp1 es " + tmp1.toString());
                        finalType = tmp1.get(0).getType();
                    }
                    tmp1.remove(op);
                    tmp2.clear();
                    tmp2.addAll(tmp1);
                } else {
                    if (tmp2.size() == 1) {
                        finalType = tmp2.get(0).getType();
                        stop = true;
                    }
                }
            }
            operationList.clear();
            operationList.addAll(tmp2);

            System.out.println("size: " + operationList.size());
            // check for flag
            if (operationList.size() == 0) {
                stop = true;
                if (finalType.equals("illegal")) {
                    semanticErrorsList.add("Expression <strong>" + ctx + "</strong> " +
                            "is illegal.");
                }
                System.out.println("type is " + finalType);
            }
        }
        System.out.println("printing after while " + operationList.toString());
        return finalType;
    }

    @Override
    public void exitStatement(decafParser.StatementContext ctx) {
        String typeOf = "";
        if (ctx.methodCall() != null) {
            System.out.println("is method from stmt");
        } else {
            typeOf = getTypeOfExpression(ctx.getText());
        }

        System.out.println("printing statement from exitStatement " + ctx.getText());

        if (ctx.location() != null) {
            if (ctx.location().location() != null) {
                decafParser.LocationContext primeLocation = ctx.location();
                decafParser.LocationContext lastLocation = ctx.location().location();
                boolean stopIterationOnLocations = false;

                while (!stopIterationOnLocations) {
                    if (lastLocation.location() != null) {
                        primeLocation = lastLocation;
                        decafParser.LocationContext tmpLocation = lastLocation.location();
                        lastLocation = tmpLocation;
                    } else {
                        stopIterationOnLocations = true;
                    }
                }

                String primeLocId = primeLocation.getText();
                String[] primeLocSplits = primeLocId.split("\\.");
                primeLocId = primeLocSplits[0];
                String structType = "";

                for (VarElement ve : varList) {
                    if (ve.getID().equals(primeLocId)) {
                        structType = ve.getVarType();
                    }
                }

                List<VarElement> primeLocationVars = new LinkedList<>();

                for (VarElement ve : varList) {
                    if (ve.getContext().getFirm().equals(structType)) {
                        primeLocationVars.add(ve);
                    }
                }

                for (VarElement ve : primeLocationVars) {
                    if (ve.getID().equals(lastLocation.getText())) {
                        if (ve.getVarType().equals(typeOf)) {
                            System.out.println("types matched");
                        } else if (!ve.getVarType().equals(typeOf)) {
                            if (typeOf != null) {
                                if (!typeOf.equals("")) {
                                    System.out.println("types didnt match");
//                                    semanticErrorsList.add("Types don't match at <strong>" + ctx.getText() + "</strong> <i>" +
//                                            ve.getVarType() + "</i> is not equal to </i><i>" + typeOf + "</i>");
                                }
                            }
                        }
                    }
                }
            } else {
                for (VarElement ve : varList) {
                    if (ve.getID().equals(ctx.location().getText())) {
                        if (ve.getVarType().equals(typeOf)) {
                            System.out.println("types matched");
                        } else if (!ve.getVarType().equals(typeOf)) {
                            if (typeOf != null) {
                                if (!typeOf.equals("")) {
                                    System.out.println("types didnt match");
//                                    semanticErrorsList.add("Types don't match at <strong>" + ctx.getText() + "</strong> <i>" +
//                                            ve.getVarType() + "</i> is not equal to </i><i>" + typeOf + "</i>");
                                }
                            }
                        }
                    }
                }
            }
        }


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
//        for (int i = 0; i < operation.length(); i++) {
//            if (arithOperatorsList.contains(Character.toString(operation.charAt(i)))) {
//                operators++;
//            }
//        }
        String[] splits = operation.split("\\+|-|\\*|/|%|&&|\\|\\||==|!=|<|<=|>|>=");
        operators = splits.length - 1;
        return operators;
    }

    @Override
    public void enterBlock(decafParser.BlockContext ctx) {
        if (ctx.varDeclaration() != null || ctx.statement() != null) {

        }
    }

    @Override
    public void enterStatement(decafParser.StatementContext ctx) {
//        if (!currentMethodContext.getType().equals("void")) {
//            String txt = ctx.getText();
//            if (!ctx.getText().contains("return")) {
//                semanticErrorsList.add("Method is not returning " + currentMethodContext.getType() + " type.");
//            }
//        }

        // TODO check var = LCall for TAC
        if (ctx.location() != null && ctx.expression().methodCall() != null) {
            methodCallLocation = ctx.location().getText();
        }

        if (ctx.getText().contains("return")) {
            String[] lineSplit = ctx.getText().replace(";", "").split("return");
            String returnVal = lineSplit[lineSplit.length-1];

            if (returnVal.matches("[(](.)*[)]")) {
                String rv = returnVal.replace("(", "").replace(")", "");
                returnVal = rv;
            }

            System.out.println("the return expression is: " + returnVal);

            if (ctx.expressionA() != null) {
                if (ctx.expressionA().expression() != null) {
                    System.out.println("LA EXPRESION EN RETURN ES " + ctx.expressionA().expression().getText());
                    operateExpression(ctx.expressionA().expression());
                    // check if it is a method call
                    String opType = "";
                    if (returnVal.contains("(")) {
                        String[] split = returnVal.split("\\(");
                        for (MethodElement me : methodFirms) {
                            if (me.getFirm().equals(split[0])) {
                                opType = me.getType();
                                break;
                            }
                        }
                    } else {
                        try {
                            int returnInt = Integer.parseInt(returnVal);
                            opType = "int";
                        } catch (Exception e) {
                            if (returnVal.matches("'.'")) {
                                opType = "char";
                            } else {
                                opType = getTypeOfExpression(ctx.getText());
                            }
                        }
                    }

                    if (!opType.equals(currentMethodContext.getType())) {
                        semanticErrorsList.add("Method <strong>" + currentMethodContext.getFirm() + "</strong> " +
                                "is returning an invalid type. <br>Expecting <strong>" + currentMethodContext.getType() + "" +
                                "</strong>, got <strong>"
                                + opType + "</strong>");
                    }
                }
            }

            if (currentMethodContext.getType().equals("void")) {
                // should not return nothing
                semanticErrorsList.add("Method <strong>" + currentMethodContext.getFirm() + "</strong> " +
                        "declared as void. Invalid return statement.");
            }

            // TODO return TAC
            returnTAC = tacIndent + "Return " + getCurrentTemp() + ";";
        }

        // location = expression type
        if (ctx.location() != null && ctx.expression() != null) {
            if (!ctx.location().getText().equals(currentLocation)) {
                // this means there is a new location, so make the operations related to temporaries
                assignTemporals();
                writeAssignTAC();
                currentLocation = ctx.location().getText();
            }

            String txt = ctx.location().getText();
            if (ctx.location().getText().matches("(.)*(\\[(.)*\\])")) {
                // it is refering to an array[NUM] = something
                if (!ctx.location().getText().matches("(.)*(\\[([0-9])*\\])")) {
                    semanticErrorsList.add(ctx.location().getText() + " is not of type int.");
                }
            }
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
                if (ctx.expression() != null) {
                    System.out.println("operating " + ctx.expression().getText());
                    System.out.println("calculating type " + ctx.getText());
//                    operateExpression(ctx.expression());
//                    String opType = getTypeOfExpression(ctx);
//                    System.out.println("type of expression from location = expression, location: " + ctx.location().getText()
//                            + ctx.expression().getText() + " is: " + opType);
                }
            }
        }

        // if and while
        if (ctx.getText().contains("if(") || ctx.getText().contains("while(")){
            System.out.println("found if or while statement");


            // TAC
            if (ctx.getText().contains("if(")) {
                operateExpression(ctx.expression());
                evaluatedExpressions.add(ctx.expression());

                if (ctx.expression().expression() != null) {
                    for (decafParser.ExpressionContext e : ctx.expression().expression()) {
                        evaluatedExpressions.add(e);
                    }
                }

                String opType = getTypeOfExpression(ctx.getText());
                System.out.println("type inside if or while is " + opType);
                if (!opType.equals("boolean")) {
                    semanticErrorsList.add("Expression <strong>" + ctx.expression().getText() + "</strong> is not " +
                            "of type boolean. Found type <i>" + opType + "</i>.");
                }

                assignTemporals();
                writeAssignTAC();

                List<decafParser.BlockContext> blocks = ctx.block();
                boolean hasElse = false;

                // if context has more than one block, it means that it has an else part,
                // so we'll flag this for later
                if (blocks.size() > 1) {
                    hasElse = true;
                }

                /* Write to TAC file */
                String branchVar1 = "_L"+String.valueOf(branchVariablesCount);
                branchVariablesCount++;
                branchVariables.add(branchVar1);
                String branchVar2 = "_L"+String.valueOf(branchVariablesCount);
                branchVariables.add(branchVar2);
                branchVariablesCount++;

                // code for condition
                writeToTACFile(tacIndent + "Ifz " + getPreviousTemp() + " Goto " + branchVar1 + ";");
                // code of succesfull if
                //writeToTACFile(tacIndent + "// TAC for success");
                List<decafParser.StatementContext> successStatements = blocks.get(0).statement();
                for (decafParser.StatementContext s : successStatements) {
                    if (!s.start.getText().equals(";")) {
                        currentLocation = s.start.getText();
                        operateExpression(s.expression());
                        assignTemporals();
                        writeAssignTAC();
                        evaluatedExpressions.add(s.expression());
                        currentLocation = "_top_null";
                    }
                }

                writeToTACFile(tacIndent + "Goto " + branchVar2 + ";");
                writeToTACFile("\n" + tacIndent + branchVar1 + ":");

                // else code
                //writeToTACFile(tacIndent + "// TAC for fail (else)");
                if (hasElse) {
                    successStatements = blocks.get(1).statement();
                    for (decafParser.StatementContext s : successStatements) {
                        if (!s.start.getText().equals(";")) {
                            currentLocation = s.start.getText();
                            operateExpression(s.expression());
                            assignTemporals();
                            writeAssignTAC();
                            evaluatedExpressions.add(s.expression());
                            currentLocation = "_top_null";
                        }
                    }
                }
                writeToTACFile("\n" + tacIndent + branchVar2 + ":");
            } else if (ctx.getText().contains("while(")) {

                String branchVar1 = "_L"+String.valueOf(branchVariablesCount);
                branchVariablesCount++;
                branchVariables.add(branchVar1);
                String branchVar2 = "_L"+String.valueOf(branchVariablesCount);
                branchVariables.add(branchVar2);
                branchVariablesCount++;

                if (!tmpOpList.isEmpty()) {
                    assignTemporals();
                    writeAssignTAC();
                    currentLocation = getNextTemp();
                }

                writeToTACFile("\n" + tacIndent + "StartWhile:");
                writeToTACFile("\n" + tacIndent + branchVar1 + ":");

                operateExpression(ctx.expression());
                evaluatedExpressions.add(ctx.expression());

                if (ctx.expression().expression() != null) {
                    for (decafParser.ExpressionContext e : ctx.expression().expression()) {
                        evaluatedExpressions.add(e);
                    }
                }

                String opType = getTypeOfExpression(ctx.getText());
                System.out.println("type inside if or while is " + opType);
                if (!opType.equals("boolean")) {
                    semanticErrorsList.add("Expression <strong>" + ctx.expression().getText() + "</strong> is not " +
                            "of type boolean. Found type <i>" + opType + "</i>.");
                }

                assignTemporals();
                writeAssignTAC();

                /* Write to TAC file */
                // TODO place code for condition
                //writeToTACFile(tacIndent +"// condition code here");
                writeToTACFile(tacIndent + "Ifz " + getPreviousTemp() + " Goto " + branchVar2 + ";");
                // TODO place code inside while here
                //writeToTACFile(tacIndent +"// while code here");
                List<decafParser.BlockContext> blocks = ctx.block();
                List<decafParser.StatementContext> successStatements = blocks.get(0).statement();
                for (decafParser.StatementContext s : successStatements) {
                    if (!s.start.getText().equals(";")) {
                        currentLocation = s.start.getText();
                        operateExpression(s.expression());
                        assignTemporals();
                        writeAssignTAC();
                        evaluatedExpressions.add(s.expression());
                        currentLocation = "_top_null";
                    }
                }

                writeToTACFile(tacIndent + "Goto " + branchVar1 + ":");
                writeToTACFile("\n" + tacIndent + branchVar2 + ":");
            }
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

        // we have an array
        if (ctx.getText().matches("(.)*(\\[[0-9]\\])")) {
            // lets add the expression inside the array to the evaluated list so algorithm does not confuse
            if (ctx.expression() != null) {
                decafParser.ExpressionContext ec = ctx.expression();
                evaluatedExpressions.add(ec);
            }
        }

        // verify if ID exits. If not, the location is invalid, since the variable does not exist.
        boolean isLocationDefined = false;
        // location can be a variable
        for (VarElement listVar : varList) {
            if ((listVar.getID().equals(ID)) && (listVar.getContext().equals(currentMethodContext))) {
                // the location exists
                isLocationDefined = true;
                break;
            } else if ((listVar.getID().equals(ID)) && (
                    (listVar.getContext().getFirm().equals("global")) || (listVar.getContext().getType().equals("struct"))
                    )) {
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
        boolean isStruct = false;

        if (ctx.varType() != null) {
            // check if varDeclaration is made with a struct type
            if (ctx.varType().ID() != null) {
                isStruct = true;
                varType = ctx.varType().ID().getText();
            } else {
                varType = ctx.varType().getText();
            }
        }

        if (ctx.ID() != null) {
            ID = ctx.ID().getText();
        }

        if (ctx.NUM() != null) {
            num = ctx.NUM().getText();
        }

        // Check if variable has already been declared in the same context
        VarElement newVar = new VarElement(varType, ID, currentMethodContext);
        newVar.setStruct(isStruct);

        if (newVar.getContext().getType().equals("struct")) {
            newVar.setPositionInStruct(positionInStructCount);
            positionInStructCount++;
        }

        if (!num.equals("")) {
            try {
                int numInt = Integer.parseInt(num);
                if (!num.equals("")) {
                    newVar.setNUM(num);
                    if (numInt == 0) {
                        semanticErrorsList.add("<strong>\"" + ID + "\"</strong> length " +
                                "has to be bigger than 0.");
                    }
                }
            } catch (Exception e){
                semanticErrorsList.add("<strong>\"" + ID + "\"</strong> length " +
                        "is not of type int");
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

        // check if method is one of reserved methods
        if (firm.equals("print")) {
            //writeToTACSignal = true;

            writeToTACFile(tacIndent + "PushParam " + ctx.arg1().arg2().arg().get(0).getText() + ";");
            writeToTACFile(tacIndent + "LCall " + firm + ";\n" + tacIndent + "PopParams 1;");

            //writeToTACSignal = false;
        } else {
            // verify if method call is valid, this means if the method has been correctly declared before
            boolean isMethodDeclared = false;
            List<decafParser.ArgContext> argList = ctx.arg1().arg2().arg();
            for (MethodElement listMethod : methodFirms) {
                if (listMethod.getFirm().equals(firm)) {
                    if (argList.size() != listMethod.getArgs().size()) {
                        semanticErrorsList.add("Method call for <strong>" + firm + "</strong> has more parameters <br> than the method definition");
                    }

                    isMethodDeclared = true;
                    // verify that arguments match the type of the parameters in method
                    int i = 0;
                    for (decafParser.ParameterContext pc : listMethod.getArgs()) {
                        System.out.println("PARAMETER TYPE IS: " + pc.parameterType().getText());
                        System.out.println("ARG at i = " + i + " is: " + argList.get(i).getText());

                        String argType = "";
                        for (VarElement ve : varList) {
                            if (ve.getID().equals(argList.get(i).getText())) {
                                argType = ve.getVarType();
                            }
                        }

                        try {
                            decafParser.ExpressionContext eCtx = argList.get(i).expression();
                            String thing = eCtx.getText();
                            operateExpression(eCtx);
                            argType = getTypeOfExpression(eCtx.getText());
                            if (argType == null) {
                                argType = "";
                            }
                        } catch (Exception e) {
                            argType = "";
                        }

                        if (argType.equals("")) {
                            String argument = argList.get(i).getText();
                            System.out.println(argument);

                            try {
                                int parsedArg = Integer.parseInt(argument);
                                argType = "int";
                            } catch (Exception e) {
                                if (argument.equals("true") || argument.equals("false")) {
                                    argType = "boolean";
                                } else if (argument.matches("'.'")) {
                                    argType = "char";
                                }
                            }
                        }

                        if (!pc.parameterType().getText().equals(argType)) {
                            semanticErrorsList.add("Parameter " + (i+1) + " type <strong>" + pc.parameterType().getText()
                                    + "</strong> in " + firm + " does not match <br>argument type <strong>" + argType
                                    + "</strong> in method call.");
                        }
                        i++;
                    }
                    break;
                }
            }

            for (decafParser.ArgContext arg : argList) {
                tmpOpList.add(new Operation(arg.getText()));
                evaluatedExpressions.add(arg.expression());
            }

            // TAC Generation
            List<String> argTmps = new LinkedList<>();
            if (!tmpOpList.isEmpty()) {
                // TODO remove duplicates from tmpOpList
                removeTmpOpListDuplicates();

                String currentTemp = getCurrentTemp();

                for (Operation op : tmpOpList) {
                    argTmps.add(getCurrentTemp());
                    getNextTemp();
                }

                setCurrentTemp(currentTemp);

                assignTemporals();
                writeAssignTAC();
                //argTmps.add(getPreviousTemp()); // TODO check this
                currentLocation = getNextTemp();
            }

            System.out.println("evaluatedExpressions has: " + evaluatedExpressions.toString());

//        String paramList = "";
//
//        for (String arg : argTmps) {
//            paramList = paramList + arg + ", ";
//        }
//
//        paramList = paramList.substring(0, paramList.length()-2);
//
//        writeToTACFile(tacIndent + "PushParam " + paramList + ";");

            for (String arg : argTmps) {
                writeToTACFile(tacIndent + "PushParam " + arg + ";");
            }

            if (methodCallLocation != null) {
                writeToTACFile(tacIndent + methodCallLocation + " = LCall _" + firm + ";");
                methodCallLocation = null;
            } else {
                writeToTACFile(tacIndent + "LCall _" + firm + ";");
            }

            popParamsSize = argTmps.size() * 4;
            writeToTACFile(tacIndent + "PopParams " + popParamsSize + ";");
            popParamsSize = 0;

            if (!isMethodDeclared) { semanticErrorsList.add("Can't make call to undeclared method " + firm); }
        }
    }

    @Override
    public void enterStructDeclaration(decafParser.StructDeclarationContext ctx) {
        String firm = "";
        List<decafParser.ParameterContext> args = new LinkedList<>();

        if (ctx.ID() != null) {
            firm = ctx.ID().getText();
        }

        MethodElement newMethod = new MethodElement("struct", firm, args);
        currentMethodContext = newMethod;
        //System.out.println("nuevo metodo " + firm);
        //System.out.println(methodFirms.toString());

        boolean foundMethod = false;

        for (MethodElement listMethod : methodFirms) {
            if (listMethod.equals(newMethod)) {
                semanticErrorsList.add("Struct " + firm + " has " +
                        "already been declared.");
                foundMethod = true;
                break;
            }
        }

        if (!foundMethod) {
            methodFirms.add(newMethod);
        }

        // now check with vars
        VarElement newVar = new VarElement("struct", firm, newMethod);
        boolean foundVar = false;

        for (VarElement ve : varList) {
            if (ve.equals(newVar)) {
                semanticErrorsList.add("Struct " + firm + " has " +
                        "already been declared.");
                foundVar = true;
                break;
            }
        }

        if (!foundVar) {
            varList.add(newVar);
        }
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
                /* Write to TAC file */
                writeToTACFile(tacIndent + firm + ":");
                tacIndent = tacIndent + "\t";
                writeToTACFile(tacIndent + "BeginFunc N;");
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

        if (!type.equals("void")) {
            if (ctx.block() != null) {
                String txt = ctx.block().getText();
                if (!ctx.block().getText().contains("return")) {
                    semanticErrorsList.add("Method " + firm + " has no return expression.");
                }
            } else {
                semanticErrorsList.add("Method " + firm + " has no return expression.");
            }
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
                /* Write to TAC file */
                writeToTACFile(tacIndent + "_" + firm + ":");
                tacIndent = tacIndent + "\t";
                writeToTACFile(tacIndent + "BeginFunc N;");
            }

//            if (!methodFirms.contains(newMethod)) { methodFirms.add(newMethod); } else {
//                semanticErrorsList.add("Method " + firm + " with arguments: " + args.toString() + " has " +
//                        "already been declared.");
//            }
        }
    }

    @Override
    public void exitMethodDeclaration(decafParser.MethodDeclarationContext ctx) {
        assignTemporals();
        if (!tempVarsValues.isEmpty()) {
            writeAssignTAC();
        }

        // change write signal to true to write to file
        writeToTACSignal = true;

        int N = 0;
        for (String str : outTAC) {
            if (str.contains("_t")) {
                N++;
            }
        }

        for (String str : outTAC) {
            if (str.contains("BeginFunc N;")) {
                writeToTACFile(tacIndent + "BeginFunc " + String.valueOf(4*N) + ";");
            } else {
                writeToTACFile(str);
            }
        }

        if (returnTAC != null) {
            writeToTACFile(returnTAC);
            returnTAC = null;
        }

        writeToTACFile(tacIndent + "EndFunc;");
        writeToTACSignal = false;
        outTAC.clear();
        tacIndent = tacIndent.substring(0, tacIndent.length()-1);
        this.tempVarsCount = 0;                 // reset counter
        evaluatedExpressions.clear();
        currentLocation = "_top_null";
        positionInStructCount = 0;
    }

    public List<String> getSemanticErrorsList() {
        // existance of 'main' method
        if (!foundMain) { semanticErrorsList.add("No 'main' method declared."); }

        return semanticErrorsList;
    }

    private void writeToTACFile(String line) {
        if (writeToTACSignal) {
            try(FileWriter fw = new FileWriter("decaf.tac", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw))
            {
                out.println(line);
            } catch (IOException e) {
                //exception handling left as an exercise for the reader
            }
        } else {
            outTAC.add(line);
        }
    }

    /**
     * Alters temporal variables count (+1)
     * @return the next temporary variable available
     */
    private String getNextTemp() {
        String tempVar = "_t" + String.valueOf(tempVarsCount);
        tempVarsCount++;
        return tempVar;
    }

    private String getPreviousTemp() {
        return "_t" + String.valueOf(tempVarsCount-1);
    }

    private String getCurrentTemp() {
        return "_t" + String.valueOf(tempVarsCount);
    }

    private void setCurrentTemp(String tmp) {
        int tmpVal = Integer.parseInt(Character.toString(tmp.charAt(tmp.length()-1)));
        tempVarsCount = tmpVal;
    }

    private void assignTemporals() {
        // Verify that tempVarsValues is empty
        if (tempVarsValues.size() == 0) {
            // Assign each operation a temporal variable
            for (Operation o : tmpOpList) {
                String tempToAdd = "";
                if (tempVarsValues.size() == 0) {
                    tempToAdd = "_top_assign";
                } else {
                    tempToAdd = getNextTemp();
                }
                tempVarsValues.put(tempToAdd, o.getOperation());
            }

            // Replace expressions with temporal variables inside other expressions
            HashMap<String, String> tmpTempVarsValues = new HashMap<>();
            tmpTempVarsValues.putAll(tempVarsValues);
            final Set<String> expressions = tempVarsValues.keySet();
            final Set<String> expressionsCopy = expressions;

            for (String e : expressions) {
                for (String ec : expressionsCopy) {
                    if (!tempVarsValues.get(e).equals(tempVarsValues.get(ec))) {
                        if (tempVarsValues.get(e).contains(tempVarsValues.get(ec))) {
                            String tmpVal = tmpTempVarsValues.get(ec);
                            String newExpression = tmpTempVarsValues.get(e).replace(tmpVal, ec);
                            tmpTempVarsValues.replace(e, newExpression);
                        }
                    }
                }
            }
            tempVarsValues = tmpTempVarsValues;
        }
    }

    private void resetTemporalOperations() {
        this.tempVarsValues = new HashMap<>();  // reset HashMap to map expressions with temp vars
        this.tmpOpList.clear();
    }

    private void writeAssignTAC() {
        // TODO order tempVarsValues so that _top_assign has first position
        if (!tempVarsValues.isEmpty()) {
            int t = tempVarsValues.size();
            Queue<String> expsInOrder = new LinkedList<>();
            expsInOrder.add("_top_assign");
            String value = tempVarsValues.get("_top_assign");
            tempVarsValues.remove("_top_assign");
            Set<String> expressions = tempVarsValues.keySet();

            for (String e : expressions) {
                expsInOrder.add(e);
            }

            tempVarsValues.put("_top_assign", value);

            List<String> out = new LinkedList<>();
            List<String> toWriteList = new LinkedList<>();

            // Check if currentLocation is an array index, if so change it to TAC
            boolean currLocIsArr = false;
            String arrLoc = "";

            boolean currLocIsStruct = false;
            String structLoc = "";

            if (currentLocation.matches("(.)*(\\[([0-9])\\])")) {
                currLocIsArr = true;
                String[] currLocSplit = currentLocation.split("\\[");

                for (int i = 0; i < currentLocation.length(); i++) {
                    String index = "";
                    if (Character.toString(currentLocation.charAt(i)).equals("[")) {
                        index = Character.toString(currentLocation.charAt(i+1));
                        String tmp1 = getNextTemp();
                        String tmp2 = getNextTemp();
                        String tmp3 = getNextTemp();
                        String tmp4 = getNextTemp();
                        String tmp5 = getNextTemp();
                        writeToTACFile(tacIndent + tmp1 + " = " + index + ";");
                        writeToTACFile(tacIndent + tmp2 + " = 4;");
                        writeToTACFile(tacIndent + tmp3 + " = " + tmp1 + "*" + tmp2 + ";");
                        writeToTACFile(tacIndent + tmp4 + " = " + currLocSplit[0] + "+" + tmp3 + ";");
                        writeToTACFile(tacIndent + tmp5 + " = " + "*(" + tmp4 + ");");
                        arrLoc = tmp5;
                        currentLocation = getNextTemp();
                        break;
                    }
                }
            } else if (currentLocation.matches("((.)*\\.(.)*)+")) {
                // SCall
                writeToTACFile(tacIndent + "SCall " + currentLocation);
                // has struct structure
                currLocIsStruct = true;

                String[] structLocationSplit = currentLocation.split("\\.");
                String[] orderedSplit = new String[structLocationSplit.length];

                int q = 0;
                for (int k = structLocationSplit.length-1; k>=0; k--) {
                    orderedSplit[q] = structLocationSplit[k];
                    q++;
                }

                // orderedSplit has vars in order
                // TODO struct TAC
                for (int g = 0; g < orderedSplit.length; g++) {
                    String var = orderedSplit[g];

                    if (g < orderedSplit.length-1) {
                        String prevVar = orderedSplit[g+1];

                        // search varList for prevVar to see the struct
                        // TODO check
                        boolean keepSearching = true;
                        int vePos = 0;
                        for (VarElement ve : varList) {
                            if (keepSearching) {
                                if (ve.getID().equals(var) && ve.getContext().getType().equals("struct")) {
                                    for (VarElement ve2 : varList) {
                                        if (ve2.getID().equals(prevVar) && ve.getContext().getFirm().equals(ve2.getVarType())) {
                                            // ve is the desired var
                                            // TODO check here
                                            vePos = ve2.getPositionInStruct();
                                            keepSearching = false;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        String tmp1 = getNextTemp();
                        String tmp2 = getNextTemp();
                        String tmp3 = getNextTemp();
                        String tmp4 = getNextTemp();
                        String tmp5 = getNextTemp();
                        writeToTACFile(tacIndent + tmp1 + " = " + vePos + ";");
                        writeToTACFile(tacIndent + tmp2 + " = 4;");
                        writeToTACFile(tacIndent + tmp3 + " = " + tmp1 + "*" + tmp2 + ";");
                        writeToTACFile(tacIndent + tmp4 + " = " + prevVar + "+" + tmp3 + ";");
                        writeToTACFile(tacIndent + tmp5 + " = " + "*(" + tmp4 + ");");
                        arrLoc = tmp5;
                        currentLocation = getNextTemp();
                    }
                }
            }

            // write all lines
            for (int j = 0; j < t; j++) {
                String e = expsInOrder.poll();
                String exp = e;                     // expression
                String var = tempVarsValues.get(e); // tmp var of expression

                if (!currentLocation.equals("_top_null")) {
                    if (exp.equals("_top_assign")) exp = currentLocation;
                } else {
                    if (exp.equals("_top_assign")) exp = getNextTemp();
                }

                // check if expression has an array inside
                String arrTmp = "";
                if (var.matches("(.)*(\\[([0-9])\\])(.)*")) {
                    // it does!
                    // so split by possible operator
                    String[] expSplit = var.split("\\+|-|\\*|/|%|&&|\\|\\||==|!=|<|<=|>|>=");

                    String operator = "";

                    if (var.contains("+")) { operator =  "+"; }
                    if (var.contains("-")) { operator =  "-"; }
                    if (var.contains("/")) { operator =  "/"; }
                    if (var.contains("*")) { operator =  "*"; }
                    if (var.contains("%")) { operator =  "%"; }
                    if (var.contains("&&")) { operator =  "&&"; }
                    if (var.contains("||")) { operator =  "||"; }
                    if (var.contains("==")) { operator =  "=="; }
                    if (var.contains("!=")) { operator =  "!="; }
                    if (var.contains("<")) { operator =  "<"; }
                    if (var.contains("<=")) { operator =  "<="; }
                    if (var.contains(">")) { operator =  ">"; }
                    if (var.contains(">=")) { operator =  ">="; }

                    //  expSplit[0] has the whole array call
                    String[] currLocSplit = expSplit[0].split("\\[");

                    for (int i = 0; i < expSplit[0].length(); i++) {
                        String index = "";
                        if (Character.toString(expSplit[0].charAt(i)).equals("[")) {
                            index = Character.toString(expSplit[0].charAt(i+1));
                            String tmp1 = getNextTemp();
                            String tmp2 = getNextTemp();
                            String tmp3 = getNextTemp();
                            String tmp4 = getNextTemp();
                            String tmp5 = getNextTemp();
                            writeToTACFile(tacIndent + tmp1 + " = " + index + ";");
                            writeToTACFile(tacIndent + tmp2 + " = 4;");
                            writeToTACFile(tacIndent + tmp3 + " = " + tmp1 + "*" + tmp2 + ";");
                            writeToTACFile(tacIndent + tmp4 + " = " + currLocSplit[0] + "+" + tmp3 + ";");
                            writeToTACFile(tacIndent + tmp5 + " = " + "*(" + tmp4 + ");");
                            arrTmp = tmp5 + operator + expSplit[1];
                            break;
                        }
                    }

                }

                String toWrite = "";

                if (!arrTmp.equals("")) {
                    toWrite = tacIndent + exp + " = " + arrTmp + ";";
                } else {
                    toWrite = tacIndent + exp + " = " + var + ";";
                }

                // Check for equal expressions in both sides x = x
                String toVerify = toWrite.replace("\t", "").replace(" ", "")
                        .replace(";", "");
                String[] verSplits = toVerify.split("=");

                if (!verSplits[0].equals(verSplits[1])) {
                    out.add(toWrite);
                }


                //writeToTACFile(toWrite);
            }

            for (int i = out.size()-1; i >= 0; i--) {
                // checking for boolean expressions
                String wholeExpression = out.get(i);
                wholeExpression = wholeExpression.replace(" ", "");
                wholeExpression = wholeExpression.replace(";", "");

                // handle <=
                wholeExpression = wholeExpression.replace("<=", "leq&;");

                String[] expSplit = wholeExpression.split("=");
                String[] tmpExpSplit = new String[expSplit.length];

                // handle <=
                int k = 0;
                for (String str : expSplit) {
                    tmpExpSplit[k] = str.replace("leq&;", "<=");
                    k++;
                }

                expSplit = tmpExpSplit;

                // expSplit[0] has tempVar and expSplit[1] has expression
                // conditions to split expSplit[1]:
                // 1. a > b
                // 2. a <= b
                // 3. a >= b (check)
                if (expSplit[1].contains(">") || expSplit[1].contains("<=") || expSplit[1].contains(">=")) {
                    //case 1
                    if (expSplit[1].contains(">")) {
                        String[] secondSplit = expSplit[1].split(">");
                        String finalExpr = expSplit[0] + " = " + secondSplit[1] + "<" + secondSplit[0] + ";";
                        toWriteList.add(finalExpr);
                    } else if (expSplit[1].contains("<=")) {
                        String[] secondSplit = expSplit[1].split("<=");
                        String tmp1 = getNextTemp();
                        String tmp2 = getNextTemp();
                        String finalExp1 = tacIndent + tmp1 + " = " + secondSplit[0] + "<" + secondSplit[1] + ";";
                        String finalExp2 = tacIndent + tmp2 + " = " + secondSplit[0] + "==" + secondSplit[1] + ";";
                        String finalExp3 = expSplit[0] + " = " + tmp1 + "||" + tmp2 + ";";
                        toWriteList.add(finalExp1);
                        toWriteList.add(finalExp2);
                        toWriteList.add(finalExp3);
                    }
                } else {
                    toWriteList.add(out.get(i));
                }
            }

            for (String o : toWriteList) {
                writeToTACFile(o);
            }

            if (currLocIsArr) {
                writeToTACFile(tacIndent + arrLoc + " = " + currentLocation + ";");
            }

            if (currLocIsStruct) {
                writeToTACFile(tacIndent + arrLoc + " = " + currentLocation + ";");
                writeToTACFile(tacIndent + "End SCall;");
            }

            // reset temporaries
            resetTemporalOperations();
        }
    }

    private String[] splitter(String operation, String operator) {
        if (operator.equals("+") || operator.equals("*") || operator.equals("||")) {
            return operation.split("\\+|\\*|\\|\\|");
        } else {
            return operation.split(operator);
        }
    }

    private void removeTmpOpListDuplicates() {
        List<String> tmp = new LinkedList<>();

        for (Operation op : tmpOpList) {
            String in = op.getOperation();

            if (!tmp.contains(in)) {
                tmp.add(in);
            }
        }

        List<Operation> tmp2 = new LinkedList<>();

        for (String str : tmp) {
            tmp2.add(new Operation(str));
        }

        tmpOpList.clear();
        tmpOpList.addAll(tmp2);
    }

    public List<VarElement> getVarList() {
        return this.varList;
    }

    public List<MethodElement> getMethodFirms() {
        return methodFirms;
    }
}
