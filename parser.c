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
void inputStmt();
void ifStmt();
void whileStmt();
Node* expression();

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

// FACTOR
Node* factor(){
    Node *left = NULL;
    if(strcmp(currentToken.type,"IDENTIFIER")==0){
        checkDeclared(currentToken.value);
        left = createNode(currentToken.value);
        match("IDENTIFIER");
    }
    else if(strcmp(currentToken.type,"NUMBER")==0){
        left = createNode(currentToken.value);
        match("NUMBER");
    }
    else if(strcmp(currentToken.type,"LPAREN")==0){
        match("LPAREN");
        left = expression();
        match("RPAREN");
    }
    else{
        errorExpected("IDENTIFIER, NUMBER, or '('");
    }
    return left;
}

// MULTIPLICATIVE EXPR
Node* multExpr(){
    Node *left = factor();
    while(strcmp(currentToken.type,"MULT")==0 || strcmp(currentToken.type,"DIV")==0 || strcmp(currentToken.type,"MOD")==0){
        char opType[50]; strcpy(opType, currentToken.type);
        char opVal[50]; strcpy(opVal, currentToken.value);
        match(opType);
        
        Node *right = factor();
        Node *curr = createNode(opVal);
        addChild(curr, left);
        addChild(curr, right);
        left = curr;
    }
    return left;
}

// ADDITIVE EXPR
Node* addExpr(){
    Node *left = multExpr();
    while(strcmp(currentToken.type,"PLUS")==0 || strcmp(currentToken.type,"MINUS")==0){
        char opType[50]; strcpy(opType, currentToken.type);
        char opVal[50]; strcpy(opVal, currentToken.value);
        match(opType);
        
        Node *right = multExpr();
        Node *curr = createNode(opVal);
        addChild(curr, left);
        addChild(curr, right);
        left = curr;
    }
    return left;
}

// EXPRESSION
Node* expression(){
    Node *left = addExpr();
    while(strcmp(currentToken.type,"EQ")==0 || strcmp(currentToken.type,"NEQ")==0 ||
          strcmp(currentToken.type,"LT")==0 || strcmp(currentToken.type,"LTE")==0 ||
          strcmp(currentToken.type,"GT")==0 || strcmp(currentToken.type,"GTE")==0){
        char opType[50]; strcpy(opType, currentToken.type);
        char opVal[50]; strcpy(opVal, currentToken.value);
        match(opType);
        
        Node *right = addExpr();
        Node *curr = createNode(opVal);
        addChild(curr, left);
        addChild(curr, right);
        left = curr;
    }
    return left;
}

// SIMPLE STATEMENT BUILDER
Node* buildSimpleStmt(){
    //Input
    if(strcmp(currentToken.value,"suno")==0){
        match("KEYWORD");
        Node *p = createNode("Input");
        checkDeclared(currentToken.value);
        Node *id = createNode(currentToken.value);
        match("IDENTIFIER");
        match("SEMICOLON");
        addChild(p, id);
        return p;
    }
    //Print
    else if(strcmp(currentToken.value,"bol")==0){
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
        Node *expr = expression();
        match("SEMICOLON");
        Node *a = createNode("Assignment");
        Node *id = createNode(name);
        addChild(a, id);
        addChild(a, expr);
        return a;
    }
    else{
        errorExpected("VALID STATEMENT");
    }
    return NULL;
}

// DECLARATION
void declaration(){
    char type[50];
    strcpy(type, currentToken.value);
    match("KEYWORD");
    if(strcmp(currentToken.type,"IDENTIFIER")!=0)
        errorExpected("IDENTIFIER");
    addSymbol(currentToken.value, type);
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
    Node *expr = expression();
    match("SEMICOLON");
    Node *n = createNode("Assignment");
    Node *id = createNode(name);
    addChild(n, id);
    addChild(n, expr);
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
// INPUT
void inputStmt(){
    match("KEYWORD");
    checkDeclared(currentToken.value);
    Node *n = createNode("Input");
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
    Node *expr = expression();
    match("RPAREN");
    Node *ifNode = createNode("If");
    Node *condNode = createNode("Condition");
    addChild(condNode, expr);
    addChild(ifNode, condNode);
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
    Node *expr = expression();
    match("RPAREN");
    Node *w = createNode("While");
    Node *condNode = createNode("Condition");
    addChild(condNode, expr);
    addChild(w, condNode);
    Node *body = buildSimpleStmt();
    addChild(w, body);
    addChild(root, w);
}

// ===== STATEMENT =====
void statement(){
    if(strcmp(currentToken.value,"ank")==0 || 
       strcmp(currentToken.value,"dashmlav")==0 || 
       strcmp(currentToken.value,"akshar")==0)
        declaration();
    else if(strcmp(currentToken.value,"bol")==0)
        printStmt();
    else if(strcmp(currentToken.value,"suno")==0)
        inputStmt();
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