#include<stdio.h>
#include<string.h>
#include<stdlib.h>
#include"semantic.h"

typedef struct {
    char name[50];
    char type[50];
} Symbol;

Symbol symbolTable[100];
int count = 0;

int exists(char *name){
    for(int i=0;i<count;i++){
        if(strcmp(symbolTable[i].name,name)==0)
            return 1;
    }
    return 0;
}

void addSymbol(char *name, char *type){
    if(exists(name)){
        printf("\n\033[1;31m[!] Semantic Error:\033[0m Variable \033[1;31m'%s'\033[0m is already declared!\n", name);
        printf("\033[1;33m💡 Hint: Ek naam ke do variable na banao.\033[0m\n");
        exit(1);
    }
    strcpy(symbolTable[count].name,name);
    strcpy(symbolTable[count].type,type);
    count++;
}

void checkDeclared(char *name){
    if(!exists(name)){
        printf("\n\033[1;31m[!] Semantic Error:\033[0m Variable \033[1;31m'%s'\033[0m is not declared!\n", name);
        printf("\033[1;33m💡 Hint: Pehle '%s' ko declare kara, fir use kara.\033[0m\n", name);
        exit(1);
    }
}

void semanticCheck(){
    printf("\nSymbol Table:\n");
    for(int i=0;i<count;i++){
        printf("%d : %s : %s\n", i+1, symbolTable[i].name, symbolTable[i].type);
    }
}