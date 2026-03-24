#include<stdio.h>
#include<string.h>
#include<stdlib.h>
#include"semantic.h"

char symbolTable[100][50];
int count = 0;

int exists(char *name){
    for(int i=0;i<count;i++){
        if(strcmp(symbolTable[i],name)==0)
            return 1;
    }
    return 0;
}

void addSymbol(char *name){
    if(exists(name)){
        printf("\nSemantic Error: '%s' already declared\n", name);
        exit(1);
    }
    strcpy(symbolTable[count++],name);
}

void checkDeclared(char *name){
    if(!exists(name)){
        printf("\nSemantic Error: '%s' not declared\n", name);
        exit(1);
    }
}

void semanticCheck(){
    printf("\nSymbol Table:\n");
    for(int i=0;i<count;i++){
        printf("%d : %s\n", i+1, symbolTable[i]);
    }
}