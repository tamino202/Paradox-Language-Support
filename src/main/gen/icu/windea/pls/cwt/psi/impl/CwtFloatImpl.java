// This is a generated file. Not intended for manual editing.
package icu.windea.pls.cwt.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static icu.windea.pls.cwt.psi.CwtTypes.*;
import icu.windea.pls.cwt.psi.*;

public class CwtFloatImpl extends CwtNumberImpl implements CwtFloat {

  public CwtFloatImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CwtVisitor visitor) {
    visitor.visitFloat(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CwtVisitor) accept((CwtVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public PsiElement getFloatToken() {
    return findNotNullChildByType(FLOAT_TOKEN);
  }

  @Override
  public float getFloatValue() {
    return CwtPsiImplUtil.getFloatValue(this);
  }

}
