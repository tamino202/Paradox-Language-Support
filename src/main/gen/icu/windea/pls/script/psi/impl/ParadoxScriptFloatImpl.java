// This is a generated file. Not intended for manual editing.
package icu.windea.pls.script.psi.impl;

import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import icu.windea.pls.script.psi.*;
import icu.windea.pls.model.ParadoxValueType;

public class ParadoxScriptFloatImpl extends ParadoxScriptNumberImpl implements ParadoxScriptFloat {

  public ParadoxScriptFloatImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull ParadoxScriptVisitor visitor) {
    visitor.visitFloat(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ParadoxScriptVisitor) accept((ParadoxScriptVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  public float getFloatValue() {
    return ParadoxScriptPsiImplUtil.getFloatValue(this);
  }

  @Override
  @NotNull
  public ParadoxValueType getValueType() {
    return ParadoxScriptPsiImplUtil.getValueType(this);
  }

}
