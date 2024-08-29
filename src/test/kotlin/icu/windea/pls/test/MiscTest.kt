package icu.windea.pls.test

import icu.windea.pls.core.*
import org.junit.Test

class MiscTest {
    @Test
    fun test() {
        foo()
    }
    
    
    private fun foo() {
        val a = Thread.currentThread().stackTrace
        println(a)
        withRecursionGuard {
            println()
        }
        println()
        println()
        withRecursionGuard {
            println()
            println()
        }
    }
}