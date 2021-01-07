grammar tac;

fragment LETTER:    ('a'..'z'|'A'..'Z') ;
fragment DIGIT:     '0'..'9' ;
ID:                 LETTER( LETTER | DIGIT)* ;
NUM:                DIGIT(DIGIT)* ;
COMMENTS:           '//' ~('\r' | '\n' )*  -> channel(HIDDEN);
WHITESPACE:         [ \t\r\n\f]+  ->channel(HIDDEN);
CHAR:               '\'' ( ~['\r\n\\] | '\\' ['\\] ) '\'';

program:            (declaration)* ;
declaration:        mainDeclaration | methodDeclaration ;
mainDeclaration:    'main' ':' funcBlock ;
methodDeclaration:  location ':' funcBlock ;
funcBlock:          'BeginFunc' NUM ';' block 'EndFunc' ';' ;
block:              (statement)* ;
statement:          ifStatement
                    | whileStatement
                    | returnExp
                    | methodCall
                    | structCall
                    | assignStatement
                    | (expression)? ';';
ifStatement:        'Ifz' location 'Goto' location ';' block 'Goto' location ';' label block label ;
whileStatement:     'StartWhile:' label (assignStatement) 'Ifz' location 'Goto' location ';' block 'Goto' location (':')* label ;
assignStatement:    location '=' expression ';' ;
methodCall:         (pushParam)* 'LCall' location ';' 'PopParams' NUM ';' ;
structCall:         'SCall' structLocation block 'End Scall;' ;
returnExp:             'Return' location ;
structLocation:     ID ('.' structLocation)? ;
pushParam:          'PushParam' location ';' ;
location:           ID | '_' ID | memoryLocation ;
memoryLocation:     '*(_' ID ');' ;
label:              '_' ID ':' ;
expression:         methodCall
                    | location
                    | literal
                    | expression and_op expression
                    | expression or_op expression
                    | expression eq_op expression
                    | expression rel_op expression
                    | expression modulus_op expression
                    | expression div_op expression
                    | expression mul_op expression
                    | expression arith_op_sum_subs expression
                    ;
and_op:                '&&';
or_op:                 '||';
eq_op:                 '==' | '!=' ;
rel_op:                '<' | '<=' ;
modulus_op:            '%';
div_op:                '/';
mul_op:                '*';
arith_op_sum_subs:     '+' | '-';
literal:               int_literal | char_literal | bool_literal ;
int_literal:           NUM ;
char_literal:          CHAR ;
bool_literal:          'true' | 'false' ;