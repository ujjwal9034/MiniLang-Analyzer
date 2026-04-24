#include<stdio.h>
#include"lexer.h"
#include"parser.h"
#include"semantic.h"
#include"tree.h"
#include"tac.h"

extern Node *root;

int main(){
    runLexer("input.txt");
    parseProgram();
    printf("\nParse Tree:\n");
    printTree(root,0);
    semanticCheck();
    generateTAC(root);
    return 0;
}