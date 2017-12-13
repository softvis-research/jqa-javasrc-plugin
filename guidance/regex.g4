grammar regex;

RegularExpression : alternatives ( '|' alternatives )*;

Block : '(' alternatives ( '|' alternatives )* ')';

ComplexRange : '[' ranges+ ']';

IntervalRange : from[CHAR_LITERAL] '-' to[CHAR_LITERAL];

CharTerminal : value[CHAR_LITERAL];

StringTerminal : value[STRING_LITERAL];

Dot : '.';

Alternative : elements*;

Element : atom (suffix[MULTIPLICITY])?;

Not : '~' body;

MULTIPLICITY : $'?'|'*'|'+'$;
