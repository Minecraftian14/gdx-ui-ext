package in.mcxiv.date;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class DatePicker extends Table {

    Locale locale;
    int yearMinimum;
    int yearMaximum;
    DatePickerStyle style;
    TextStyle yearStyle;
    TextStyle monthStyle;

    DateListener dateListener;
    LocalDateListener localDateListener;

    int __yearCache;
    IntSupplier getYear;
    IntConsumer setYear;
    SelectBox<String> monthSelection;
    int day = 1;
    TextButton __dayButtonCache;

    public DatePicker(Skin skin, Locale locale, int yearMinimum, int yearMaximum, DatePickerStyle style, TextStyle yearStyle, TextStyle monthStyle) {
        super(skin);
        this.locale = locale;
        this.yearMinimum = yearMinimum;
        this.yearMaximum = yearMaximum;
        this.style = style;
        this.yearStyle = yearStyle;
        this.monthStyle = monthStyle;
        initialize();
        setSize(getPrefWidth(), getPrefHeight());
    }

    public DatePicker(Skin skin) {
        this(skin, Locale.getDefault(), 0, 0, new DatePickerStyle(), TextStyle.FULL_STANDALONE, TextStyle.SHORT_STANDALONE);
    }

    public void initialize() {
        clearChildren();
        LocalDate date = LocalDate.now();
        initialize(date);
    }

    private void fireListeners() {
        if (dateListener != null) dateListener.dateChanged(day, getMonth(), getYear());
        if (localDateListener != null) localDateListener.dateChanged(LocalDate.of(getYear(), getMonthIndex() + 1, day));
    }

    private void initialize(LocalDate date) {
        Table dayBody = new Table(getSkin());
        dayBody.defaults().growX().uniform();
        TextButton[][] dayButtons = new TextButton[6][7];
        ClickListener dayButtonListener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                TextButton button = (TextButton) event.getListenerActor();
                if (button.getText().toString().isEmpty()) return;
                day = Integer.parseInt(button.getText().toString());
                fireListeners();
                if (__dayButtonCache != null) __dayButtonCache.setChecked(false);
                __dayButtonCache = button;
            }
        };
        for (DayOfWeek value : DayOfWeek.values()) {
            Label label = new Label(value.getDisplayName(monthStyle, locale), getSkin());
            label.setAlignment(Align.center);
            if (value.ordinal() >= 5) label.getColor().set(style.holidayColor);
            dayBody.add(label);
        }
        dayBody.row();
        for (int drw = 0; drw < 6; drw++) {
            for (int dow = 0; dow < 7; dow++) {
                TextButton button = dayButtons[drw][dow] = new TextButton("", getSkin(), "toggle");
                button.addListener(dayButtonListener);
                if (dow >= 5) button.getLabel().getColor().set(style.holidayColor);
                dayBody.add(button);
            }
            dayBody.row();
        }
        updateDateButtons(dayButtons, date);
        ChangeListener dateUpdationRequestListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateDateButtons(dayButtons, LocalDate.of(getYear(), getMonthIndex() + 1, day = 1));
                fireListeners();
            }
        };

        if (yearMaximum - yearMinimum <= 0) {
            Pool<GlyphLayout> layoutPool = Pools.get(GlyphLayout.class);
            GlyphLayout layout = layoutPool.obtain();
            layout.setText(getSkin().getFont("default-font"), "00000");
            float width = layout.width;
            layoutPool.free(layout);
            TextField yearField = new TextField(String.valueOf(__yearCache = date.getYear()), getSkin()) {
                @Override
                public float getPrefWidth() {
                    return width;
                }
            };
            getYear = () -> {
                try {
                    return __yearCache = Integer.parseInt(yearField.getText());
                } catch (NumberFormatException e) {
                    return __yearCache;
                }
            };
            setYear = year -> {
                yearField.setText(String.valueOf(year));
                dateUpdationRequestListener.changed(null, null);
            };
            yearField.addListener(dateUpdationRequestListener);
            add(yearField);
        } else {
            SelectBox<Integer> yearSelection = new SelectBox<>(getSkin());
            Integer[] years = new Integer[yearMaximum - yearMinimum + 1];
            for (int i = yearMinimum; i <= yearMaximum; i++) years[i] = i;
            yearSelection.setItems(years);
            yearSelection.setSelected(__yearCache = date.getYear());
            getYear = yearSelection::getSelected;
            setYear = yearSelection::setSelected;
            yearSelection.addListener(dateUpdationRequestListener);
            add(yearSelection);
        }

        monthSelection = new SelectBox<>(getSkin());
        Month[] months = Month.values();
        String[] monthNames = new String[months.length];
        for (int i = 0; i < months.length; i++)
            monthNames[i] = months[i].getDisplayName(yearStyle, locale);
        monthSelection.setItems(monthNames);
        monthSelection.addListener(dateUpdationRequestListener);
        add(monthSelection);

        ImageButton prevYear = new ImageButton(getSkin().getDrawable("icon-arrow-left"));
        ImageButton prevMonth = new ImageButton(getSkin().getDrawable("icon-arrow-left"));
        ImageButton nextMonth = new ImageButton(getSkin().getDrawable("icon-arrow-right"));
        ImageButton nextYear = new ImageButton(getSkin().getDrawable("icon-arrow-right"));

        prevYear.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                prevYear();
            }
        });
        nextYear.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nextYear();
            }
        });
        prevMonth.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                prevMonth();
            }
        });
        nextMonth.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nextMonth();
            }
        });

        add(prevYear).uniformX().right();
        add(prevMonth).uniformX().right();
        add(nextMonth).uniformX().right();
        add(nextYear).uniformX().right();
        row();

        add(dayBody).colspan(6).growX();
    }

    private void updateDateButtons(TextButton[][] dayButtons, LocalDate date) {
        if (__dayButtonCache != null) __dayButtonCache.setChecked(false);
        int firstDOW = LocalDate.of(date.getYear(), date.getMonth(), 1).getDayOfWeek().ordinal();
        int totalDays = date.lengthOfMonth();
        int disp = 0;
        for (int drw = 0; drw < 6; drw++) {
            for (int dow = 0; dow < 7; dow++) {
                if (dow == firstDOW && disp == 0) disp = 1;
                TextButton button = dayButtons[drw][dow];
                button.setText(String.valueOf(disp == 0 ? "" : disp > totalDays ? "" : disp));
                if (disp == day) (__dayButtonCache = button).setChecked(true);
                if (disp > 0) disp++;
            }
        }
    }

    public int getYear() {
        return getYear.getAsInt();
    }

    public void setYear(int year) {
        setYear.accept(year);
    }

    public void nextYear() {
        setYear(getYear() + 1);
    }

    public void prevYear() {
        setYear(getYear() - 1);
    }

    public String getMonth() {
        return monthSelection.getSelected();
    }

    public int getMonthIndex() {
        return monthSelection.getSelectedIndex();
    }

    public void setMonthIndex(int month) {
        int size = monthSelection.getItems().size;
        monthSelection.setSelectedIndex((size + (month % size)) % size);
    }

    public void setMonth(String month) {
        monthSelection.setSelected(month);
    }

    public void nextMonth() {
        setMonthIndex(getMonthIndex() + 1);
    }

    public void prevMonth() {
        setMonthIndex(getMonthIndex() - 1);
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        if (__dayButtonCache != null) __dayButtonCache.setChecked(false);
        this.day = day;
    }

    public interface DateListener {
        default void dateChanged(int day, String month, int year) {
        }
    }

    public interface LocalDateListener {
        default void dateChanged(LocalDate date) {
        }
    }

    public void setDateListener(DateListener dateListener) {
        this.dateListener = dateListener;
    }

    public void setLocalDateListener(LocalDateListener localDateListener) {
        this.localDateListener = localDateListener;
    }

    public static class DatePickerStyle {
        public Color holidayColor = Color.BLUE;
    }

}
