#include "tree.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

Node *createNode(char *name) {
  Node *n = (Node *)malloc(sizeof(Node));
  strcpy(n->name, name);
  n->child = NULL;
  n->sibling = NULL;
  return n;
}

//  better readable tree
void printTreeUtil(Node *root, char *prefix, int isLast) {
  if (root == NULL)
    return;

  printf("%s", prefix);
  printf(isLast ? "└── " : "├── ");
  printf("%s\n", root->name);

  char newPrefix[1024];
  strcpy(newPrefix, prefix);
  strcat(newPrefix, isLast ? "    " : "│   ");

  Node *child = root->child;
  while (child) {
    printTreeUtil(child, newPrefix, child->sibling == NULL);
    child = child->sibling;
  }
}

void printTree(Node *root, int level) {
  if (root == NULL)
    return;

  printf("%s\n", root->name);

  char prefix[1024] = "";
  Node *child = root->child;
  while (child) {
    printTreeUtil(child, prefix, child->sibling == NULL);
    child = child->sibling;
  }
}