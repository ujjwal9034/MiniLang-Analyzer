#ifndef TREE_H
#define TREE_H

typedef struct Node{
    char name[50];
    struct Node *child;
    struct Node *sibling;
}Node;

Node* createNode(char *name);
void printTree(Node *root, int level);

#endif