#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include"tree.h"

Node* createNode(char *name){
    Node *n = (Node*)malloc(sizeof(Node));
    strcpy(n->name,name);
    n->child = NULL;
    n->sibling = NULL;
    return n;
}

// 🔥 better readable tree
void printTreeUtil(Node *root, int level){
    if(root == NULL) return;

    // indentation
    for(int i=0;i<level;i++){
        printf("    ");   // 4 spaces
    }

    printf("|-- %s\n", root->name);

    // children
    Node *child = root->child;
    while(child){
        printTreeUtil(child, level+1);
        child = child->sibling;
    }
}

void printTree(Node *root, int level){
    if(root == NULL) return;

    printf("%s\n", root->name);

    Node *child = root->child;
    while(child){
        printTreeUtil(child, 1);
        child = child->sibling;
    }
}