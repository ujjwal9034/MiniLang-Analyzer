/*
 * tac.c  -  Three-Address Code (TAC) Generator
 * Phase 3 of MiniLang Compiler
 *
 * TAC format:
 *   t1 = b * 10        (binary arithmetic / comparison)
 *   a  = t1            (copy / assignment)
 *   in  a              (read input into a)
 *   out a              (print a)
 *   if  t2 goto L1     (conditional branch - true)
 *   ifFalse t2 goto L2 (conditional branch - false)
 *   goto L1            (unconditional jump)
 *   L1:                (label definition)
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "tac.h"

static int tempCounter  = 1;
static int labelCounter = 1;
static int stepCounter  = 1;

static int newTemp()  { return tempCounter++;  }
static int newLabel() { return labelCounter++; }

static void emit(const char *instr, const char *comment) {
    printf("  Step %-3d  |  %-35s  // %s\n", stepCounter++, instr, comment);
}

static char* makeTempName(int n) {
    char *buf = (char*)malloc(10);
    sprintf(buf, "t%d", n);
    return buf;
}

static char* generateExprTAC(Node *expr) {
    if (expr == NULL) return "";

    /* leaf node - identifier or number, no instruction needed */
    if (expr->child == NULL)
        return expr->name;

    /* binary operator node */
    char *left  = generateExprTAC(expr->child);
    char *right = generateExprTAC(expr->child->sibling);

    int   id  = newTemp();
    char *res = makeTempName(id);

    char instr[128], comment[128];
    snprintf(instr,   sizeof(instr),   "%s = %s %s %s", res, left, expr->name, right);
    snprintf(comment, sizeof(comment), "Compute (%s %s %s) -> store in %s", left, expr->name, right, res);

    emit(instr, comment);
    return res;
}

static void generateTACRecursive(Node *root) {
    if (root == NULL) return;

    /* Assignment */
    if (strcmp(root->name, "Assignment") == 0) {
        Node *idNode   = root->child;
        Node *exprNode = idNode->sibling;

        printf("\n  -- Assignment: %s = <expr> --\n", idNode->name);

        char *res = generateExprTAC(exprNode);

        char instr[64], comment[64];
        snprintf(instr,   sizeof(instr),   "%s = %s", idNode->name, res);
        snprintf(comment, sizeof(comment), "Store result into variable '%s'", idNode->name);
        emit(instr, comment);
        return;
    }

    /* Input */
    if (strcmp(root->name, "Input") == 0) {
        printf("\n  -- Input Statement --\n");
        char instr[64], comment[64];
        snprintf(instr,   sizeof(instr),   "in %s", root->child->name);
        snprintf(comment, sizeof(comment), "Read value from user into '%s'", root->child->name);
        emit(instr, comment);
        return;
    }

    /* Print */
    if (strcmp(root->name, "Print") == 0) {
        printf("\n  -- Print Statement --\n");
        char instr[64], comment[64];
        snprintf(instr,   sizeof(instr),   "out %s", root->child->name);
        snprintf(comment, sizeof(comment), "Print value of '%s'", root->child->name);
        emit(instr, comment);
        return;
    }

    /*
     * If-Else
     *   tN = <condition>
     *   if tN goto Ltrue
     *   goto Lfalse
     * Ltrue:
     *   <then body>
     *   goto Lend      (only if else exists)
     * Lfalse:
     *   <else body>    (only if else exists)
     * Lend:
     */
    if (strcmp(root->name, "If") == 0) {
        printf("\n  -- If Statement --\n");

        Node *condWrap = root->child;
        Node *condExpr = condWrap->child;

        char *condRes = generateExprTAC(condExpr);

        int lTrue  = newLabel();
        int lFalse = newLabel();
        int lEnd   = newLabel();

        char instr[64], comment[64];

        snprintf(instr,   sizeof(instr),   "if %s goto L%d", condRes, lTrue);
        snprintf(comment, sizeof(comment), "Condition TRUE  -> jump to L%d", lTrue);
        emit(instr, comment);

        snprintf(instr,   sizeof(instr),   "goto L%d", lFalse);
        snprintf(comment, sizeof(comment), "Condition FALSE -> jump to L%d", lFalse);
        emit(instr, comment);

        printf("\nL%d:\n", lTrue);

        Node *thenStmt = condWrap->sibling;
        generateTACRecursive(thenStmt);

        Node *elseNode = thenStmt->sibling;
        if (elseNode != NULL) {
            snprintf(instr,   sizeof(instr),   "goto L%d", lEnd);
            snprintf(comment, sizeof(comment), "End of then-block, skip else -> L%d", lEnd);
            emit(instr, comment);

            printf("\nL%d:\n", lFalse);
            generateTACRecursive(elseNode->child);

            printf("\nL%d:\n", lEnd);
        } else {
            printf("\nL%d:\n", lFalse);
        }
        return;
    }

    /*
     * While Loop
     * Lstart:
     *   tN = <condition>
     *   ifFalse tN goto Lend
     *   <body>
     *   goto Lstart
     * Lend:
     */
    if (strcmp(root->name, "While") == 0) {
        printf("\n  -- While Loop --\n");

        int lStart = newLabel();
        int lEnd   = newLabel();

        printf("\nL%d:\n", lStart);

        Node *condWrap = root->child;
        Node *condExpr = condWrap->child;
        char *condRes  = generateExprTAC(condExpr);

        char instr[64], comment[64];

        snprintf(instr,   sizeof(instr),   "ifFalse %s goto L%d", condRes, lEnd);
        snprintf(comment, sizeof(comment), "Condition FALSE -> exit loop, jump to L%d", lEnd);
        emit(instr, comment);

        Node *body = condWrap->sibling;
        generateTACRecursive(body);

        snprintf(instr,   sizeof(instr),   "goto L%d", lStart);
        snprintf(comment, sizeof(comment), "Loop back to L%d", lStart);
        emit(instr, comment);

        printf("\nL%d:\n", lEnd);
        return;
    }

    /* Default: walk children (Program, Declaration, etc.) */
    Node *child = root->child;
    while (child != NULL) {
        generateTACRecursive(child);
        child = child->sibling;
    }
}

void generateTAC(Node *root) {
    /* sentinel line used by the Java IDE to detect the TAC section */
    printf("\nThree-Address Code:\n");
    printf("================================================================\n");
    printf("  PHASE 3 : Intermediate Code Generation (TAC)\n");
    printf("  Format : result = operand1 op operand2\n");
    printf("  t1,t2,.. = temporaries    L1,L2,.. = jump labels\n");
    printf("================================================================\n");
    printf("\n");
    printf("  %-8s  %-35s  %s\n", "Step", "Instruction", "Explanation");
    printf("  %-8s  %-35s  %s\n",
           "--------", "-----------------------------------",
           "-----------------------------");

    generateTACRecursive(root);

    printf("\n----------------------------------------------------------------\n");
    printf("  Temporaries : %d  (t1 .. t%d)\n", tempCounter-1, tempCounter-1);
    printf("  Labels      : %d  (L1 .. L%d)\n", labelCounter-1, labelCounter-1);
    printf("  Instructions: %d\n", stepCounter-1);
    printf("----------------------------------------------------------------\n");
}
