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

public class ParadoxScriptRootBlockImpl extends ASTWrapperPsiElement implements ParadoxScriptRootBlock {

  public ParadoxScriptRootBlockImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull ParadoxScriptVisitor visitor) {
    visitor.visitRootBlock(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ParadoxScriptVisitor) accept((ParadoxScriptVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<ParadoxScriptBlock> getBlockList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ParadoxScriptBlock.class);
  }

  @Override
  @NotNull
  public List<ParadoxScriptBoolean> getBooleanList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ParadoxScriptBoolean.class);
  }

  @Override
  @NotNull
  public List<ParadoxScriptColor> getColorList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ParadoxScriptColor.class);
  }

  @Override
  @NotNull
  public List<ParadoxScriptFloat> getFloatList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ParadoxScriptFloat.class);
  }

  @Override
  @NotNull
  public List<ParadoxScriptInlineMath> getInlineMathList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ParadoxScriptInlineMath.class);
  }

  @Override
  @NotNull
  public List<ParadoxScriptInt> getIntList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ParadoxScriptInt.class);
  }

  @Override
  @NotNull
  public List<ParadoxScriptProperty> getPropertyList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ParadoxScriptProperty.class);
  }

  @Override
  @NotNull
  public List<ParadoxScriptString> getStringList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ParadoxScriptString.class);
  }

  @Override
  @NotNull
  public List<ParadoxScriptVariable> getVariableList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ParadoxScriptVariable.class);
  }

  @Override
  @NotNull
  public List<ParadoxScriptVariableReference> getVariableReferenceList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ParadoxScriptVariableReference.class);
  }

  @Override
  @NotNull
  public String getValue() {
    return ParadoxScriptPsiImplUtil.getValue(this);
  }

  @Override
  public boolean isEmpty() {
    return ParadoxScriptPsiImplUtil.isEmpty(this);
  }

  @Override
  public boolean isNotEmpty() {
    return ParadoxScriptPsiImplUtil.isNotEmpty(this);
  }

  @Override
  @NotNull
  public List<ParadoxScriptValue> getValueList() {
    return ParadoxScriptPsiImplUtil.getValueList(this);
  }

  @Override
  @NotNull
  public List<PsiElement> getComponents() {
    return ParadoxScriptPsiImplUtil.getComponents(this);
  }

}
