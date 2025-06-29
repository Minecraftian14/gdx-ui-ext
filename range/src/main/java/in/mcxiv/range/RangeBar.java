package in.mcxiv.range;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.Pools;

public class RangeBar extends Widget implements Disableable {
    protected ProgressBarStyle style;
    float min, max, stepSize;
    protected float valueL, animateFromValueL;
    protected float valueR, animateFromValueR;
    float positionL;
    float positionR;
    final boolean vertical;
    protected float animateDuration, animateTime;
    protected Interpolation animateInterpolation = Interpolation.linear, visualInterpolation = Interpolation.linear;
    boolean disabled;
    protected boolean round = true, programmaticChangeEvents = true;

    public RangeBar(float min, float max, float stepSize, boolean vertical, Skin skin) {
        this(min, max, stepSize, vertical, skin.get("default-" + (vertical ? "vertical" : "horizontal"), ProgressBarStyle.class));
    }

    public RangeBar(float min, float max, float stepSize, boolean vertical, Skin skin, String styleName) {
        this(min, max, stepSize, vertical, skin.get(styleName, ProgressBarStyle.class));
    }

    public RangeBar(float min, float max, float stepSize, boolean vertical, ProgressBarStyle style) {
        if (min > max) throw new IllegalArgumentException("max must be > min. min,max: " + min + ", " + max);
        if (stepSize <= 0) throw new IllegalArgumentException("stepSize must be > 0: " + stepSize);
        setStyle(style);
        this.min = min;
        this.max = max;
        this.stepSize = stepSize;
        this.vertical = vertical;
        this.valueL = min;
        this.valueR = max;
        setSize(getPrefWidth(), getPrefHeight());
    }

    public void setStyle(ProgressBarStyle style) {
        if (style == null) throw new IllegalArgumentException("style cannot be null.");
        this.style = style;
        invalidateHierarchy();
    }

    public ProgressBarStyle getStyle() {
        return style;
    }

    public void act(float delta) {
        super.act(delta);
        if (animateTime > 0) {
            animateTime -= delta;
            Stage stage = getStage();
            if (stage != null && stage.getActionsRequestRendering()) Gdx.graphics.requestRendering();
        }
    }

    public void draw(Batch batch, float parentAlpha) {
        ProgressBarStyle style = this.style;
        Drawable knob = style.knob, currentKnob = getKnobDrawable();
        Drawable bg = getBackgroundDrawable();
        Drawable knobBefore = getKnobBeforeDrawable();
        Drawable knobAfter = getKnobAfterDrawable();

        Color color = getColor();
        float x = getX(), y = getY();
        float width = getWidth(), height = getHeight();
        float knobHeight = knob == null ? 0 : knob.getMinHeight();
        float knobWidth = knob == null ? 0 : knob.getMinWidth();
        float percentL = getVisualPercentL();
        float percentR = getVisualPercentR();

        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

        if (vertical) {
            drawVertical(batch, bg, x, width, y, height, knobHeight, percentL, knobBefore, knobAfter, currentKnob);
        } else {
            drawHorizontal(batch, bg, x, y, height, width, knobWidth, percentL, percentR, knobBefore, knobAfter, currentKnob);
        }
    }

    private void drawVertical(Batch batch, Drawable bg, float x, float width, float y, float height, float knobHeight, float percentL, Drawable knobBefore, Drawable knobAfter, Drawable currentKnob) {
        float bgTopHeight = 0, bgBottomHeight = 0;
        if (bg != null) {
            drawRound(batch, bg, x + (width - bg.getMinWidth()) * 0.5f, y, bg.getMinWidth(), height);
            bgTopHeight = bg.getTopHeight();
            bgBottomHeight = bg.getBottomHeight();
            height -= bgTopHeight + bgBottomHeight;
        }

        float total = height - knobHeight;
        float beforeHeight = MathUtils.clamp(total * percentL, 0, total);
        positionL = bgBottomHeight + beforeHeight;

        float knobHeightHalf = knobHeight * 0.5f;
        if (knobBefore != null) {
            drawRound(batch, knobBefore, //
                x + (width - knobBefore.getMinWidth()) * 0.5f, //
                y + bgBottomHeight, //
                knobBefore.getMinWidth(), beforeHeight + knobHeightHalf);
        }
        if (knobAfter != null) {
            drawRound(batch, knobAfter, //
                x + (width - knobAfter.getMinWidth()) * 0.5f, //
                y + positionL + knobHeightHalf, //
                knobAfter.getMinWidth(),
                total - (round ? (float) Math.ceil(beforeHeight - knobHeightHalf) : beforeHeight - knobHeightHalf));
        }
        if (currentKnob != null) {
            float w = currentKnob.getMinWidth(), h = currentKnob.getMinHeight();
            drawRound(batch, currentKnob, //
                x + (width - w) * 0.5f, //
                y + positionL + (knobHeight - h) * 0.5f, //
                w, h);
        }
    }

