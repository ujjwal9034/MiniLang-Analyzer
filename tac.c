/* tac.c - Three Address Code Generator for MiniLang
   Written as part of Phase 3 of the compiler.
   Format: result = operand1 op operand2
   Temporaries: t1, t2, t3, ...
   Labels: L1, L2, L3, ...
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "tac.h"

static int tempCount  = 1;
static int labelCount = 1;

static int newTemp()  { return tempCount++;  }
static int newLabel() { return labelCount++; }

/* make a heap string like "t3" or "L2" */
static char* tempName(int n) {
    char *s = (char*)malloc(10);
    sprintf(s, "t%d", n);
    return s;
}

/* walk an expression subtree, emit TAC, return the result variable/temp */
static char* exprTAC(Node *node) {
    if (node == NULL) return "";

    /* leaf node - just a variable name or number, no instruction needed */
    if (node->child == NULL)
        return node->name;

    /* inner node - binary operator */
    char *left  = exprTAC(node->child);
    char *right = exprTAC(node->child->sibling);

    int id = newTemp();
    char *res = tempName(id);

    printf("    %s = %s %s %s\n", res, left, node->name, right);
    return res;
}

static void tacStmt(Node *node);

static void tacStmt(Node *node) {
    if (node == NULL) return;

    /* read input */
    if (strcmp(node->name, "Input") == 0) {
        printf("    read %s\n", node->child->name);
        return;
    }

    /* print output */
    if (strcmp(node->name, "Print") == 0) {
        printf("    print %s\n", node->child->name);
        return;
    }

    /* assignment: evaluate rhs expression, then assign */
    if (strcmp(node->name, "Assignment") == 0) {
        Node *lhs  = node->child;
        Node *rhs  = lhs->sibling;
        char *val  = exprTAC(rhs);
        printf("    %s = %s\n", lhs->name, val);
        return;
    }

    /*
       if-else
       ---
       evaluate condition -> temp
       if temp goto Ltrue
       goto Lfalse
       Ltrue:
         <then body>
         goto Lend      <- only if else exists
       Lfalse:
         <else body>    <- only if else exists
       Lend:
    */
    if (strcmp(node->name, "If") == 0) {
        Node *condWrap = node->child;
        Node *condExpr = condWrap->child;

        printf("\n    // if statement\n");
        char *cond = exprTAC(condExpr);

        int ltrue  = newLabel();
        int lfalse = newLabel();
        int lend   = newLabel();

        printf("    if %s goto L%d\n", cond, ltrue);
        printf("    goto L%d\n", lfalse);

        printf("  L%d:\n", ltrue);
        Node *thenStmt = condWrap->sibling;
        tacStmt(thenStmt);

        Node *elseNode = thenStmt->sibling;
        if (elseNode != NULL) {
            printf("    goto L%d\n", lend);
            printf("  L%d:\n", lfalse);
            tacStmt(elseNode->child);
            printf("  L%d:\n", lend);
        } else {
            printf("  L%d:\n", lfalse);
        }
        return;
    }

    /*
       while loop
       ---
       Lstart:
         evaluate condition -> temp
         ifFalse temp goto Lend
         <body>
         goto Lstart
       Lend:
    */
    if (strcmp(node->name, "While") == 0) {
        int lstart = newLabel();
        int lend   = newLabel();

        printf("\n    // while loop\n");
        printf("  L%d:\n", lstart);

        Node *condWrap = node->child;
        Node *condExpr = condWrap->child;
        char *cond = exprTAC(condExpr);

        printf("    ifFalse %s goto L%d\n", cond, lend);

        Node *body = condWrap->sibling;
        tacStmt(body);

        printf("    goto L%d\n", lstart);
        printf("  L%d:\n", lend);
        return;
    }

    /* for Program/Declaration nodes - just walk their children */
    Node *child = node->child;
    while (child != NULL) {
        tacStmt(child);
        child = child->sibling;
    }
}

void generateTAC(Node *root) {
    printf("\nThree-Address Code:\n");
    printf("-------------------\n");
    tacStmt(root);
    printf("\n");
}
