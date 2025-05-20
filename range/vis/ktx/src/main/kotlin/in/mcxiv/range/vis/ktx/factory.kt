package `in`.mcxiv.range.vis.ktx

import `in`.mcxiv.range.vis.VisRangeSlider
import ktx.scene2d.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Scene2dDsl
@OptIn(ExperimentalContracts::class)
inline fun <S> KWidget<S>.visRangeSlider(
    min: Float = 0f, max: Float = 1f, steps: Float = 0.01f,
    vertical: Boolean = false,
    style: String = if (vertical) defaultVerticalStyle else defaultHorizontalStyle,
    init: (@Scene2dDsl VisRangeSlider).(S) -> Unit = {},
): VisRangeSlider {
    contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
    return actor(VisRangeSlider(min, max, steps, vertical, style), init)
}
