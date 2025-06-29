package in.mcxiv.grid;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.badlogic.gdx.utils.*;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public class Grid extends Widget {

    public enum ColumnFit {
        EXACT,
        FIRST_COLUMN,
        ESTIMATE,
        MINIMUM,
    }

    protected GridStyle style;
    public Object[][] items;
    public Object[] formats; // One format per column
    public ColumnFit columnFit = ColumnFit.EXACT;
    private int alignment = Align.left;

    public IntMap<IntSet> selection = new IntMap<>();
    public boolean isSelectionDisabled = false;
    private Rectangle cullingArea;

    public final InputListener keyListener;
    private final Object stringsLock = new Object();
    public String[][] strings;
    protected int rows;
    protected int cols;
    protected float cellHeight;
    protected float[] cellWidth;
    protected float prefWidth, prefHeight;

    // Mouse events
    protected int mPressedRowIdx = -1;
    protected int mPressedColIdx = -1;
    protected int mHoverRowIdx = -1;
    protected int mHoverColIdx = -1;
    protected int mDragStartRowIdx = -1;
    protected int mDragStartColIdx = -1;

    public Grid(Object[][] itemsIn, Object[] formats, int alignment, ColumnFit columnFit, GridStyle style) {
        setStyle(style);
        this.formats = formats;
        setItems(itemsIn);
        setSize(getPrefWidth(), getPrefHeight());
        this.columnFit = columnFit;
        this.alignment = alignment;

        addListener(keyListener = new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                switch (keycode) {
                    case Input.Keys.A:
                        if (UIUtils.ctrl()) {
                            if (isSelectionDisabled) return true;
                            selectAll();
                            return true;
                        }
                        break;
                    case Input.Keys.HOME:
                        mPressedColIdx = 0;
                        mPressedRowIdx = 0;
                        return true;
                    case Input.Keys.END:
                        mPressedColIdx = cols - 1;
                        mPressedRowIdx = rows - 1;
                        return true;
                    case Input.Keys.UP:
                        if (mPressedRowIdx > 0) mPressedRowIdx--;
                        return true;
                    case Input.Keys.DOWN:
                        if (mPressedRowIdx < rows - 1) mPressedRowIdx++;
                        return true;
                    case Input.Keys.LEFT:
                        if (mPressedColIdx > 0) mPressedColIdx--;
                        return true;
                    case Input.Keys.RIGHT:
                        if (mPressedColIdx < cols - 1) mPressedColIdx++;
                        return true;
                    case Input.Keys.SPACE:
                        if (mPressedColIdx == -1 || mPressedRowIdx == -1) return true;
                        if (isSelectionDisabled) return true;
                        flipSelection(mPressedRowIdx, mPressedColIdx);
                        return true;
                    case Input.Keys.ESCAPE:
                        mPressedRowIdx = mPressedColIdx = -1;
                        deselectAll();
                        if (getStage() != null) getStage().setKeyboardFocus(null);
                        return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (pointer != 0 || button != 0) return true;
                if (isSelectionDisabled) return true;
                if (getStage() != null) getStage().setKeyboardFocus(Grid.this);
                int colIdx = screenToCol(x, true);
                int rowIdx = screenToRow(y, true);
                if (colIdx == -1 || rowIdx == -1) return true;
                flipSelection(rowIdx, colIdx);
                mPressedColIdx = colIdx;
                mPressedRowIdx = rowIdx;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (pointer != 0 || button != 0) return;
                mPressedColIdx = -1;
                mPressedRowIdx = -1;
                if (mDragStartRowIdx != -1 || mDragStartColIdx != -1) {
                    int mDragEndColIdx = screenToCol(x, false);
                    int mDragEndRowIdx = screenToRow(y, false);
                    int rowStart = Math.min(mDragStartRowIdx, mDragEndRowIdx), rowEnd = Math.max(mDragStartRowIdx, mDragEndRowIdx);
                    int colStart = Math.min(mDragStartColIdx, mDragEndColIdx), colEnd = Math.max(mDragStartColIdx, mDragEndColIdx);
//                    for (int rowIdx = rowStart; rowIdx <= rowEnd; rowIdx++)
//                        for (int colIdx = colStart; colIdx <= colEnd; colIdx++)
//                            select(rowIdx, colIdx);
                    selectAll(rowStart, rowEnd + 1, colStart, colEnd + 1);
                    mDragStartRowIdx = mDragStartColIdx = -1;
                }
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                mHoverColIdx = mHoverRowIdx = -1;
                if (mDragStartColIdx == -1 || mDragStartRowIdx == -1) {
                    mDragStartColIdx = screenToCol(x, false);
                    mDragStartRowIdx = screenToRow(y, false);
                }
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                mHoverColIdx = screenToCol(x, true);
                mHoverRowIdx = screenToRow(y, true);
                return false;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == 0) mPressedColIdx = mPressedRowIdx = -1;
                if (pointer == -1) mHoverColIdx = mHoverRowIdx = -1;
            }
        });
    }

    public int screenToCol(float x, boolean natural) {
        Drawable background = style.background;
        if (background != null) x -= background.getLeftWidth();
        if (x < 0) return natural ? -1 : 0;
        int colIdx = 0;
        for (; colIdx < cols; colIdx++) {
            x -= cellWidth[colIdx];
            if (x < 0) return colIdx;
        }
        return natural ? -1 : cols - 1;
    }

    public int screenToRow(float y, boolean natural) {
        float height = getHeight();
        Drawable background = style.background;
        if (background != null) {
            height -= background.getTopHeight() + background.getBottomHeight();
            y -= background.getBottomHeight();
        }
        int index = (int) ((height - y) / cellHeight);
        if (index < 0) return natural ? -1 : 0;
        if (index >= rows) return natural ? -1 : rows - 1;
        return index;
    }

    public void setItems(Object[][] items) {
        if (items == null) throw new IllegalArgumentException("What are you trying to do?");
        int rows = items.length;
        int cols = rows == 0 ? 0 : items[0].length;
        for (int rowIdx = 1; rowIdx < rows; rowIdx++)
            if (items[rowIdx].length != cols) throw new IllegalArgumentException("Items array should be a perfect rectangle.");
        this.items = items;
        bakeFormats(rows, cols);
        this.rows = rows;
        this.cols = cols;
        this.cellWidth = new float[cols];
    }

    @SuppressWarnings("unchecked")
    public void bakeFormats(int rows, int cols) {
        if (rows == 0 || cols == 0) return;
        synchronized (stringsLock) {
            if (strings == null || strings.length < rows || strings[0].length < cols)
                this.strings = new String[rows][cols];
            for (int rowIdx = 0; rowIdx < rows; rowIdx++)
                for (int colIdx = 0; colIdx < cols; colIdx++) {
                    Object format = formats[Math.min(colIdx, formats.length - 1)];
                    Object item = items[rowIdx][colIdx];
                    if (format instanceof String)
                        strings[rowIdx][colIdx] = String.format((String) format, item);
                    else if (format instanceof DateTimeFormatter && item instanceof TemporalAccessor)
                        strings[rowIdx][colIdx] = ((DateTimeFormatter) format).format((TemporalAccessor) item);
                    else if (format instanceof Function)
                        strings[rowIdx][colIdx] = ((Function<Object, String>) format).apply(item);
                    else strings[rowIdx][colIdx] = Objects.toString(item);

                    strings[rowIdx][colIdx] = Objects.toString(strings[rowIdx][colIdx]);
                }
        }
    }

    @Override
    public void layout() {
        BitmapFont font = style.font;
        Drawable selectedDrawable = style.selection;

        cellHeight = font.getCapHeight() - font.getDescent() * 2;
        cellHeight += selectedDrawable.getTopHeight() + selectedDrawable.getBottomHeight();
        prefHeight = rows * cellHeight;

        prefWidth = 0;
        int extraSpace = 10;
        Arrays.fill(cellWidth, 0);
        Pool<GlyphLayout> layoutPool = Pools.get(GlyphLayout.class);
        GlyphLayout layout = layoutPool.obtain();

        if (columnFit == ColumnFit.EXACT) {
            synchronized (stringsLock) {
                for (int rowIdx = 0; rowIdx < rows; rowIdx++) {
                    for (int colIdx = 0; colIdx < cols; colIdx++) {
                        layout.setText(font, strings[rowIdx][colIdx]);
                        cellWidth[colIdx] = Math.max(extraSpace + layout.width, cellWidth[colIdx]);
                    }
                }
            }
            for (int colIdx = 0; colIdx < cols; colIdx++)
                prefWidth += cellWidth[colIdx];

        } else if (columnFit == ColumnFit.FIRST_COLUMN) {
            synchronized (stringsLock) {
                for (int rowIdx = 0; rowIdx < rows; rowIdx++) {
                    layout.setText(font, strings[rowIdx][0]);
                    prefWidth = Math.max(extraSpace + layout.width, prefWidth);
                }
            }
            Arrays.fill(cellWidth, prefWidth);
            prefWidth *= cols;

        } else if (columnFit == ColumnFit.ESTIMATE) {
            layout.setText(font, "WWWWWW");
            Arrays.fill(cellWidth, extraSpace + layout.width);
            prefWidth = layout.width * cols;

        } else if (columnFit == ColumnFit.MINIMUM) {
            Arrays.fill(cellWidth, extraSpace + cellHeight);
            prefWidth = (extraSpace + cellHeight) * cols;
        }
        layoutPool.free(layout);
        prefWidth += selectedDrawable.getLeftWidth() + selectedDrawable.getRightWidth();

        Drawable background = style.background;
        if (background != null) {
            prefWidth = Math.max(prefWidth + background.getLeftWidth() + background.getRightWidth(), background.getMinWidth());
            prefHeight = Math.max(prefHeight + background.getTopHeight() + background.getBottomHeight(), background.getMinHeight());
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        drawBackground(batch, parentAlpha);

        BitmapFont font = style.font;
        Drawable cellBackground = style.cellBackground;
        Drawable selectedDrawable = style.selection;
        Color fontColorSelected = style.fontColorSelected;
        Color fontColorUnselected = style.fontColorUnselected;

        Color color = getColor();
        batch.getColor().set(color);

        float x = getX(), y = getY();
        float initialItemY = getHeight();

        Drawable background = style.background;
        if (background != null) {
            float leftWidth = background.getLeftWidth();
            x += leftWidth;
            initialItemY -= background.getTopHeight();
        }

        float textOffsetX = selectedDrawable.getLeftWidth();
        float textOffsetY = selectedDrawable.getTopHeight() - font.getDescent();

        font.getColor().set(fontColorUnselected);

        float itemX = 0;
        for (int colIdx = 0; colIdx < cols; colIdx++) {

            float columnWidth = cellWidth[colIdx];
            float textWidth = columnWidth - textOffsetX - selectedDrawable.getRightWidth();

            float itemY = initialItemY;
            for (int rowIdx = 0; rowIdx < rows; rowIdx++) {

                Object item = items[rowIdx][colIdx];
                String string;
                synchronized (stringsLock) {
                    string = strings[rowIdx][colIdx];
                }
                boolean selected = isSelected(rowIdx, colIdx);

                Drawable drawable = cellBackground;
                if (mPressedColIdx == colIdx && mPressedRowIdx == rowIdx) {
                    drawable = style.down;
                } else if (selected) {
                    drawable = selectedDrawable;
                    font.getColor().set(fontColorSelected);
                } else if (mHoverColIdx == colIdx && mHoverRowIdx == rowIdx) {
                    drawable = style.over;
                }

                drawCell(batch, drawable, x + itemX, y + itemY - cellHeight, columnWidth, cellHeight);
                try {
                    drawItem(batch, font, string, x + textOffsetX + itemX, y + itemY - textOffsetY, textWidth, 0);
                } catch (Exception e) {
                    System.out.println("colIdx=" + colIdx + ", rowIdx=" + rowIdx);
                    throw new RuntimeException(e);
                }

                if (selected) font.getColor().set(fontColorUnselected);
                itemY -= cellHeight;
            }
            itemX += columnWidth;
        }
    }

    protected void drawBackground(Batch batch, float parentAlpha) {
        if (style.background != null) {
            Color color = getColor();
            batch.getColor().set(color);
            style.background.draw(batch, getX(), getY(), getWidth(), getHeight());
        }
    }

    private void drawCell(Batch batch, Drawable drawable, float x, float y, float w, float h) {
        if (drawable == null) return;
        drawable.draw(batch, x, y, w, h);
    }

    protected GlyphLayout drawItem(Batch batch, BitmapFont font, String string, float x, float y, float w, float h) {
        if (string == null) {
            System.out.println("x=" + x + ", y=" + y + ", w=" + w + ", h=" + h);
            System.out.println("Items");
            for (Object[] item : items) System.out.println(Arrays.toString(item));
            System.out.println("Strings");
            for (String[] strings : strings) System.out.println(Arrays.toString(strings));
        }
        // TODO: Sometimes the string is null. In order to fix that I have added s a stringsLock. Verify if that fixed things.
        //  If a NPE is thrown again, the next step is to remove that lock, and then use your brains.
        //  Ofc, I can just add a null->"null" or an on demand bakeFormats. But also analyze why it was null in the first place.
        return font.draw(batch, string, x, y, 0, string.length(), w, alignment, false, "...");
    }

    public void setStyle(GridStyle style) {
        if (style == null) throw new IllegalArgumentException("style cannot be null.");
        this.style = style;
        invalidateHierarchy();
    }

    public GridStyle getStyle() {
        return style;
    }

    public float getPrefWidth() {
        validate();
        return prefWidth;
    }

    public float getPrefHeight() {
        validate();
        return prefHeight;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    // Returns true if not selected already
    public boolean select(int row, int col) {
        if (!selection.containsKey(row)) selection.put(row, new IntSet());
        return selection.get(row).add(col);
    }

    // Returns true if not deselected already
    public boolean deselect(int row, int col) {
        if (!selection.containsKey(row)) return false;
        return selection.get(row).remove(col);
    }

    public boolean isSelected(int row, int col) {
        if (!selection.containsKey(row)) return false;
        return selection.get(row).contains(col);
    }

    // Returns true if item is now selected
    public boolean flipSelection(int row, int col) {
        if (isSelected(row, col)) {
            deselect(row, col);
            return false;
        } else {
            select(row, col);
            return true;
        }
    }

    public void selectAll(int rStart, int rEnd, int cStart, int cEnd) {
        if (isSelectionDisabled) return;
        for (int row = rStart; row < rEnd; row++) {
            if (!selection.containsKey(row)) selection.put(row, new IntSet());
            IntSet entries = selection.get(row);
            for (int col = cStart; col < cEnd; col++) entries.add(col);
        }
    }

    public void selectAll() {
        selectAll(0, rows, 0, cols);
    }

    public void deselectAll() {
        for (int row = 0; row < rows; row++)
            if (selection.containsKey(row))
                selection.get(row).clear();
    }

    public void deselectAll(int rStart, int rEnd, int cStart, int cEnd) {
        for (int row = rStart; row < rEnd; row++) {
            if (!selection.containsKey(row)) continue;
            IntSet entries = selection.get(row);
            for (int col = cStart; col < cEnd; col++) entries.remove(col);
        }
    }

    public static class GridStyle extends ListStyle {
        public Drawable cellBackground;
    }

}
