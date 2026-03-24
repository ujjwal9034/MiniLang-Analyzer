#include<stdio.h>
#include<ctype.h>
#include<string.h>
#include"lexer.h"

FILE *fp;
FILE *out;
Token currentToken;
int line = 1;

int isKeyword(char *str){
    return strcmp(str,"ank")==0 ||
           strcmp(str,"bol")==0 ||
           strcmp(str,"agar")==0 ||
           strcmp(str,"jabtak")==0 ||
           strcmp(str,"naito")==0;
}

void runLexer(const char *filename){
    fp=fopen(filename,"r");
    out=fopen("tokens.txt","w");
}

void emitToken(Token t){
    printf("%s : %s\n", t.type, t.value);
    fprintf(out,"%s : %s\n", t.type, t.value);
}

Token getNextToken(){
    char ch;
    int i=0;
    while((ch=fgetc(fp))!=EOF && isspace(ch)){
        if(ch=='\n') line++;
    }
    if(ch==EOF){
        strcpy(currentToken.type,"EOF");
        strcpy(currentToken.value,"EOF");
        currentToken.line=line;
        return currentToken;
    }
    if(isalpha(ch)){
        char buffer[50];
        buffer[i++]=ch;
        while(isalnum(ch=fgetc(fp))) buffer[i++]=ch;
        buffer[i]='\0';
        ungetc(ch,fp);

        if(isKeyword(buffer))
            strcpy(currentToken.type,"KEYWORD");
        else
            strcpy(currentToken.type,"IDENTIFIER");

        strcpy(currentToken.value,buffer);
        currentToken.line=line;
        return currentToken;
    }
    if(isdigit(ch)){
        char buffer[50];
        buffer[i++]=ch;
        while(isdigit(ch=fgetc(fp))) buffer[i++]=ch;
        buffer[i]='\0';
        ungetc(ch,fp);
        strcpy(currentToken.type,"NUMBER");
        strcpy(currentToken.value,buffer);
        currentToken.line=line;
        return currentToken;
    }
    if(ch=='='){ strcpy(currentToken.type,"ASSIGN"); strcpy(currentToken.value,"="); }
    else if(ch=='+'){ strcpy(currentToken.type,"PLUS"); strcpy(currentToken.value,"+"); }
    else if(ch==';'){ strcpy(currentToken.type,"SEMICOLON"); strcpy(currentToken.value,";"); }
    else if(ch=='('){ strcpy(currentToken.type,"LPAREN"); strcpy(currentToken.value,"("); }
    else if(ch==')'){ strcpy(currentToken.type,"RPAREN"); strcpy(currentToken.value,")"); }
    else{
        strcpy(currentToken.type,"UNKNOWN");
        currentToken.value[0]=ch;
        currentToken.value[1]='\0';
    }

    currentToken.line=line;
    return currentToken;
}