package `in`.mcxiv.date.vis.ktx

import `in`.mcxiv.date.vis.VisDatePicker
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import java.time.format.TextStyle
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Scene2dDsl
@OptIn(ExperimentalContracts::class)
inline fun <S> KWidget<S>.visDatePicker(
    locale: Locale = Locale.getDefault(), yearMinimum: Int = 0, yearMaximum: Int = 0,
    style: String = "default", yearStyle: TextStyle = TextStyle.FULL_STANDALONE, monthStyle : TextStyle = TextStyle.SHORT,
    init: (@Scene2dDsl VisDatePicker).(S) -> Unit = {},
): VisDatePicker {
    contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
    return actor(VisDatePicker(locale, yearMinimum, yearMaximum, style, yearStyle, monthStyle), init)
}
