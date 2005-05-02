package com.siyeh.ig.resources;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.ExpressionInspection;
import com.siyeh.ig.GroupNames;
import com.siyeh.ig.psiutils.TypeUtils;

public class JNDIResourceInspection extends ExpressionInspection{
    public String getID(){
        return "JNDIResourceOpenedButNotSafelyClosed";
    }

    public String getDisplayName(){
        return "JNDI resource opened but not safely closed";
    }

    public String getGroupDisplayName(){
        return GroupNames.RESOURCE_GROUP_NAME;
    }

    public String buildErrorString(PsiElement location){
        final PsiExpression expression = (PsiExpression) location;
        final PsiType type = expression.getType();
        final String text = type.getPresentableText();
        return text +
                       " should be opened in a try block, and closed in a finally block #loc";
    }

    public BaseInspectionVisitor createVisitor(InspectionManager inspectionManager,
                                               boolean onTheFly){
        return new JNDIResourceVisitor(this, inspectionManager, onTheFly);
    }

    private static class JNDIResourceVisitor extends BaseInspectionVisitor{
        private JNDIResourceVisitor(BaseInspection inspection,
                                  InspectionManager inspectionManager,
                                  boolean isOnTheFly){
            super(inspection, inspectionManager, isOnTheFly);
        }

        public void visitMethodCallExpression(PsiMethodCallExpression expression){
            super.visitMethodCallExpression(expression);
            if(!isJNDIFactoryMethod(expression)) {
                return;
            }
            final PsiElement parent = expression.getParent();
            if(!(parent instanceof PsiAssignmentExpression)) {
                registerError(expression);
                return;
            }
            final PsiAssignmentExpression assignment =
                    (PsiAssignmentExpression) parent;
            final PsiExpression lhs = assignment.getLExpression();
            if(!(lhs instanceof PsiReferenceExpression)) {
                return;
            }
            final PsiElement referent =
                    ((PsiReference) lhs).resolve();
            if(referent == null || !(referent instanceof PsiVariable)) {
                return;
            }
            final PsiVariable boundVariable = (PsiVariable) referent;

            PsiElement currentContext = expression;
            while(true){
                final PsiTryStatement tryStatement =
                        (PsiTryStatement) PsiTreeUtil.getParentOfType(currentContext,
                                                                      PsiTryStatement.class);
                if(tryStatement == null) {
                    registerError(expression);
                    return;
                }
                if(resourceIsOpenedInTryAndClosedInFinally(tryStatement,
                                                           expression,
                                                           boundVariable)) {
                    return;
                }
                currentContext = tryStatement;
            }
        }


        public void visitNewExpression(PsiNewExpression expression){
            super.visitNewExpression(expression);
            if(!isJNDIResource(expression)){
                return;
            }
            final PsiElement parent = expression.getParent();
            if(!(parent instanceof PsiAssignmentExpression)){
                registerError(expression);
                return;
            }
            final PsiAssignmentExpression assignment =
                    (PsiAssignmentExpression) parent;
            final PsiExpression lhs = assignment.getLExpression();
            if(!(lhs instanceof PsiReferenceExpression)){
                return;
            }
            final PsiElement referent =
                    ((PsiReference) lhs).resolve();
            if(referent == null || !(referent instanceof PsiVariable)){
                return;
            }
            final PsiVariable boundVariable = (PsiVariable) referent;

            PsiElement currentContext = expression;
            while(true){
                final PsiTryStatement tryStatement =
                        (PsiTryStatement) PsiTreeUtil.getParentOfType(currentContext,
                                                                      PsiTryStatement.class);
                if(tryStatement == null){
                    registerError(expression);
                    return;
                }
                if(resourceIsOpenedInTryAndClosedInFinally(tryStatement,
                                                           expression,
                                                           boundVariable)){
                    return;
                }
                currentContext = tryStatement;
            }
        }

        private static boolean resourceIsOpenedInTryAndClosedInFinally(PsiTryStatement tryStatement,
                                                                       PsiExpression lhs,
                                                                       PsiVariable boundVariable){
            final PsiCodeBlock finallyBlock = tryStatement.getFinallyBlock();
            if(finallyBlock == null){
                return false;
            }
            final PsiCodeBlock tryBlock = tryStatement.getTryBlock();
            if(tryBlock == null){
                return false;
            }
            if(!PsiTreeUtil.isAncestor(tryBlock, lhs, true)){
                return false;
            }
            return containsResourceClose(finallyBlock, boundVariable);
        }

        private static boolean containsResourceClose(PsiCodeBlock finallyBlock,
                                                     PsiVariable boundVariable){
            final CloseVisitor visitor =
                    new CloseVisitor(boundVariable);
            finallyBlock.accept(visitor);
            return visitor.containsStreamClose();
        }
    }

    private static class CloseVisitor extends PsiRecursiveElementVisitor{
        private boolean containsClose = false;
        private PsiVariable socketToClose;

        private CloseVisitor(PsiVariable elementToClose){
            super();
            this.socketToClose = elementToClose;
        }

        public void visitElement(PsiElement element){
            if(!containsClose){
                super.visitElement(element);
            }
        }

        public void visitMethodCallExpression(PsiMethodCallExpression call){
            if(containsClose){
                return;
            }
            super.visitMethodCallExpression(call);
            final PsiReferenceExpression methodExpression =
                    call.getMethodExpression();
            if(methodExpression == null){
                return;
            }
            final String methodName = methodExpression.getReferenceName();
            if(!"close".equals(methodName)){
                return;
            }
            final PsiExpression qualifier =
                    methodExpression.getQualifierExpression();
            if(!(qualifier instanceof PsiReferenceExpression)){
                return;
            }
            final PsiElement referent =
                    ((PsiReference) qualifier).resolve();
            if(referent == null)
            {
                return;
            }
            if(referent.equals(socketToClose)){
                containsClose = true;
            }
        }

        public boolean containsStreamClose(){
            return containsClose;
        }
    }

    private static boolean isJNDIResource(PsiNewExpression expression){
        return TypeUtils.expressionHasTypeOrSubtype("javax.naming.InitialContext",
                                                    expression);
    }

    private static boolean isJNDIFactoryMethod(PsiMethodCallExpression expression){
        final PsiReferenceExpression methodExpression = expression.getMethodExpression();
        if(methodExpression == null)
        {
            return false;
        }
        final String methodName = methodExpression.getReferenceName();
        if(!("list".equals(methodName) || "listBindings".equals(methodName)))
        {
            return false;
        }
        final PsiExpression qualifier = methodExpression.getQualifierExpression();
        if(qualifier == null)
        {
            return false;
        }
        return TypeUtils.expressionHasTypeOrSubtype("javax.naming.Context",
                                                    qualifier);
    }
}
