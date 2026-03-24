#ifndef LEXER_H
#define LEXER_H

typedef struct {
    char type[20];
    char value[50];
    int line;
} Token;

void runLexer(const char *filename);
Token getNextToken();
void emitToken(Token t);

#endif