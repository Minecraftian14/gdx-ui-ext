package in.mcxiv.range.vis;

import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.kotcrab.vis.ui.VisUI;
import in.mcxiv.range.RangeSlider;

public class VisRangeSlider extends RangeSlider {
    public VisRangeSlider(float min, float max, float stepSize, boolean vertical) {
        super(min, max, stepSize, vertical, VisUI.getSkin());
    }

    public VisRangeSlider(float min, float max, float stepSize, boolean vertical, String styleName) {
        super(min, max, stepSize, vertical, VisUI.getSkin(), styleName);
    }

    public VisRangeSlider(float min, float max, float stepSize, boolean vertical, Slider.SliderStyle style) {
        super(min, max, stepSize, vertical, style);
    }
}
