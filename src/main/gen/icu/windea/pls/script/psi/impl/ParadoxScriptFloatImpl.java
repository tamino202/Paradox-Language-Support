// This is a generated file. Not intended for manual editing.
package icu.windea.pls.script.psi.impl;

import com.intellij.lang.*;
import com.intellij.navigation.*;
import com.intellij.psi.*;
import com.intellij.psi.search.*;
import icu.windea.pls.lang.model.*;
import icu.windea.pls.script.psi.*;
import org.jetbrains.annotations.*;

public class ParadoxScriptFloatImpl extends ParadoxScriptValueImpl implements ParadoxScriptFloat {

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
  @NotNull
  public String getName() {
    return ParadoxScriptPsiImplUtil.getName(this);
  }

  @Override
  @NotNull
  public String getValue() {
    return ParadoxScriptPsiImplUtil.getValue(this);
  }

  @Override
  public float getFloatValue() {
    return ParadoxScriptPsiImplUtil.getFloatValue(this);
  }

  @Override
  @NotNull
  public ParadoxType getType() {
    return ParadoxScriptPsiImplUtil.getType(this);
  }

  @Override
  @NotNull
  public ItemPresentation getPresentation() {
    return ParadoxScriptPsiImplUtil.getPresentation(this);
  }

  @Override
  @NotNull
  public GlobalSearchScope getResolveScope() {
    return ParadoxScriptPsiImplUtil.getResolveScope(this);
  }

  @Override
  @NotNull
  public SearchScope getUseScope() {
    return ParadoxScriptPsiImplUtil.getUseScope(this);
  }

}
