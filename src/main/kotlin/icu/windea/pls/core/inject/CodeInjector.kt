package icu.windea.pls.core.inject

import com.intellij.openapi.extensions.*
import javassist.*

/**
 * 用于在运行时动态修改第三方代码。
 */
interface CodeInjector {
    fun inject(pool: ClassPool)
    
    companion object {
        @JvmField val EP_NAME = ExtensionPointName.create<CodeInjector>("icu.windea.pls.codeInjector")
    }
}

