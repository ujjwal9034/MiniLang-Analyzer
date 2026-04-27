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

void printTreeUtil(Node *root, char *prefix, int isLast) {
  if (root == NULL)
    return;

  printf("%s", prefix);
  printf(isLast ? "└── " : "├── ");

  // Color logic for nodes
  if (strcmp(root->name, "Assignment") == 0 || strcmp(root->name, "Declaration") == 0)
      printf("\x1b[1;32m%s\x1b[0m\n", root->name); // Green
  else if (strcmp(root->name, "If") == 0 || strcmp(root->name, "While") == 0 || strcmp(root->name, "Else") == 0)
      printf("\x1b[1;35m%s\x1b[0m\n", root->name); // Magenta
  else if (strcmp(root->name, "Input") == 0 || strcmp(root->name, "Print") == 0)
      printf("\x1b[1;36m%s\x1b[0m\n", root->name); // Cyan
  else if (strcmp(root->name, "Condition") == 0)
      printf("\x1b[1;33m%s\x1b[0m\n", root->name); // Yellow
  else if (root->child == NULL)
      printf("\x1b[37m%s\x1b[0m\n", root->name); // Gray for leaves
  else
      printf("\x1b[1;34m%s\x1b[0m\n", root->name); // Blue

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

  printf("\x1b[1;36m%s\x1b[0m\n", root->name);

  char prefix[1024] = "";
  Node *child = root->child;
  while (child) {
    printTreeUtil(child, prefix, child->sibling == NULL);
    child = child->sibling;
  }
}