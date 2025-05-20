package in.mcxiv.range;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.Pools;

public class RangeSlider extends RangeBar {
    int button = -1; // -1 is nothing happening
    int draggingPointer = -1; // not -1 is touch down, -1 is touch up
    int leftOrRight = -1; // 0 is left, 1 is right.
    boolean mouseOver;
    private Interpolation visualInterpolationInverse = Interpolation.linear;
    private float[] snapValues;
    private float threshold;

    public RangeSlider(float min, float max, float stepSize, boolean vertical, Skin skin) {
        this(min, max, stepSize, vertical, skin.get("default-" + (vertical ? "vertical" : "horizontal"), SliderStyle.class));
    }

    public RangeSlider(float min, float max, float stepSize, boolean vertical, Skin skin, String styleName) {
        this(min, max, stepSize, vertical, skin.get(styleName, SliderStyle.class));
    }

    public RangeSlider(float min, float max, float stepSize, boolean vertical, SliderStyle style) {
        super(min, max, stepSize, vertical, style);

        addListener(new InputListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (disabled) return false;
                if (RangeSlider.this.button != -1 && RangeSlider.this.button != button) return false;
                if (draggingPointer != -1) return false;
                draggingPointer = pointer;
//                if (whoIsTheCloset(x, y) == 0) calculatePositionAndValueL(x, y);
//                else calculatePositionAndValueR(x, y);
                calculatePositionAndAppropriatelySetTheValue(x, y);
                return true;
            }

            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (pointer != draggingPointer) return;
                draggingPointer = -1;
                // The position is invalid when focus is cancelled
                if (event.isTouchFocusCancel() || !calculatePositionAndAppropriatelySetTheValue(x, y)) {
                    // Fire an event on touchUp even if the value didn't change, so listeners can see when a drag ends via isDragging.
                    ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class);
                    fire(changeEvent);
                    Pools.free(changeEvent);
                }
                leftOrRight = -1;
            }

            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                calculatePositionAndAppropriatelySetTheValue(x, y);
            }

            public void enter(InputEvent event, float x, float y, int pointer, @Null Actor fromActor) {
                if (pointer == -1) mouseOver = true;
            }

            public void exit(InputEvent event, float x, float y, int pointer, @Null Actor toActor) {
                if (pointer == -1) mouseOver = false;
            }
        });
    }

    public SliderStyle getStyle() {
        return (SliderStyle) super.getStyle();
    }

    public boolean isOver() {
        return mouseOver;
    }

    protected @Null Drawable getBackgroundDrawable() {
        SliderStyle style = (SliderStyle) super.getStyle();
        if (disabled && style.disabledBackground != null) return style.disabledBackground;
        if (isDragging() && style.backgroundDown != null) return style.backgroundDown;
        if (mouseOver && style.backgroundOver != null) return style.backgroundOver;
        return style.background;
    }

    protected @Null Drawable getKnobDrawable() {
        SliderStyle style = (SliderStyle) super.getStyle();
        if (disabled && style.disabledKnob != null) return style.disabledKnob;
        if (isDragging() && style.knobDown != null) return style.knobDown;
        if (mouseOver && style.knobOver != null) return style.knobOver;
        return style.knob;
    }

    protected Drawable getKnobBeforeDrawable() {
        SliderStyle style = (SliderStyle) super.getStyle();
        if (disabled && style.disabledKnobBefore != null) return style.disabledKnobBefore;
        if (isDragging() && style.knobBeforeDown != null) return style.knobBeforeDown;
        if (mouseOver && style.knobBeforeOver != null) return style.knobBeforeOver;
        return style.knobBefore;
    }

    protected Drawable getKnobAfterDrawable() {
        SliderStyle style = (SliderStyle) super.getStyle();
        if (disabled && style.disabledKnobAfter != null) return style.disabledKnobAfter;
        if (isDragging() && style.knobAfterDown != null) return style.knobAfterDown;
        if (mouseOver && style.knobAfterOver != null) return style.knobAfterOver;
        return style.knobAfter;
    }

    int whoIsTheCloset(float x, float y) {
        SliderStyle style = getStyle();
        Drawable knob = style.knob;
        Drawable bg = getBackgroundDrawable();
        float position;

        if (vertical) {
            float height = getHeight() - bg.getTopHeight() - bg.getBottomHeight();
            float knobHeight = knob == null ? 0 : knob.getMinHeight();
            position = y - bg.getBottomHeight() - knobHeight * 0.5f;
            position = Math.max(Math.min(0, bg.getBottomHeight()), position);
            position = Math.min(height - knobHeight, position);
        } else {
            float width = getWidth() - bg.getLeftWidth() - bg.getRightWidth();
            float knobWidth = knob == null ? 0 : knob.getMinWidth();
            position = x - bg.getLeftWidth() - knobWidth * 0.5f;
            position = Math.max(Math.min(0, bg.getLeftWidth()), position);
            position = Math.min(width - knobWidth, position);
        }
        if (Math.abs(positionL - position) < Math.abs(positionR - position)) return 0;
        else return 1;
    }

    boolean calculatePositionAndValueL(float x, float y) {
        SliderStyle style = getStyle();
        Drawable knob = style.knob;
        Drawable bg = getBackgroundDrawable();

        float value;
        float oldPosition = positionL;

        float min = getMinValue();
        float max = getMaxValue();

        if (vertical) {
            float height = getHeight() - bg.getTopHeight() - bg.getBottomHeight();
            float knobHeight = knob == null ? 0 : knob.getMinHeight();
            positionL = y - bg.getBottomHeight() - knobHeight * 0.5f;
            value = min + (max - min) * visualInterpolationInverse.apply(positionL / (height - knobHeight));
            positionL = Math.max(Math.min(0, bg.getBottomHeight()), positionL);
            positionL = Math.min(height - knobHeight, positionL);
        } else {
            float width = getWidth() - bg.getLeftWidth() - bg.getRightWidth();
            float knobWidth = knob == null ? 0 : knob.getMinWidth();
            positionL = x - bg.getLeftWidth() - knobWidth * 0.5f;
            value = min + (max - min) * visualInterpolationInverse.apply(positionL / (width - knobWidth));
            positionL = Math.max(Math.min(0, bg.getLeftWidth()), positionL);
            positionL = Math.min(width - knobWidth, positionL);
        }

        float oldValue = value;
        if (!Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) && !Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT)) value = snap(value);
        boolean valueSet = setValueL(value);
        if (value == oldValue) positionL = oldPosition;
        return valueSet;
    }

    boolean calculatePositionAndValueR(float x, float y) {
        SliderStyle style = getStyle();
        Drawable knob = style.knob;
        Drawable bg = getBackgroundDrawable();

        float value;
        float oldPosition = positionR;

        float min = getMinValue();
        float max = getMaxValue();

        if (vertical) {
            float height = getHeight() - bg.getTopHeight() - bg.getBottomHeight();
            float knobHeight = knob == null ? 0 : knob.getMinHeight();
            positionR = y - bg.getBottomHeight() - knobHeight * 0.5f;
            value = min + (max - min) * visualInterpolationInverse.apply(positionR / (height - knobHeight));
            positionR = Math.max(Math.min(0, bg.getBottomHeight()), positionR);
            positionR = Math.min(height - knobHeight, positionR);
        } else {
            float width = getWidth() - bg.getLeftWidth() - bg.getRightWidth();
            float knobWidth = knob == null ? 0 : knob.getMinWidth();
            positionR = x - bg.getLeftWidth() - knobWidth * 0.5f;
            value = min + (max - min) * visualInterpolationInverse.apply(positionR / (width - knobWidth));
            positionR = Math.max(Math.min(0, bg.getLeftWidth()), positionR);
            positionR = Math.min(width - knobWidth, positionR);
        }

        float oldValue = value;
        if (!Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) && !Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT)) value = snap(value);
        boolean valueSet = setValueL(value);
        if (value == oldValue) positionR = oldPosition;
        return valueSet;
    }

    boolean calculatePositionAndAppropriatelySetTheValue(float x, float y) {
        Drawable knob = style.knob;
        Drawable bg = getBackgroundDrawable();

        float newValue;
        float newPosition;

        if (vertical) {
            float height = getHeight() - bg.getTopHeight() - bg.getBottomHeight();
            float knobHeight = knob == null ? 0 : knob.getMinHeight();
            newPosition = y - bg.getBottomHeight() - knobHeight * 0.5f;
            newValue = min + (max - min) * visualInterpolationInverse.apply(newPosition / (height - knobHeight));
            newPosition = Math.max(Math.min(0, bg.getBottomHeight()), newPosition);
            newPosition = Math.min(height - knobHeight, newPosition);
        } else {
            float width = getWidth() - bg.getLeftWidth() - bg.getRightWidth();
            float knobWidth = knob == null ? 0 : knob.getMinWidth();
            newPosition = x - bg.getLeftWidth() - knobWidth * 0.5f;
            newValue = min + (max - min) * visualInterpolationInverse.apply(newPosition / (width - knobWidth));
            newPosition = Math.max(Math.min(0, bg.getLeftWidth()), newPosition);
            newPosition = Math.min(width - knobWidth, newPosition);
        }

        if ((leftOrRight == 1) || ((leftOrRight == -1) && Math.abs(positionL - newPosition) < Math.abs(positionR - newPosition))) {
            leftOrRight = 1;
            float oldPosition = positionL;
            positionL = newPosition;
            float oldValue = newValue;
            if (!Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) && !Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT)) newValue = snap(newValue);
            boolean valueSet = setValueL(newValue);
            if (newValue == oldValue) positionL = oldPosition;
            return valueSet;
        } else {
            leftOrRight = 0;
            float oldPosition = positionR;
            positionR = newPosition;
            float oldValue = newValue;
            if (!Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) && !Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT)) newValue = snap(newValue);
            boolean valueSet = setValueR(newValue);
            if (newValue == oldValue) positionR = oldPosition;
            return valueSet;
        }
    }

    protected float snap(float value) {
        if (snapValues == null || snapValues.length == 0) return value;
        float bestDiff = -1, bestValue = 0;
        for (int i = 0; i < snapValues.length; i++) {
            float snapValue = snapValues[i];
            float diff = Math.abs(value - snapValue);
            if (diff <= threshold) {
                if (bestDiff == -1 || diff < bestDiff) {
                    bestDiff = diff;
                    bestValue = snapValue;
                }
            }
        }
        return bestDiff == -1 ? value : bestValue;
    }

    public void setSnapToValues(float threshold, @Null float... values) {
        if (values != null && values.length == 0) throw new IllegalArgumentException("values cannot be empty.");
        this.snapValues = values;
        this.threshold = threshold;
    }

    @Deprecated
    public void setSnapToValues(@Null float[] values, float threshold) {
        setSnapToValues(threshold, values);
    }

    public @Null float[] getSnapToValues() {
        return snapValues;
    }

    public float getSnapToValuesThreshold() {
        return threshold;
    }


    public boolean isDragging() {
        return draggingPointer != -1;
    }


    public void setButton(int button) {
        this.button = button;
    }

    public void setVisualInterpolationInverse(Interpolation interpolation) {
        this.visualInterpolationInverse = interpolation;
    }

    public void setVisualPercent(float percent) {
        setValueL(min + (max - min) * visualInterpolationInverse.apply(percent));
    }

}
