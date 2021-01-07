package com.josejo911.semanticControl;

import com.josejo911.antlr.decafParser;
import com.josejo911.antlrtac.tacBaseListener;
import com.josejo911.antlrtac.tacParser;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class TacSemanticListener extends tacBaseListener {
    private tacParser parser;
    private String currentIndent;
    private String currentContext;
    private final String dataIndent = "\t\t\t\t";

    private List<VarElement> varListFromSource;
    private List<MethodElement> methodListFromSource;
    private Stack<String> temporariesStack;
    private Stack<String> savedValuesStack;
    private Stack<String> argumentsStack;

    private String[] registersInUse;

    private List<tacParser.AssignStatementContext> assignments;
    private List<tacParser.IfStatementContext> ifs;
    private List<tacParser.WhileStatementContext> whiles;
    private List<decafParser.MethodCallContext> methodCalls;

    private List<String> pushedParams;

    public TacSemanticListener(tacParser parser, List<VarElement> varListFromSource, List<MethodElement> methodListFromSource) {
        this.parser = parser;
        this.varListFromSource = varListFromSource;
        this.methodListFromSource = methodListFromSource;
        this.currentIndent = "";
        this.currentContext = "main";

        this.temporariesStack = new Stack<>();
        this.savedValuesStack = new Stack<>();
        this.argumentsStack = new Stack<>();
        fillRegistersStack();

        registersInUse = new String[3];

        assignments = new LinkedList<>();
        ifs = new LinkedList<>();
        whiles = new LinkedList<>();
        methodCalls = new LinkedList<>();

        pushedParams = new LinkedList<>();
    }

    @Override
    public void enterProgram(tacParser.ProgramContext ctx) {
        // write headers of MIPS
        writeToMIPSFile(currentIndent + "# MIPS autogenerado.");
        writeToMIPSFile(currentIndent + "# IntelliJO IDEA, Implementacion de DECAF con Java, ANTLR4 y el framework de VAADIN");
        writeToMIPSFile(currentIndent + "\n");
        writeToMIPSFile(currentIndent + ".text");
        writeToMIPSFile(currentIndent + ".globl main");
    }

    @Override
    public void exitProgram(tacParser.ProgramContext ctx) {
        // write data section
        writeToMIPSFile(currentIndent + "# ---------- data section ----------");
        writeToMIPSFile(currentIndent + ".data");

        // traverse varList and write in data section
        for (VarElement ve : this.varListFromSource) {
            String methodName = ve.getContext().getFirm();
            String varName = ve.getID();
            String type = ve.getVarType();

            // write data
            writeDataInMemory(methodName, varName, type);
        }

        // re write file with expected order
        orderFile();
    }

    @Override
    public void enterMainDeclaration(tacParser.MainDeclarationContext ctx) {
        writeToMIPSFile("# ------------------------------------------------- StartMain ---------------------------------------------------------");
        this.currentContext = "main";                            // current context = main
        writeToMIPSFile(currentIndent + "main:");           // main method declaration
        incrementIndent();                                       // increment indent
    }

    @Override
    public void exitMainDeclaration(tacParser.MainDeclarationContext ctx) {
        writeToMIPSFile("\n");
        // write exit call
        writeToMIPSFile(currentIndent + "# ---------- Exit ----------\n" +
                currentIndent + "li $v0, 10\n" +
                currentIndent + "syscall");
        decrementIndent();                                       // decrement indent
        writeToMIPSFile(currentIndent + "\n");
        writeToMIPSFile("# -------------------------------------------------- EndMain ---------------------------------------------------------");
    }

    @Override
    public void enterMethodDeclaration(tacParser.MethodDeclarationContext ctx) {
        writeToMIPSFile("# -------------------------------------------------- StartProcedure ---------------------------------------------------");
        String methodName = ctx.location().getText().replace("_", "");
        this.currentContext = methodName;
        writeToMIPSFile(currentIndent + methodName + ":");
        incrementIndent();

        int NUM = Integer.parseInt(ctx.funcBlock().NUM().getText()) / 4;

        // push onto stack
        for (int i = 0; i < NUM; i++) {
            writeToMIPSFile(currentIndent + "addi $sp, $sp, -4\t\t\t\t# Adjust stack pointer");
            writeToMIPSFile(currentIndent + "sw $s" + String.valueOf(i) + ", 0($sp)\t\t\t\t\t# Save reg");
        }


    }

    @Override
    public void exitMethodDeclaration(tacParser.MethodDeclarationContext ctx) {
        int NUM = Integer.parseInt(ctx.funcBlock().NUM().getText()) / 4;
        // restore saved values
        for (int i = NUM; i > 0; i--) {
            writeToMIPSFile(currentIndent + "lw $s" + String.valueOf(i) + ", 0($sp)\t\t\t\t\t# Restore reg");
            writeToMIPSFile(currentIndent + "addi $sp, $sp, 4\t\t\t\t# Adjust stack pointer");
        }

        // return from function
        writeToMIPSFile(currentIndent + "jr $ra\t\t\t\t\t\t\t# Jump to addr stored in $ra");
        writeToMIPSFile("\n");
        decrementIndent();
        writeToMIPSFile("# -------------------------------------------------- EndProcedure -----------------------------------------------------");
    }

    @Override
    public void enterReturnExp(tacParser.ReturnExpContext ctx) {
        String returnVar = ctx.location().getText().replace("_", "$");
        //move $v0, returnVar
        writeToMIPSFile(currentIndent + "move $v0, " + returnVar + "\t\t\t\t\t# Return");
    }

    @Override
    public void enterMethodCall(tacParser.MethodCallContext ctx) {
        // get params
        List<tacParser.PushParamContext> params = ctx.pushParam();
        // get methodName
        String methodName = ctx.location().getText().replace("_", "");

        // check if method is one special method, like print
        if (methodName.equals("print")) {
            writeToMIPSFile(currentIndent + "li " + "$v0" + ", " + "1\t\t\t\t\t# Print call");
            writeToMIPSFile(currentIndent + "lw $a0, " + params.get(0).location().getText() + "_" + currentContext);
            writeToMIPSFile(currentIndent + "syscall");
        } else {
            // load params in registers
            for (tacParser.PushParamContext param : params) {
                String argRegister = getNextAvailableAR();

                if (!argRegister.equals("$err")) {
                    String newParam = param.location().getText().replace("_", "$");
                    pushedParams.add(argRegister);
                    writeToMIPSFile(currentIndent + "move " + argRegister + ", " + newParam);
                }
            }

            List<decafParser.ParameterContext> paramsInSymbolTable = new LinkedList<>();

            for (MethodElement me : this.methodListFromSource) {
                if (me.getFirm().equals(methodName)) {
                    paramsInSymbolTable.addAll(me.getArgs());
                    break;
                }
            }

            int index = 0;
            for (decafParser.ParameterContext param : paramsInSymbolTable) {
                String varName = param.ID().getText() + "_" + methodName;
                writeToMIPSFile(currentIndent + "sw " + pushedParams.get(index) + ", " + varName);
                index = index + 1;
            }

            // jump to function
            writeToMIPSFile(currentIndent + "jal " + methodName);

            // load returned value
            String returnReg = getNextAvailableSVR();
            writeToMIPSFile(currentIndent + "move " + returnReg + ", " + "$v0");

            //pushedParams.clear();
        }
    }

    @Override
    public void enterWhileStatement(tacParser.WhileStatementContext ctx) {
        System.out.println("Entering while stmt");
        if (!whiles.contains(ctx)) {
            // get objects
            List<tacParser.LabelContext> lbls = ctx.label();
            String lbl1 = lbls.get(0).getText().replace(":", "");
            String lbl2 = lbls.get(1).getText().replace(":", "");
            tacParser.AssignStatementContext assignStmt = ctx.assignStatement();
            List<tacParser.LocationContext> locations = ctx.location();
            String temp = locations.get(0).getText();
            tacParser.BlockContext falseBlock = ctx.block();
            temp = temp.replace("_", "$");

            // handle assignment before if
            writeToMIPSFile(currentIndent + lbl1 + ":");
            enterAssignStatement(assignStmt);
            assignments.add(assignStmt);

            // write MIPS branch
            writeToMIPSFile(currentIndent + "blez " + temp + ", " + lbl2);

            // TODO handle false block
            List<tacParser.StatementContext> fbStatements = falseBlock.statement();
            for (tacParser.StatementContext stmt : fbStatements) {
                if (stmt.assignStatement() != null) {
                    enterAssignStatement(stmt.assignStatement());
                    if (!assignments.contains(stmt.assignStatement())){
                        assignments.add(stmt.assignStatement());
                    }
                } else if (stmt.ifStatement() != null) {
                    enterIfStatement(stmt.ifStatement());
                    if (!ifs.contains(stmt.ifStatement())) {
                        ifs.add(stmt.ifStatement());
                    }
                } else if (stmt.whileStatement() != null) {
                    enterWhileStatement(stmt.whileStatement());
                    if (!whiles.contains(stmt.whileStatement())) {
                        whiles.add(stmt.whileStatement());
                    }
                }
            }

            // branch to label 1
            writeToMIPSFile(currentIndent + "b " + lbl1);

            // write label 2
            writeToMIPSFile(currentIndent + lbl2 + ":");

            whiles.add(ctx);

        }
    }

    @Override
    public void exitWhileStatement(tacParser.WhileStatementContext ctx) {
        fillRegistersStack();
    }

    @Override
    public void enterIfStatement(tacParser.IfStatementContext ctx) {
        if (!ifs.contains(ctx)) {
            // get involved variables and labels
            List<tacParser.LocationContext> locations = ctx.location();
            String temp = locations.get(0).getText();
            String lbl1 = locations.get(1).getText();
            String lbl2 = locations.get(2).getText();

            temp = temp.replace("_", "$");

            // write MIPS branch
            writeToMIPSFile(currentIndent + "blez " + temp + ", " + lbl1);

            // get blocks
            List<tacParser.BlockContext> blocks = ctx.block();
            tacParser.BlockContext falseBlock = blocks.get(0);
            tacParser.BlockContext trueBlock = blocks.get(1);

            // TODO handle false block
            List<tacParser.StatementContext> fbStatements = falseBlock.statement();
            for (tacParser.StatementContext stmt : fbStatements) {
                if (stmt.assignStatement() != null) {
                    enterAssignStatement(stmt.assignStatement());
                    if (!assignments.contains(stmt.assignStatement())){
                        assignments.add(stmt.assignStatement());
                    }
                } else if (stmt.ifStatement() != null) {
                    enterIfStatement(stmt.ifStatement());
                    if (!ifs.contains(stmt.ifStatement())) {
                        ifs.add(stmt.ifStatement());
                    }
                } else if (stmt.whileStatement() != null) {
                    enterWhileStatement(stmt.whileStatement());
                    if (!whiles.contains(stmt.whileStatement())) {
                        whiles.add(stmt.whileStatement());
                    }
                }
            }

            // make jump to label 2
            writeToMIPSFile(currentIndent + "b " + lbl2);

            // label 1
            writeToMIPSFile(currentIndent + lbl1 + ":");

            // TODO handle true block
            List<tacParser.StatementContext> tbStatements = trueBlock.statement();
            for (tacParser.StatementContext stmt : tbStatements) {
                if (stmt.assignStatement() != null) {
                    enterAssignStatement(stmt.assignStatement());
                    if (!assignments.contains(stmt.assignStatement())){
                        assignments.add(stmt.assignStatement());
                    }
                } else if (stmt.ifStatement() != null) {
                    enterIfStatement(stmt.ifStatement());
                    if (!ifs.contains(stmt.ifStatement())) {
                        ifs.add(stmt.ifStatement());
                    }
                } else if (stmt.whileStatement() != null) {
                    enterWhileStatement(stmt.whileStatement());
                    if (!whiles.contains(stmt.whileStatement())) {
                        whiles.add(stmt.whileStatement());
                    }
                }
            }

            // label 2
            writeToMIPSFile(currentIndent + lbl2 + ":");

            // add if context to list
            ifs.add(ctx);
        }
    }

    @Override
    public void enterAssignStatement(tacParser.AssignStatementContext ctx) {
        boolean isEqExpr = false;
        if (ctx.location().getText().equals(ctx.expression().getText()))
            isEqExpr = true;

        if (!assignments.contains(ctx) && !isEqExpr) {
            // separate location and expression
            tacParser.LocationContext location = ctx.location();
            tacParser.ExpressionContext expression = ctx.expression();

            String locRegister = "";
            String locRegisterData = "";
            // check if location has a termprary register in it
            if (location.getText().contains("_t")) {
                locRegister = location.getText().replace("_t", "$t");
                temporariesStack.remove(locRegister);                       // remove register from available registers
            } else {
                locRegister = getNextAvailableSVR();
                locRegisterData = location.getText() + "_" + currentContext;
            }

            // set assign register
            registersInUse[0] = locRegister;

            // get the operator for expression
            String operator = getOperatorFromExpression(expression);
            if (!operator.equals("$err_no_op")) {
                // expression has two operands
                // separate the operands
                String[] operands = splitTac(expression.getText());

                int i = 1;
                for (String operand : operands) {
                    if (operand.contains("_t")) {
                        registersInUse[i] = operand.replace("_t", "$t");
                    } else {
                        try{
                            // check if its not an immediate number
                            int immediate = Integer.parseInt(operand);
                            registersInUse[i] = getNextAvailableTR();
                            writeToMIPSFile(currentIndent + "li " + registersInUse[i] + ", " + String.valueOf(immediate));
                        } catch (Exception e) {
                            // operand is data in memory, so we should load it MIPS style

                            // first change the name of the variable so it matchs context
                            String varName = operand + "_" + currentContext;

                            // get a register where to load the data
                            String register = getNextAvailableTR();
                            registersInUse[i] = register;

                            // load it in MIPS
                            writeToMIPSFile(currentIndent + "lw " + register + ", " + varName + dataIndent + "# ld " +
                                    "data " + varName);
                        }
                    }
                    i = i + 1;
                }

                // registersInUse now stores three registers: [0] contains register to store operation, [1] and [2] the values
                // find out what is the operation

                String operationIs = getMIPSOperation(operator);
                if (!operationIs.equals("$err_invalid_op")) {
                    if (!operationIs.equals("mult") && !operationIs.equals("div")) {
                        // write operation in MIPS
                        if (operationIs.equals("slt")) {
                            // write less than in MIPS and assign to register
                            writeToMIPSFile(currentIndent + operationIs + " " + registersInUse[0] + ", " + registersInUse[1] + ", "
                                    + registersInUse[2]);

                            this.temporariesStack.push(registersInUse[1]);
                            this.temporariesStack.push(registersInUse[2]);

                            // now check if is greater than zero. 1 is true, 0 is false or less than
                            // TODO

                        } else {
                            writeToMIPSFile(currentIndent + operationIs + " " + registersInUse[0] + ", " + registersInUse[1] + ", "
                                    + registersInUse[2]);

                            // free registers except [0]
                            this.temporariesStack.push(registersInUse[1]);
                            this.temporariesStack.push(registersInUse[2]);

                            // check if there's need to store
                            if (!locRegisterData.equals("")) {
                                // we need to store
                                writeToMIPSFile(currentIndent + "sw " + registersInUse[0] + ", " + locRegisterData + dataIndent +
                                        "# str data");

                                // since we did a store we can free the registers
                                //this.savedValuesStack.push(registersInUse[0]);
                                this.temporariesStack.push(registersInUse[1]);
                                this.temporariesStack.push(registersInUse[2]);
                            }
                        }
                    } else {
                        writeToMIPSFile(currentIndent + operationIs + " " + registersInUse[1] + ", " + registersInUse[2]);
                        writeToMIPSFile(currentIndent + "mflo " + registersInUse[0]);

                        // check if there's need to store
                        if (!locRegisterData.equals("")) {
                            // we need to store
                            writeToMIPSFile(currentIndent + "sw " + registersInUse[0] + ", " + locRegisterData + dataIndent +
                                    "# str data");

                            // since we did a store we can free the registers
                            //this.savedValuesStack.push(registersInUse[0]);
                        }

                        // free registers except [0]
                        this.temporariesStack.push(registersInUse[1]);
                        this.temporariesStack.push(registersInUse[2]);
                    }
                }

            } else {
                // TODO handle here possible immediate values and other stuff
                String exp = expression.getText();

                // check for inmmediate value
                try {
                    int immediate = Integer.parseInt(exp);

                    // check if there's need to store
                    if (!locRegisterData.equals("")) {
                        writeToMIPSFile(currentIndent + "li " + registersInUse[0] + ", " + String.valueOf(immediate));
                        // we need to store
                        writeToMIPSFile(currentIndent + "sw " + registersInUse[0] + ", " + locRegisterData + dataIndent +
                                "# str data");

                        // since we did a store we can free the registers
                        //this.savedValuesStack.push(registersInUse[0]);
                    } else {
                        writeToMIPSFile(currentIndent + "li " + registersInUse[0] + ", " + String.valueOf(immediate));
                    }
                } catch (Exception e) {
                    // not an immediate value
                    if (exp.contains("_t")) {
                        registersInUse[1] = exp.replace("_t", "$t");
                    } else {
                        // operand is data in memory, so we should load it MIPS style

                        // first change the name of the variable so it matchs context
                        String varName = exp + "_" + currentContext;

                        // get a register where to load the data
                        String register = getNextAvailableTR();
                        registersInUse[1] = register;

                        // load it in MIPS
                        writeToMIPSFile(currentIndent + "lw " + register + ", " + varName + dataIndent + "# ld " +
                                "data " + varName);
                    }

                    // make assignment
                    if (!locRegisterData.equals("")) {
                        writeToMIPSFile(currentIndent + "move " + registersInUse[0] + ", " + registersInUse[1]);
                        // we need to store
                        writeToMIPSFile(currentIndent + "sw " + registersInUse[0] + ", " + locRegisterData + dataIndent +
                                "# str data");

                        // since we did a store we can free the registers
                        //this.savedValuesStack.push(registersInUse[0]);
                        this.temporariesStack.push(registersInUse[1]);
                    } else {
                        writeToMIPSFile(currentIndent + "move " + registersInUse[0] + ", " + registersInUse[1]);
                    }
                }
            }
            assignments.add(ctx);
        }
    }

    private String getMIPSOperation(String operator) {
        if (operator.equals("+")) {
            return "add";
        } else if (operator.equals("&&")) {
            return "and";
        } else if (operator.equals("/")) {
            return "div";
        } else if (operator.equals("*")) {
            return "mult";
        } else if (operator.equals("||")) {
            return "or";
        } else if (operator.equals("-")) {
            return "sub";
        } else if (operator.equals("<")) {
            return "slt";
        } else return "$err_invalid_op";
    }

    private String getOperatorFromExpression(tacParser.ExpressionContext expression) {
        if (expression.and_op() != null) {
            return expression.and_op().getText();
        } else if (expression.or_op() != null) {
            return expression.or_op().getText();
        } else if (expression.eq_op() != null) {
            return expression.eq_op().getText();
        } else if (expression.rel_op() != null) {
            return expression.rel_op().getText();
        } else if (expression.modulus_op() != null) {
            return expression.modulus_op().getText();
        } else if (expression.div_op() != null) {
            return expression.div_op().getText();
        } else if (expression.mul_op() != null) {
            return expression.mul_op().getText();
        } else if (expression.arith_op_sum_subs() != null) {
            return expression.arith_op_sum_subs().getText();
        } else return "$err_no_op";
    }

    private String[] splitTac(String toSplit){
        return toSplit.split("\\+|-|\\*|/|%|&&|\\|\\||==|!=|<|<=|>|>=");
    }

    private void fillRegistersStack() {
        this.temporariesStack.clear();
        this.savedValuesStack.clear();
        this.argumentsStack.clear();

        for (int i = 9; i >= 0; i--) {
            this.temporariesStack.push("$t" + String.valueOf(i));
        }

        for (int i = 7; i >= 0; i--) {
            this.savedValuesStack.push("$s" + String.valueOf(i));
        }

        for (int i = 3; i >= 0; i--) {
            this.argumentsStack.push("$a" + String.valueOf(i));
        }
    }

    // To obtain next available temporary register
    private String getNextAvailableTR() {
        if (!this.temporariesStack.isEmpty()) {
            return this.temporariesStack.pop();
        } else return "$err";
    }

    // To obtain next available saved value register
    private String getNextAvailableSVR() {
        if (!this.savedValuesStack.isEmpty()) {
            return this.savedValuesStack.pop();
        } else return "$err";
    }

    // To obtain next available argument register
    private String getNextAvailableAR() {
        if (!this.argumentsStack.isEmpty()) {
            return this.argumentsStack.pop();
        } else return "$err";
    }

    private void incrementIndent() {
        this.currentIndent = this.currentIndent + "\t";
    }

    private void decrementIndent() {
        this.currentIndent = this.currentIndent.substring(0, this.currentIndent.length()-1);
    }

    private void writeDataInMemory(String methodName, String varName, String type) {
        if (type.equals("int")) {
            writeToMIPSFile(currentIndent + varName + "_" + methodName + ":" + dataIndent + ".word 0");
        } else if (type.equals("char")) {
            writeToMIPSFile(currentIndent + varName + "_" + methodName + ":" + dataIndent + ".asciiz \"\"");
        }
    }

    private void writeToMIPSFile(String line) {
        try(FileWriter fw = new FileWriter("generated_mips.asm", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(line);
        } catch (IOException e) {
        }
    }

    private void writeToMIPSOrderedFile(String line) {
        try(FileWriter fw = new FileWriter("ordered_mips.asm", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(line);
        } catch (IOException e) {
        }
    }

    private void orderFile() {
        // create file
        try {
            File file = new File("ordered_mips.asm");
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // read generated_mips.asm and omit # StartProcedure and # dataSection sections
        try (BufferedReader br = new BufferedReader(new FileReader("generated_mips.asm"))) {
            String line;
            boolean write = true;
            while ((line = br.readLine()) != null) {
                boolean skip = false;
                if (line.contains("# -------------------------------------------------- StartProcedure ---------------------------------------------------")){
                    // omit
                    write = false;
                }

                if (line.contains("# ------------------------------------------------- StartMain ---------------------------------------------------------")) {
                    // write
                    write = true;
                    skip = true;
                }

                if (line.contains("# -------------------------------------------------- EndMain ---------------------------------------------------------")) {
                    skip = true;
                }

                if (line.contains("# ---------- data section ----------")) {
                    // omit
                    write = false;
                }

                if (write && !skip) {
                    writeToMIPSOrderedFile(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // read generated_mips.asm and write procedures and data section
        try (BufferedReader br = new BufferedReader(new FileReader("generated_mips.asm"))) {
            String line;
            boolean write = false;
            while ((line = br.readLine()) != null) {
                boolean skip = false;
                if (line.contains("# -------------------------------------------------- StartProcedure ---------------------------------------------------")){
                    // omit
                    write = true;
                    skip = true;
                }

                if (line.contains("# -------------------------------------------------- EndProcedure -----------------------------------------------------")) {
                    // write
                    write = false;
                }

                if (line.contains("# ---------- data section ----------")) {
                    // omit
                    write = true;
                }

                if (write && !skip) {
                    writeToMIPSOrderedFile(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // copy content of ordered file into generated file and delete ordered file
        try {
            File file = new File("generated_mips.asm");
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedReader br = new BufferedReader(new FileReader("ordered_mips.asm"))) {
            String line;
            while ((line = br.readLine()) != null) {
                writeToMIPSFile(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            File file = new File("ordered_mips.asm");
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
