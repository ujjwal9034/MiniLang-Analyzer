#include<stdio.h>
#include<string.h>
#include<stdlib.h>
#include"lexer.h"
#include"parser.h"
#include"semantic.h"
#include"tree.h"

Token currentToken;
Node *root = NULL;
int lastLine = 1;

// FUNCTION DECLARATIONS
Node* buildSimpleStmt();
void declaration();
void assignment();
void printStmt();
void ifStmt();
void whileStmt();
void expression();

// ERROR 
void errorExpected(char *expected){
    printf("\nSyntax Error at line %d : Expected %s but found '%s'\n",
           lastLine, expected, currentToken.value);
    printf("Syntax galat aagya baa, thik krle (line %d)\n", lastLine);
    exit(1);
}

// MATCH
void match(char *type){
    if(strcmp(currentToken.type,type)==0){
        lastLine = currentToken.line;
        emitToken(currentToken);
        currentToken = getNextToken();
    } else{
        errorExpected(type);
    }
}

// ADD CHILD
void addChild(Node *parent, Node *child){
    if(parent->child == NULL){
        parent->child = child;
    } else{
        Node *temp = parent->child;
        while(temp->sibling != NULL)
            temp = temp->sibling;
        temp->sibling = child;
    }
}

// EXPRESSION
void expression(){
    if(strcmp(currentToken.type,"IDENTIFIER")==0){
        checkDeclared(currentToken.value);
        match("IDENTIFIER");
    }
    else if(strcmp(currentToken.type,"NUMBER")==0){
        match("NUMBER");
    }
    else{
        errorExpected("IDENTIFIER or NUMBER");
    }

    while(strcmp(currentToken.type,"PLUS")==0){
        match("PLUS");
        expression();
    }
}

// SIMPLE STATEMENT BUILDER
Node* buildSimpleStmt(){
    //Print
    if(strcmp(currentToken.value,"bol")==0){
        match("KEYWORD");
        Node *p = createNode("Print");
        checkDeclared(currentToken.value);
        Node *id = createNode(currentToken.value);
        match("IDENTIFIER");
        match("SEMICOLON");
        addChild(p, id);
        return p;
    }
    // assignment
    else if(strcmp(currentToken.type,"IDENTIFIER")==0){
        char name[50];
        strcpy(name,currentToken.value);
        checkDeclared(name);
        match("IDENTIFIER");
        match("ASSIGN");
        expression();
        match("SEMICOLON");
        Node *a = createNode("Assignment");
        Node *id = createNode(name);
        addChild(a, id);
        return a;
    }
    else{
        errorExpected("VALID STATEMENT");
    }
    return NULL;
}

// DECLARATION
void declaration(){
    match("KEYWORD");
    if(strcmp(currentToken.type,"IDENTIFIER")!=0)
        errorExpected("IDENTIFIER");
    addSymbol(currentToken.value);
    Node *n = createNode("Declaration");
    Node *id = createNode(currentToken.value);
    match("IDENTIFIER");
    match("SEMICOLON");
    addChild(n, id);
    addChild(root, n);
}
// ASSIGNMENT
void assignment(){
    char name[50];
    strcpy(name,currentToken.value);
    checkDeclared(name);
    match("IDENTIFIER");
    match("ASSIGN");
    expression();
    match("SEMICOLON");
    Node *n = createNode("Assignment");
    Node *id = createNode(name);
    addChild(n, id);
    addChild(root, n);
}
// PRINT
void printStmt(){
    match("KEYWORD");
    checkDeclared(currentToken.value);
    Node *n = createNode("Print");
    Node *id = createNode(currentToken.value);
    match("IDENTIFIER");
    match("SEMICOLON");
    addChild(n, id);
    addChild(root, n);
}
// IF-ELSE
void ifStmt(){
    match("KEYWORD");
    match("LPAREN");
    expression();
    match("RPAREN");
    Node *ifNode = createNode("If");
    Node *thenNode = buildSimpleStmt();
    addChild(ifNode, thenNode);
    if(strcmp(currentToken.value,"naito")==0){
        match("KEYWORD");
        Node *elseNode = createNode("Else");
        Node *elseStmt = buildSimpleStmt();
        addChild(elseNode, elseStmt);
        addChild(ifNode, elseNode);
    }
    addChild(root, ifNode);
}

// WHILE
void whileStmt(){
    match("KEYWORD");
    match("LPAREN");
    expression();
    match("RPAREN");
    Node *w = createNode("While");
    Node *body = buildSimpleStmt();
    addChild(w, body);
    addChild(root, w);
}

// ===== STATEMENT =====
void statement(){
    if(strcmp(currentToken.value,"ank")==0)
        declaration();
    else if(strcmp(currentToken.value,"bol")==0)
        printStmt();
    else if(strcmp(currentToken.value,"agar")==0)
        ifStmt();
    else if(strcmp(currentToken.value,"jabtak")==0)
        whileStmt();
    else if(strcmp(currentToken.type,"IDENTIFIER")==0)
        assignment();
    else
        errorExpected("VALID STATEMENT");
}

// ===== PROGRAM =====
void parseProgram(){
    currentToken = getNextToken();
    root = createNode("Program");
    while(strcmp(currentToken.type,"EOF")!=0){
        statement();
    }
    printf("\nParsing completed successfully\n");
}