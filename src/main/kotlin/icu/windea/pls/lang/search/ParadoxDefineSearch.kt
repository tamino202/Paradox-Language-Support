package icu.windea.pls.lang.search

import com.intellij.openapi.extensions.*
import com.intellij.psi.search.searches.*
import com.intellij.util.*
import icu.windea.pls.core.*
import icu.windea.pls.lang.search.selector.*
import icu.windea.pls.model.indexInfo.*

/**
 * 预定义的命名空间与变量的查询。
 */
class ParadoxDefineSearch : ExtensibleQueryFactory<ParadoxDefineIndexInfo.Compact, ParadoxDefineSearch.SearchParameters>(EP_NAME) {
    /**
     * @property namespace 命名空间。
     * @property variable 变量名。
     */
    class SearchParameters(
        val namespace: String?,
        val variable: String?,
        override val selector: ChainedParadoxSelector<ParadoxDefineIndexInfo.Compact>
    ) : ParadoxSearchParameters<ParadoxDefineIndexInfo.Compact>

    companion object {
        @JvmField
        val EP_NAME = ExtensionPointName.create<QueryExecutor<ParadoxDefineIndexInfo.Compact, SearchParameters>>("icu.windea.pls.search.defineSearch")
        @JvmField
        val INSTANCE = ParadoxDefineSearch()

        /**
         *  @see icu.windea.pls.lang.search.ParadoxDefineSearch.SearchParameters
         */
        @JvmStatic
        fun search(
            namespace: String?,
            variable: String?,
            selector: ChainedParadoxSelector<ParadoxDefineIndexInfo.Compact>
        ): ParadoxQuery<ParadoxDefineIndexInfo.Compact, SearchParameters> {
            return INSTANCE.createParadoxQuery(SearchParameters(namespace, variable, selector))
        }

        /**
         * @param expression 以点分隔的命名空间与变量名。如，`NAMESPACE.Variable`。
         */
        fun search(
            expression: String,
            selector: ChainedParadoxSelector<ParadoxDefineIndexInfo.Compact>
        ): ParadoxQuery<ParadoxDefineIndexInfo.Compact, SearchParameters> {
            val (namespace, variable) = expression.splitToPair('.') ?: tupleOf(expression, null)
            return INSTANCE.createParadoxQuery(SearchParameters(namespace, variable, selector))
        }
    }
}
