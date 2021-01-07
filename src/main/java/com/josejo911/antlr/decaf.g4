// decaf grammar for antlr v.4
// author@: Jose JO

grammar decaf;

// -------------------------------- LEXER --------------------------------------

fragment LETTER:    ('a'..'z'|'A'..'Z') ;
fragment DIGIT:     '0'..'9' ;
ID:                 LETTER( LETTER | DIGIT)* ;
NUM:                DIGIT(DIGIT)* ;
COMMENTS:           '//' ~('\r' | '\n' )*  -> channel(HIDDEN);
WHITESPACE:         [ \t\r\n\f]+  ->channel(HIDDEN);
CHAR:               '\'' ( ~['\r\n\\] | '\\' ['\\] ) '\'';

// -------------------------------- LEXER --------------------------------------

// ------------------------------- PARSER --------------------------------------

program:              'class' ID '{' (declaration)* '}'  ;

declaration:          structDeclaration| varDeclaration | methodDeclaration  ;

varDeclaration:       varType ID ';' | varType ID '[' NUM ']' ';'  ;

structDeclaration:    'struct' ID '{' (varDeclaration)* '}'  ;

varType:              'int'
                      | 'char'
                      | 'boolean'
                      | 'struct' ID
                      | structDeclaration
                      | 'void'  ;

methodDeclaration:    methodType ID '(' (parameter (',' parameter)*)* ')' block  ;

methodType:           'int' | 'char' | 'boolean' | 'void' ;

parameter:            parameterType ID | parameterType ID '[' ']' ;

parameterType:        'int' | 'char' | 'boolean'  ;

block:                '{' (varDeclaration)* (statement)* '}' ;

statement:            'if' '(' expression ')' block ( 'else' block )?
                       | 'while' '(' expression ')' block
                       |'return' expressionA ';'
                       | methodCall ';'
                       | block
                       | location '=' expression
                       | (expression)? ';'  ;

expressionA:           expression | ;

location:              (ID|ID '[' expression ']') ('.' location)?  ;

expression:            methodCall
                       | location
                       | literal
                       | '('expression')'
                       | '-' expression
                       | '!' expression
                       | expression and_op expression
                       | expression or_op expression
                       | expression eq_op expression
                       | expression rel_op expression
                       | expression modulus_op expression
                       | expression div_op expression
                       | expression mul_op expression
                       | expression arith_op_sum_subs expression
                       ;

methodCall:            ID '(' arg1 ')' ;

arg1:                  arg2 |;

arg2:                  (arg) (',' arg)* ;

arg:                   expression;


and_op:                '&&';

or_op:                 '||';

eq_op:                 '==' | '!=' ;

rel_op:                '<' | '>' | '<=' | '>=' ;

modulus_op:            '%';

div_op:                '/';

mul_op:                '*';

arith_op_sum_subs:     '+' | '-';


literal:               int_literal | char_literal | bool_literal ;

int_literal:           NUM ;

char_literal:          CHAR ;

bool_literal:          'true' | 'false' ;

// ------------------------------- PARSER --------------------------------------
