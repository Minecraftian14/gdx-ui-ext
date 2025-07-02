package `in`.mcxiv.demo

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.color.ColorPicker
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter
import `in`.mcxiv.date.DatePicker
import `in`.mcxiv.date.vis.VisDatePicker.VisDatePickerStyle
import `in`.mcxiv.date.vis.ktx.visDatePicker
import `in`.mcxiv.grid.Grid.GridStyle
import `in`.mcxiv.grid.vis.ktx.visGrid
import `in`.mcxiv.range.vis.ktx.visRangeSlider
import ktx.actors.onChange
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.actor
import ktx.scene2d.actors
import ktx.scene2d.scene2d
import ktx.scene2d.vis.*
import ktx.style.set

object Demo : ApplicationAdapter() {

    lateinit var batch: SpriteBatch
    lateinit var stage: Stage

    override fun create() {
        VisUI.load()
        Scene2DSkin.defaultSkin = VisUI.getSkin()

        batch = SpriteBatch()
        stage = Stage(ScreenViewport(), batch)
        stage.actors {
            visTable {
                defaults().pad(30f).growX().top()
                setFillParent(true)
                val container = scene2d.visTable {}

                actor(TabbedPane().let {
                    it.addListener(object : TabbedPaneAdapter() {
                        override fun switchedTab(tab: Tab) {
                            container.clearChildren()
                            container.actor(tab.contentTable) { it.grow() }
                        }
                    })
                    it.add(getTabDate())
                    it.add(getTabRange())
                    it.add(getTabGrid())
                    it.table
                }) { it.growX().row() }
                actor(container) { it.grow().row() }
            }
        }
        Gdx.input.inputProcessor = stage
    }

    fun getTabRange(): Tab = KTab("Range", false, false).apply {
        contentTable.setFillParent(true)

        val label = visLabel("") { it.row() }
        val slider = visRangeSlider(min = -273.15f, max = 800f, steps = 0.15f, vertical = false) { it.grow().row() }
        slider.setSelectionRange(0f, 500f)
        ({
            label.color.set(Color.BLUE).lerp(Color.RED, slider.percentL).lerp(Color.WHITE, slider.percentR)
            label.setText("Slider Value: [%+08.2f, %+08.2f]".format(slider.selectionRangeStart, slider.selectionRangeEnd))
        }).apply { this() }.let { slider.onChange { it() } }
    }

    fun getTabGrid(): Tab = KTab("Table", false, false).apply {
        contentTable.setFillParent(true)

        VisUI.getSkin()["default"] = GridStyle().apply {
            cellBackground = VisUI.getSkin().get("border", Drawable::class.java)
            fontColorUnselected = VisUI.getSkin().get("white", Color::class.java)
            selection = VisUI.getSkin().get("padded-list-selection", Drawable::class.java)
            fontColorSelected = VisUI.getSkin().get("white", Color::class.java)
            font = VisUI.getSkin().get("default-font", BitmapFont::class.java)
        }

        visGrid(arrayOf(arrayOf(1546, 28, 3), arrayOf("a", "bb", "ccccc"), arrayOf(6, 7, 8)))
    }

    fun getTabDate(): Tab = KTab("Date", false, false).apply {
        contentTable.setFillParent(true)
        VisUI.getSkin()["default"] = VisDatePickerStyle()
        visDatePicker()
    }

    override fun resume() {
        Gdx.input.inputProcessor = stage
    }

    override fun render() {
        ScreenUtils.clear(Color.BLACK)
        stage.act()
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        batch.dispose()
    }
}

fun main() {
    Lwjgl3Application(Demo, Lwjgl3ApplicationConfiguration().apply {
        setTitle("Gdx UI Ext Demo")
        useVsync(true)
        setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1)
        setWindowedMode(640, 480)
    })
}
