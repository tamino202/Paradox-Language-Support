// This is a generated file. Not intended for manual editing.
package icu.windea.pls.script.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static icu.windea.pls.script.psi.ParadoxScriptElementTypes.*;
import icu.windea.pls.script.psi.*;

public class ParadoxScriptInlineMathBiExpressionImpl extends ParadoxScriptInlineMathExpressionImpl implements ParadoxScriptInlineMathBiExpression {

  public ParadoxScriptInlineMathBiExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull ParadoxScriptVisitor visitor) {
    visitor.visitInlineMathBiExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ParadoxScriptVisitor) accept((ParadoxScriptVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<ParadoxScriptInlineMathExpression> getInlineMathExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ParadoxScriptInlineMathExpression.class);
  }

  @Override
  @NotNull
  public List<ParadoxScriptInlineMathFactor> getInlineMathFactorList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ParadoxScriptInlineMathFactor.class);
  }

}
