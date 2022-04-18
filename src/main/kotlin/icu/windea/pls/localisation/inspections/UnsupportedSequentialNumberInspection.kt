package icu.windea.pls.localisation.inspections

import com.intellij.codeInspection.*;
import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.localisation.psi.*

private fun _description(name: String) = PlsBundle.message("localisation.inspection.unsupportedSequentialNumber.description", name)

/**
 * 不支持的序列数的检查。
 */
class UnsupportedSequentialNumberInspection : LocalInspectionTool() {
	override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
		return Visitor(holder)
	}
	
	private class Visitor(private val holder: ProblemsHolder) : ParadoxLocalisationVisitor() {
		override fun visitSequentialNumber(element: ParadoxLocalisationSequentialNumber) {
			val sequentialNumberConfig = element.sequentialNumberConfig
			if(sequentialNumberConfig != null) return
			val location = element.sequentialNumberId ?: return
			holder.registerProblem(location, _description(element.name))
		}
	}
}
