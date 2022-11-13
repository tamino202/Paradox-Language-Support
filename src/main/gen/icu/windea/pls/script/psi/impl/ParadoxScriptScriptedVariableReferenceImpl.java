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
import com.intellij.openapi.util.Iconable.IconFlags;
import icu.windea.pls.core.expression.ParadoxDataType;
import icu.windea.pls.script.reference.ParadoxScriptedVariableReference;
import javax.swing.Icon;

public class ParadoxScriptScriptedVariableReferenceImpl extends ParadoxScriptValueImpl implements ParadoxScriptScriptedVariableReference {

  public ParadoxScriptScriptedVariableReferenceImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull ParadoxScriptVisitor visitor) {
    visitor.visitScriptedVariableReference(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ParadoxScriptVisitor) accept((ParadoxScriptVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public Icon getIcon(@IconFlags int flags) {
    return ParadoxScriptPsiImplUtil.getIcon(this, flags);
  }

  @Override
  @NotNull
  public String getName() {
    return ParadoxScriptPsiImplUtil.getName(this);
  }

  @Override
  @NotNull
  public ParadoxScriptScriptedVariableReference setName(@NotNull String name) {
    return ParadoxScriptPsiImplUtil.setName(this, name);
  }

  @Override
  @NotNull
  public ParadoxScriptedVariableReference getReference() {
    return ParadoxScriptPsiImplUtil.getReference(this);
  }

  @Override
  @NotNull
  public ParadoxDataType getExpressionType() {
    return ParadoxScriptPsiImplUtil.getExpressionType(this);
  }

}
