package icu.windea.pls.lang.documentation

import com.intellij.codeInsight.documentation.*
import com.intellij.platform.backend.documentation.*
import com.intellij.platform.backend.documentation.DocumentationLinkHandler
import com.intellij.psi.*
import icu.windea.pls.core.*
import icu.windea.pls.cwt.*
import icu.windea.pls.ep.documentation.*

class CwtDocumentationLinkHandler: DocumentationLinkHandler {
    override fun resolveLink(target: DocumentationTarget, url: String): LinkResolveResult? {
        if(target !is CwtDocumentationTarget) return null
        val link = url.removePrefixOrNull(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL) ?: return null
        val resolved = ParadoxDocumentationLinkProvider.resolve(link, target.element) ?: return null
        val documentationTarget = when {
            resolved is PsiFileSystemItem -> return null
            resolved.language.isParadoxLanguage() -> ParadoxDocumentationTarget(resolved, null)
            resolved.language == CwtLanguage -> CwtDocumentationTarget(resolved, null)
            else -> return null
        }
        return LinkResolveResult.resolvedTarget(documentationTarget)
    }
}