    private void drawHorizontal(Batch batch, Drawable bg, float x, float y, float height, float width, float knobWidth, float percentL, float percentR, Drawable knobBefore, Drawable knobAfter, Drawable currentKnob) {
        float bgLeftWidth = 0, bgRightWidth = 0;
        if (bg != null) {
            drawRound(batch, bg, x, Math.round(y + (height - bg.getMinHeight()) * 0.5f), width, Math.round(bg.getMinHeight()));
            bgLeftWidth = bg.getLeftWidth();
            bgRightWidth = bg.getRightWidth();
            width -= bgLeftWidth + bgRightWidth;
        }

        float total = width - knobWidth;
        float knobWidthHalf = knobWidth * 0.5f;
        float beforeWidthL = MathUtils.clamp(total * percentL, 0, total);
        positionL = bgLeftWidth + beforeWidthL;
        drawHorizontalKnob(batch, x, y, height, knobWidth, knobBefore, knobAfter, currentKnob, bgLeftWidth, beforeWidthL, positionL, knobWidthHalf, total);
        float beforeWidthR = MathUtils.clamp(total * percentR, 0, total);
        positionR = bgLeftWidth + beforeWidthR;
        drawHorizontalKnob(batch, x, y, height, knobWidth, knobBefore, knobAfter, currentKnob, bgLeftWidth, beforeWidthR, positionR, knobWidthHalf, total);
    }

    private void drawHorizontalKnob(Batch batch, float x, float y, float height, float knobWidth, Drawable knobBefore, Drawable knobAfter, Drawable currentKnob, float bgLeftWidth, float beforeWidth, float position, float knobWidthHalf, float total) {
        if (knobBefore != null) {
            drawRound(batch, knobBefore, //
                x + bgLeftWidth, //
                y + (height - knobBefore.getMinHeight()) * 0.5f, //
                beforeWidth + knobWidthHalf, knobBefore.getMinHeight());
        }
        if (knobAfter != null) {
            drawRound(batch, knobAfter, //
                x + position + knobWidthHalf, //
                y + (height - knobAfter.getMinHeight()) * 0.5f, //
                total - (round ? (float) Math.ceil(beforeWidth - knobWidthHalf) : beforeWidth - knobWidthHalf),
                knobAfter.getMinHeight());
        }
        if (currentKnob != null) {
            float w = currentKnob.getMinWidth(), h = currentKnob.getMinHeight();
            drawRound(batch, currentKnob, //
                x + position + (knobWidth - w) * 0.5f, //
                y + (height - h) * 0.5f, //
                w, h);
        }
    }

    protected void drawRound(Batch batch, Drawable drawable, float x, float y, float w, float h) {
        if (round) {
            x = (float) Math.floor(x);
            y = (float) Math.floor(y);
            w = (float) Math.ceil(w);
            h = (float) Math.ceil(h);
        }
        drawable.draw(batch, x, y, w, h);
    }

    public float getValueL() {
        return valueL;
    }

    public float getValueR() {
        return valueR;
    }

    public float getVisualValueL() {
        if (animateTime > 0) return animateInterpolation.apply(animateFromValueL, valueL, 1 - animateTime / animateDuration);
        return valueL;
    }

    public float getVisualValueR() {
        if (animateTime > 0) return animateInterpolation.apply(animateFromValueR, valueR, 1 - animateTime / animateDuration);
        return valueR;
    }

    public void updateVisualValue() {
        animateTime = 0;
    }

    public float getPercentL() {
        if (min == max) return 0;
        return (valueL - min) / (max - min);
    }

    public float getPercentR() {
        if (min == max) return 0;
        return (valueR - min) / (max - min);
    }

    public float getVisualPercentL() {
        if (min == max) return 0;
        return visualInterpolation.apply((getVisualValueL() - min) / (max - min));
    }

    public float getVisualPercentR() {
        if (min == max) return 0;
        return visualInterpolation.apply((getVisualValueR() - min) / (max - min));
    }

