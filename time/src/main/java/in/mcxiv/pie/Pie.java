package in.mcxiv.pie;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.utils.Array;

public class Pie extends WidgetGroup {
    float prefRadius, minRadius, maxRadius;
    float prefWidth, prefHeight, minWidth, minHeight, maxWidth, maxHeight;
    float actorPrefWidth, actorPrefHeight, actorMinWidth, actorMinHeight, actorMaxWidth, actorMaxHeight;
    boolean sizeInvalid = true;
    Array<Actor> actors = new Array<>();
    Image hand;

    public Pie(Image hand) {
        this.hand = hand;
        addActor(hand);
        setTouchable(Touchable.childrenOnly);
    }

    public Pie(Image hand, Actor... actors) {
        this(hand);
        for (Actor actor : actors) addActor(actor);
        this.actors.addAll(actors);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        sizeInvalid = true;
    }

    public void add(Actor actor) {
        addActor(actor);
        actors.add(actor);
    }

    @Override
    public void addActor(Actor actor) {
        super.addActor(actor);
    }

    private void computeSize() {
        prefWidth = prefHeight = actorPrefWidth = actorPrefHeight = 0;
        minWidth = minHeight = actorMinWidth = actorMinHeight = 0;
        maxWidth = maxHeight = actorMaxWidth = actorMaxHeight = 0;

        float angle = MathUtils.PI2 / actors.size;
        for (Actor child : actors) {
            float childMaxWidth, childMaxHeight;
            if (child instanceof Layout) {
                Layout layout = (Layout) child;
                actorPrefWidth = Math.max(actorPrefWidth, layout.getPrefWidth());
                actorPrefHeight = Math.max(actorPrefHeight, layout.getPrefHeight());
                actorMinWidth = Math.max(actorMinWidth, layout.getMinWidth());
                actorMinHeight = Math.max(actorMinHeight, layout.getMinHeight());
                childMaxWidth = layout.getMaxWidth();
                childMaxHeight = layout.getMaxHeight();
            } else {
                actorPrefWidth = Math.max(actorPrefWidth, child.getWidth());
                actorPrefHeight = Math.max(actorPrefHeight, child.getHeight());
                actorMinWidth = Math.max(actorMinWidth, child.getWidth());
                actorMinHeight = Math.max(actorMinHeight, child.getHeight());
                childMaxWidth = 0;
                childMaxHeight = 0;
            }
            if (childMaxWidth > 0) actorMaxWidth = actorMaxWidth == 0 ? childMaxWidth : Math.min(actorMaxWidth, childMaxWidth);
            if (childMaxHeight > 0) actorMaxHeight = actorMaxHeight == 0 ? childMaxHeight : Math.min(actorMaxHeight, childMaxHeight);
        }

        // Why accurate?
        prefRadius = Math.max(actorPrefWidth, actorPrefHeight) / MathUtils.sin(angle);
        minRadius = Math.max(actorMinWidth, actorMinHeight) / MathUtils.sin(angle);
        maxRadius = Math.max(actorMaxWidth, actorMaxHeight) / MathUtils.sin(angle);

        // Why inaccurate?
        prefWidth = 2 * prefRadius + actorPrefWidth;
        minWidth = 2 * minRadius + actorMinWidth;
        maxWidth = 2 * maxRadius + actorMaxWidth;
        prefHeight = 2 * prefRadius + actorPrefHeight;
        minHeight = 2 * minRadius + actorMinHeight;
        maxHeight = 2 * maxRadius + actorMaxHeight;

        sizeInvalid = false;
    }

    @Override
    public void layout() {
        if (sizeInvalid) computeSize();
        float width = getWidth(), height = getHeight();
        float variabilityX = maxWidth > minWidth ? (width - minWidth) / (maxWidth - minWidth) : 0f;
        float variabilityY = maxHeight > minHeight ? (height - minHeight) / (maxHeight - minHeight) : 0f;
        float actorWidth = maxWidth > minWidth ? actorMinWidth + (actorMaxWidth - actorMinWidth) * variabilityX : Math.min(actorPrefWidth, actorPrefWidth * width / prefWidth);
        float actorHeight = maxHeight > minHeight ? actorMinHeight + (actorMaxHeight - actorMinHeight) * variabilityY : Math.min(actorPrefHeight, actorPrefHeight * height / prefHeight);
        float radius = Math.min((width - actorWidth) / 2, (height - actorHeight) / 2);

        int index = 0;
        for (Actor child : actors) {
            float angle = MathUtils.PI2 * index / actors.size - MathUtils.HALF_PI;
            child.setBounds(
                (width - actorWidth) * 0.5f + radius * MathUtils.cos(angle),
                (height - actorHeight) * 0.5f - radius * MathUtils.sin(angle),
                actorWidth,
                actorHeight
            );
            if (child instanceof Layout) ((Layout) child).validate();
            index++;
        }

        hand.setOrigin(radius * 0.05f, 0f);
        hand.setBounds(width * 0.5f - radius * 0.05f, height * 0.5f, radius * 0.1f, radius);
    }

    @Override
    public float getPrefWidth() {
        if (sizeInvalid) computeSize();
        return prefWidth;
    }

    @Override
    public float getPrefHeight() {
        if (sizeInvalid) computeSize();
        return prefHeight;
    }

    @Override
    public float getMinWidth() {
        if (sizeInvalid) computeSize();
        return minWidth;
    }

    @Override
    public float getMinHeight() {
        if (sizeInvalid) computeSize();
        return minHeight;
    }

    @Override
    public float getMaxWidth() {
        if (sizeInvalid) computeSize();
        return maxWidth;
    }

    @Override
    public float getMaxHeight() {
        if (sizeInvalid) computeSize();
        return maxHeight;
    }

    public float getProgress() {
        return -hand.getRotation() / 360f;
    }

    public void setProgress(float progress) {
        hand.setRotation(-progress * 360f);
        invalidate();
    }

}
