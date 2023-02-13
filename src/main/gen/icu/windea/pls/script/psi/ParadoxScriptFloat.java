// This is a generated file. Not intended for manual editing.
package icu.windea.pls.script.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.ContributedReferenceHost;
import icu.windea.pls.core.expression.ParadoxDataType;

public interface ParadoxScriptFloat extends ParadoxScriptValue, PsiLiteralValue, ContributedReferenceHost {

  @NotNull
  String getName();

  @NotNull
  String getValue();

  @NotNull
  ParadoxScriptValue setValue(@NotNull String name);

  float getFloatValue();

  @NotNull
  ParadoxDataType getType();

}
