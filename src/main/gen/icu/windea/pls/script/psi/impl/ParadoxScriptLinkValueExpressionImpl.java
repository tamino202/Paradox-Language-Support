// This is a generated file. Not intended for manual editing.
package icu.windea.pls.script.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static icu.windea.pls.script.psi.ParadoxScriptElementTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import icu.windea.pls.script.psi.*;

public class ParadoxScriptLinkValueExpressionImpl extends ASTWrapperPsiElement implements ParadoxScriptLinkValueExpression {

  public ParadoxScriptLinkValueExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull ParadoxScriptVisitor visitor) {
    visitor.visitLinkValueExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ParadoxScriptVisitor) accept((ParadoxScriptVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public ParadoxScriptLinkValue getLinkValue() {
    return notNullChild(PsiTreeUtil.getChildOfType(this, ParadoxScriptLinkValue.class));
  }

  @Override
  @NotNull
  public ParadoxScriptLinkValuePrefix getLinkValuePrefix() {
    return notNullChild(PsiTreeUtil.getChildOfType(this, ParadoxScriptLinkValuePrefix.class));
  }

}
