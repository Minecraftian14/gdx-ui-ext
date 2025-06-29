package in.mcxiv.grid.vis;

import com.kotcrab.vis.ui.VisUI;
import in.mcxiv.grid.Grid;

public class VisGrid extends Grid {
    public VisGrid(Object[][] items, Object[] formats, int alignment, ColumnFit columnFit) {
        super(items, formats, alignment, columnFit, VisUI.getSkin().get(GridStyle.class));
    }

    public VisGrid(Object[][] items, Object[] formats, int alignment, ColumnFit columnFit, String styleName) {
        super(items, formats, alignment, columnFit, VisUI.getSkin().get(styleName, GridStyle.class));
    }

    public VisGrid(Object[][] items, Object[] formats, int alignment, ColumnFit columnFit, GridStyle style) {
        super(items, formats, alignment, columnFit, style);
    }
}
