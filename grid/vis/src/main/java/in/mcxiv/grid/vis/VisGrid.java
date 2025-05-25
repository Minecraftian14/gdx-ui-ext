package in.mcxiv.grid.vis;

import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.kotcrab.vis.ui.VisUI;
import in.mcxiv.grid.Grid;

public class VisGrid extends Grid {
    public VisGrid(Object[][] items, int alignment, ColumnFit columnFit) {
        super(items, alignment, columnFit, VisUI.getSkin().get(ListStyle.class));
    }

    public VisGrid(Object[][] items, int alignment, ColumnFit columnFit, String styleName) {
        super(items, alignment, columnFit, VisUI.getSkin().get(styleName, ListStyle.class));
    }

    public VisGrid(Object[][] items, int alignment, ColumnFit columnFit, ListStyle style) {
        super(items, alignment, columnFit, style);
    }
}
