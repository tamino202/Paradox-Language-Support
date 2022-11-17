// This is a generated file. Not intended for manual editing.
package icu.windea.pls.cwt.psi.impl;

import com.intellij.lang.*;
import com.intellij.psi.*;
import icu.windea.pls.cwt.psi.*;
import org.jetbrains.annotations.*;

public class CwtIntImpl extends CwtValueImpl implements CwtInt {

  public CwtIntImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CwtVisitor visitor) {
    visitor.visitInt(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CwtVisitor) accept((CwtVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  public int getIntValue() {
    return CwtPsiImplUtil.getIntValue(this);
  }

}
