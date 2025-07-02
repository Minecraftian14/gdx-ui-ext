package in.mcxiv.time;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import in.mcxiv.pie.Pie;

import java.time.LocalTime;

public class TimePicker extends Table {

    TimePickerStyle style;
    int hour;
    int minute;
    boolean pieSelection = true; // true for Hour, false for Minute

    public TimePicker(Skin skin, TimePickerStyle style) {
        super(skin);
        this.style = style;
        initialize();
    }

    private void initialize() {
        clearChildren();
        LocalTime time = LocalTime.now();
        initialize(time);
    }

    private void initialize(LocalTime time) {
        // Initialize

        hour = time.getHour();
        minute = time.getMinute();

        Pool<GlyphLayout> layoutPool = Pools.get(GlyphLayout.class);
        GlyphLayout layout = layoutPool.obtain();
        layout.setText(getSkin().getFont("default-font"), "000");
        float fieldWidth = layout.width;
        layoutPool.free(layout);

        TextField hourField = new TextField(String.valueOf(hour % 12), getSkin(), style.hourFieldStyle) {
            @Override
            public float getPrefWidth() {
                return fieldWidth;
            }
        };
        Label colon = new Label(":", getSkin(), style.colonLabelStyle);
        colon.setAlignment(Align.center);
        TextField minuteField = new TextField(String.valueOf(minute), getSkin(), style.minuteFieldStyle) {
            @Override
            public float getPrefWidth() {
                return fieldWidth;
            }
        };
        TextButton amButton = new TextButton("AM", getSkin(), style.amButtonStyle);

        Pie pie = new Pie(style.hand);
        pie.setProgress((hour % 12) / 12f);
        TextButton[] pieButtons = new TextButton[12];
        ObjectIntMap<Actor> pieIndex = new ObjectIntMap<>();
        for (int i = 0; i < 12; i++) pieIndex.put(pieButtons[i] = new TextButton("", getSkin(), style.pieButtonStyle), i);
        for (int i = 0; i < 11; i++) pie.add(pieButtons[i]);
        pie.add(pieButtons[11]);
        setHourLabels(pieButtons);

        // Listeners

        hourField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    hour = Integer.parseInt(hourField.getText());
                    hour = Math.min(11, Math.max(0, hour));
                    hourField.setProgrammaticChangeEvents(false);
                    hourField.setText(String.valueOf(hour));
                    hourField.setProgrammaticChangeEvents(true);
                    pie.setProgress((hour % 12) / 12f);
                    if (amButton.isChecked()) hour += 12;
                } catch (NumberFormatException ignored) {
                }
            }
        });
        minuteField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    minute = Integer.parseInt(minuteField.getText());
                    minute = Math.min(59, Math.max(0, minute));
                    minuteField.setProgrammaticChangeEvents(false);
                    minuteField.setText(String.valueOf(minute));
                    minuteField.setProgrammaticChangeEvents(true);
                    pie.setProgress(minute / 60f);
                } catch (NumberFormatException ignored) {
                }
            }
        });
        amButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                amButton.setProgrammaticChangeEvents(false);
                if (amButton.isChecked()) amButton.setText("PM");
                else amButton.setText("AM");
                amButton.setProgrammaticChangeEvents(true);
            }
        });
        amButton.setChecked(hour >= 12);
        ClickListener pieListener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (pieSelection) {
                    hour = pieIndex.get(event.getListenerActor(), 0);
                    hourField.setProgrammaticChangeEvents(false);
                    hourField.setText(String.valueOf((hour + 1) % 12));
                    hourField.setProgrammaticChangeEvents(true);
                    if (amButton.isChecked()) hour += 12;
                    setMinuteLabels(pieButtons);
                    pieSelection = false;
                    pie.setProgress(minute / 60f);
                } else {
                    minute = pieIndex.get(event.getListenerActor(), 0) * 5;
                    minuteField.setProgrammaticChangeEvents(false);
                    minuteField.setText(String.valueOf(minute));
                    hourField.setProgrammaticChangeEvents(true);
                    setHourLabels(pieButtons);
                    pieSelection = true;
                    pie.setProgress((hour % 12) / 12f);
                }
            }
        };
        for (TextButton pieButton : pieButtons) pieButton.addListener(pieListener);

        // Add to UI

        add(hourField).uniformX().fill();
        add(colon).fillY();
        add(minuteField).uniformX().fill();
        add(amButton).fillY().row();
        add(pie).colspan(4).fill();
    }

    private static void setHourLabels(TextButton[] pieButtons) {
        for (int i = 0; i < pieButtons.length; i++) pieButtons[i].setText(String.valueOf(i + 1));
    }

    private static void setMinuteLabels(TextButton[] pieButtons) {
        for (int i = 0; i < pieButtons.length; i++) pieButtons[i].setText(String.valueOf(i * 5));
    }

    public static class TimePickerStyle {
        public String hourFieldStyle = "default";
        public String colonLabelStyle = "default";
        public String minuteFieldStyle = "default";
        public String amButtonStyle = "toggle";
        public String pieButtonStyle = "default";
        public Image hand;
    }
}
