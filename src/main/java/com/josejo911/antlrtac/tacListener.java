// Generated from tac.g4 by ANTLR 4.7.1
package com.josejo911.antlrtac;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link tacParser}.
 */
public interface tacListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link tacParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(tacParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(tacParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#declaration}.
	 * @param ctx the parse tree
	 */
	void enterDeclaration(tacParser.DeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#declaration}.
	 * @param ctx the parse tree
	 */
	void exitDeclaration(tacParser.DeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#mainDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMainDeclaration(tacParser.MainDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#mainDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMainDeclaration(tacParser.MainDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMethodDeclaration(tacParser.MethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMethodDeclaration(tacParser.MethodDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#funcBlock}.
	 * @param ctx the parse tree
	 */
	void enterFuncBlock(tacParser.FuncBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#funcBlock}.
	 * @param ctx the parse tree
	 */
	void exitFuncBlock(tacParser.FuncBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(tacParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(tacParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(tacParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(tacParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(tacParser.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(tacParser.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileStatement(tacParser.WhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileStatement(tacParser.WhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#assignStatement}.
	 * @param ctx the parse tree
	 */
	void enterAssignStatement(tacParser.AssignStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#assignStatement}.
	 * @param ctx the parse tree
	 */
	void exitAssignStatement(tacParser.AssignStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#methodCall}.
	 * @param ctx the parse tree
	 */
	void enterMethodCall(tacParser.MethodCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#methodCall}.
	 * @param ctx the parse tree
	 */
	void exitMethodCall(tacParser.MethodCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#structCall}.
	 * @param ctx the parse tree
	 */
	void enterStructCall(tacParser.StructCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#structCall}.
	 * @param ctx the parse tree
	 */
	void exitStructCall(tacParser.StructCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#returnExp}.
	 * @param ctx the parse tree
	 */
	void enterReturnExp(tacParser.ReturnExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#returnExp}.
	 * @param ctx the parse tree
	 */
	void exitReturnExp(tacParser.ReturnExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#structLocation}.
	 * @param ctx the parse tree
	 */
	void enterStructLocation(tacParser.StructLocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#structLocation}.
	 * @param ctx the parse tree
	 */
	void exitStructLocation(tacParser.StructLocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#pushParam}.
	 * @param ctx the parse tree
	 */
	void enterPushParam(tacParser.PushParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#pushParam}.
	 * @param ctx the parse tree
	 */
	void exitPushParam(tacParser.PushParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#location}.
	 * @param ctx the parse tree
	 */
	void enterLocation(tacParser.LocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#location}.
	 * @param ctx the parse tree
	 */
	void exitLocation(tacParser.LocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#memoryLocation}.
	 * @param ctx the parse tree
	 */
	void enterMemoryLocation(tacParser.MemoryLocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#memoryLocation}.
	 * @param ctx the parse tree
	 */
	void exitMemoryLocation(tacParser.MemoryLocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#label}.
	 * @param ctx the parse tree
	 */
	void enterLabel(tacParser.LabelContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#label}.
	 * @param ctx the parse tree
	 */
	void exitLabel(tacParser.LabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(tacParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(tacParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#and_op}.
	 * @param ctx the parse tree
	 */
	void enterAnd_op(tacParser.And_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#and_op}.
	 * @param ctx the parse tree
	 */
	void exitAnd_op(tacParser.And_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#or_op}.
	 * @param ctx the parse tree
	 */
	void enterOr_op(tacParser.Or_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#or_op}.
	 * @param ctx the parse tree
	 */
	void exitOr_op(tacParser.Or_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#eq_op}.
	 * @param ctx the parse tree
	 */
	void enterEq_op(tacParser.Eq_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#eq_op}.
	 * @param ctx the parse tree
	 */
	void exitEq_op(tacParser.Eq_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#rel_op}.
	 * @param ctx the parse tree
	 */
	void enterRel_op(tacParser.Rel_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#rel_op}.
	 * @param ctx the parse tree
	 */
	void exitRel_op(tacParser.Rel_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#modulus_op}.
	 * @param ctx the parse tree
	 */
	void enterModulus_op(tacParser.Modulus_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#modulus_op}.
	 * @param ctx the parse tree
	 */
	void exitModulus_op(tacParser.Modulus_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#div_op}.
	 * @param ctx the parse tree
	 */
	void enterDiv_op(tacParser.Div_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#div_op}.
	 * @param ctx the parse tree
	 */
	void exitDiv_op(tacParser.Div_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#mul_op}.
	 * @param ctx the parse tree
	 */
	void enterMul_op(tacParser.Mul_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#mul_op}.
	 * @param ctx the parse tree
	 */
	void exitMul_op(tacParser.Mul_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#arith_op_sum_subs}.
	 * @param ctx the parse tree
	 */
	void enterArith_op_sum_subs(tacParser.Arith_op_sum_subsContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#arith_op_sum_subs}.
	 * @param ctx the parse tree
	 */
	void exitArith_op_sum_subs(tacParser.Arith_op_sum_subsContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(tacParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(tacParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#int_literal}.
	 * @param ctx the parse tree
	 */
	void enterInt_literal(tacParser.Int_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#int_literal}.
	 * @param ctx the parse tree
	 */
	void exitInt_literal(tacParser.Int_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#char_literal}.
	 * @param ctx the parse tree
	 */
	void enterChar_literal(tacParser.Char_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#char_literal}.
	 * @param ctx the parse tree
	 */
	void exitChar_literal(tacParser.Char_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link tacParser#bool_literal}.
	 * @param ctx the parse tree
	 */
	void enterBool_literal(tacParser.Bool_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link tacParser#bool_literal}.
	 * @param ctx the parse tree
	 */
	void exitBool_literal(tacParser.Bool_literalContext ctx);
}