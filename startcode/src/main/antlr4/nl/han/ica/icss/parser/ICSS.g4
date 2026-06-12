grammar ICSS;

// Lexer tokens
IF: 'if';
ELSE: 'else';
BOX_BRACKET_OPEN: '[';
BOX_BRACKET_CLOSE: ']';

TRUE: 'TRUE';
FALSE: 'FALSE';
PIXELSIZE: [0-9]+ 'px';
PERCENTAGE: [0-9]+ '%';
SCALAR: [0-9]+;

COLOR: '#' [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f];

ID_IDENT: '#' [a-z0-9\-]+;
CLASS_IDENT: '.' [a-z0-9\-]+;

LOWER_IDENT: [a-z] [a-z0-9\-]*;
CAPITAL_IDENT: [A-Z] [A-Za-z0-9_]*;

WS: [ \t\r\n]+ -> skip;

OPEN_BRACE: '{';
CLOSE_BRACE: '}';
SEMICOLON: ';';
COLON: ':';
PLUS: '+';
MIN: '-';
MUL: '*';
ASSIGNMENT_OPERATOR: ':=';

// Parser rules
stylesheet: (variableAssignment | stylerule)+ EOF;

stylerule: selector OPEN_BRACE body CLOSE_BRACE;

selector: LOWER_IDENT | ID_IDENT | CLASS_IDENT;

variableAssignment: CAPITAL_IDENT ASSIGNMENT_OPERATOR expression SEMICOLON;

body: (ifClause | declaration)*;

ifClause: IF BOX_BRACKET_OPEN expression BOX_BRACKET_CLOSE OPEN_BRACE body CLOSE_BRACE elseClause?;

elseClause: ELSE OPEN_BRACE body CLOSE_BRACE;

declaration: LOWER_IDENT COLON expression SEMICOLON;

expression: literal #literalExpressie
          | expression MUL expression #mulExpressie
          | expression (PLUS | MIN) expression #plusMinExpressie;

literal: COLOR | PIXELSIZE | PERCENTAGE | SCALAR | TRUE | FALSE | CAPITAL_IDENT;