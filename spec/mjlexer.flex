
package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

%{

	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type) {
		return new Symbol(type, yyline+1, yycolumn);
	}
	
	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type, Object value) {
		return new Symbol(type, yyline+1, yycolumn, value);
	}

%}

%cup
%line
%column

%xstate COMMENT

%eofval{
	return new_symbol(sym.EOF);
%eofval}

%%

" " 	{ }
"\b" 	{ }
"\t" 	{ }
"\r\n" 	{ }
"\f" 	{ }

"program"   { return new_symbol(sym.PROG, yytext());}
"const" { return new_symbol(sym.CONST, yytext());}
"new" { return new_symbol(sym.NEW, yytext());}
"print" { return new_symbol(sym.PRINT, yytext());}
"read" { return new_symbol(sym.READ, yytext());}
"return" { return new_symbol(sym.RETURN, yytext());}
"void" { return new_symbol(sym.VOID, yytext());}
"do" { return new_symbol(sym.DO, yytext());}
"while" { return new_symbol(sym.WHILE, yytext());}
"continue" { return new_symbol(sym.CONTINUE, yytext());}
"break" { return new_symbol(sym.BREAK, yytext());}
"if" { return new_symbol(sym.IF, yytext());}
"else" { return new_symbol(sym.ELSE, yytext());}
"class" { return new_symbol(sym.CLASS, yytext());}
"enum" { return new_symbol(sym.ENUM, yytext());}
"this" { return new_symbol(sym.THIS, yytext());}
"super" { return new_symbol(sym.SUPER, yytext());}
"extends" { return new_symbol(sym.EXTENDS, yytext());}
"goto" { return new_symbol(sym.GOTO, yytext());}
"record" { return new_symbol(sym.RECORD, yytext());}
"+" { return new_symbol(sym.PLUS, yytext());}
"-" { return new_symbol(sym.MINUS, yytext());}
"*" { return new_symbol(sym.MUL, yytext());}
"/" { return new_symbol(sym.DIV, yytext());}
"%" { return new_symbol(sym.MOD, yytext());}
"==" { return new_symbol(sym.EQEQ, yytext());}
"!=" { return new_symbol(sym.NEQ, yytext());}
">" { return new_symbol(sym.GR, yytext());}
">=" { return new_symbol(sym.GREQ, yytext());}
"<" { return new_symbol(sym.LS, yytext());}
"<=" { return new_symbol(sym.LSEQ, yytext());}
"&&" { return new_symbol(sym.AND, yytext());}
"||" { return new_symbol(sym.OR, yytext());}
"=" { return new_symbol(sym.EQ, yytext());}
"++" { return new_symbol(sym.INC, yytext());}
"--" { return new_symbol(sym.DEC, yytext());}
";" { return new_symbol(sym.SEMI, yytext());}
":" { return new_symbol(sym.COL, yytext());}
"," { return new_symbol(sym.COMMA, yytext());}
"." { return new_symbol(sym.DOT, yytext());}
"(" { return new_symbol(sym.LNB, yytext());}
")" { return new_symbol(sym.RNB, yytext());}
"[" { return new_symbol(sym.LSB, yytext());}
"]" { return new_symbol(sym.RSB, yytext());}
"{" { return new_symbol(sym.LCB, yytext());}
"}" { return new_symbol(sym.RCB, yytext());}

"//" {yybegin(COMMENT);}
<COMMENT> . {yybegin(COMMENT);}
<COMMENT> "\r\n" { yybegin(YYINITIAL); }

[0-9]+  { return new_symbol(sym.NUMBER, new Integer (yytext())); }
"'"[ -~]"'"  {return new_symbol (sym.CHAR, yytext().charAt(1)); }
(true | false)  {return new_symbol (sym.BOOL, Boolean.parseBoolean(yytext()) ? 1 : 0); }
([a-z]|[A-Z])[a-z|A-Z|0-9|_]* 	{return new_symbol (sym.IDENT, yytext()); }


. { System.err.println("Leksicka greska ("+yytext()+") u liniji "+(yyline+1)); }