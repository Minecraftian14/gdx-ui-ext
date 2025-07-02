package in.mcxiv.date.vis;

import com.kotcrab.vis.ui.VisUI;
import in.mcxiv.date.DatePicker;

import java.time.format.TextStyle;
import java.util.Locale;

public class VisDatePicker extends DatePicker {
    public VisDatePicker(Locale locale, int yearMinimum, int yearMaximum, String style, TextStyle yearStyle, TextStyle monthStyle) {
        super(VisUI.getSkin(), locale, yearMinimum, yearMaximum, VisUI.getSkin().get(style, VisDatePickerStyle.class), yearStyle, monthStyle);
    }

    public VisDatePicker() {
        super(VisUI.getSkin());
    }

    public static class VisDatePickerStyle extends DatePickerStyle {
        {
//            selectedColor = VisUI.getSkin().getColor("vis-blue");
            holidayColor = VisUI.getSkin().getColor("vis-red");
        }
    }
}
