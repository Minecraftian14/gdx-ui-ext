package `in`.mcxiv.grid.vis.ktx

import com.badlogic.gdx.utils.Align
import `in`.mcxiv.grid.Grid.ColumnFit
import `in`.mcxiv.grid.vis.VisGrid
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.defaultStyle
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Scene2dDsl
@OptIn(ExperimentalContracts::class)
inline fun <S> KWidget<S>.visGrid(
    items: Array<Array<Any>>, alignment: Int = Align.left, columnFit: ColumnFit = ColumnFit.EXACT,
    style: String = defaultStyle,
    init: (@Scene2dDsl VisGrid).(S) -> Unit = {},
): VisGrid {
    contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
    return actor(VisGrid(items, alignment, columnFit, style), init)
}