    protected @Null Drawable getBackgroundDrawable() {
        if (disabled && style.disabledBackground != null) return style.disabledBackground;
        return style.background;
    }

    protected @Null Drawable getKnobDrawable() {
        if (disabled && style.disabledKnob != null) return style.disabledKnob;
        return style.knob;
    }

    protected Drawable getKnobBeforeDrawable() {
        if (disabled && style.disabledKnobBefore != null) return style.disabledKnobBefore;
        return style.knobBefore;
    }

    protected Drawable getKnobAfterDrawable() {
        if (disabled && style.disabledKnobAfter != null) return style.disabledKnobAfter;
        return style.knobAfter;
    }

    protected float getKnobPositionL() {
        return this.positionL;
    }

    protected float getKnobPositionR() {
        return this.positionR;
    }

    public boolean setValueL(float valueL) {
        valueL = clamp(round(valueL));
        float oldValue = this.valueL;
        if (valueL == oldValue) return false;
        float oldVisualValueL = getVisualValueL();
        this.valueL = valueL;

        if (programmaticChangeEvents) {
            ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class);
            boolean cancelled = fire(changeEvent);
            Pools.free(changeEvent);
            if (cancelled) {
                this.valueL = oldValue;
                return false;
            }
        }

        if (animateDuration > 0) {
            animateFromValueL = oldVisualValueL;
            animateTime = animateDuration;
        }
        return true;
    }

    public boolean setValueR(float valueR) {
        valueR = clamp(round(valueR));
        float oldValue = this.valueR;
        if (valueR == oldValue) return false;
        float oldVisualValueR = getVisualValueR();
        this.valueR = valueR;

        if (programmaticChangeEvents) {
            ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class);
            boolean cancelled = fire(changeEvent);
            Pools.free(changeEvent);
            if (cancelled) {
                this.valueR = oldValue;
                return false;
            }
        }

        if (animateDuration > 0) {
            animateFromValueR = oldVisualValueR;
            animateTime = animateDuration;
        }
        return true;
    }

    protected float round(float value) {
        return Math.round(value / stepSize) * stepSize;
    }

    protected float clamp(float value) {
        return MathUtils.clamp(value, min, max);
    }

    public void setRange(float min, float max) {
        if (min > max) throw new IllegalArgumentException("min must be <= max: " + min + " <= " + max);
        this.min = min;
        this.max = max;
        if (valueL < min)
            setValueL(min);
        else if (valueL > max) //
            setValueL(max);
        if (valueR < min)
            setValueR(min);
        else if (valueR > max) //
            setValueR(max);
    }

    public void setStepSize(float stepSize) {
        if (stepSize <= 0) throw new IllegalArgumentException("steps must be > 0: " + stepSize);
        this.stepSize = stepSize;
    }

    public float getPrefWidth() {
        if (vertical) {
            Drawable knob = style.knob, bg = getBackgroundDrawable();
            return Math.max(knob == null ? 0 : knob.getMinWidth(), bg == null ? 0 : bg.getMinWidth());
        } else
            return 140;
    }

    public float getPrefHeight() {
        if (vertical)
            return 140;
        else {
            Drawable knob = style.knob, bg = getBackgroundDrawable();
            return Math.max(knob == null ? 0 : knob.getMinHeight(), bg == null ? 0 : bg.getMinHeight());
        }
    }

    public float getMinValue() {
        return this.min;
    }

    public float getMaxValue() {
        return this.max;
    }

    public float getStepSize() {
        return this.stepSize;
    }

    public void setAnimateDuration(float duration) {
        this.animateDuration = duration;
    }

    public void setAnimateInterpolation(Interpolation animateInterpolation) {
        if (animateInterpolation == null) throw new IllegalArgumentException("animateInterpolation cannot be null.");
        this.animateInterpolation = animateInterpolation;
    }

    public void setVisualInterpolation(Interpolation interpolation) {
        this.visualInterpolation = interpolation;
    }

    public void setRound(boolean round) {
        this.round = round;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isAnimating() {
        return animateTime > 0;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setProgrammaticChangeEvents(boolean programmaticChangeEvents) {
        this.programmaticChangeEvents = programmaticChangeEvents;
    }

    public void setSelectionRange(float start, float end) {
        setValueL(Math.min(start, end));
        setValueR(Math.max(start, end));
    }

    public float getSelectionRangeStart() {
        return Math.min(valueL, valueR);
    }

    public float getSelectionRangeEnd() {
        return Math.max(valueL, valueR);
    }
}
