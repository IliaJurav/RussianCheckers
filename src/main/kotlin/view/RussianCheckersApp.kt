package view

import javafx.application.Application
import javafx.stage.Stage
import tornadofx.App



class RussianCheckersApp : App(RussianCheckersView::class) {
    override fun start(stage: Stage) {
        stage.icons += resources.image("/checkers.png")
        //stage.isResizable = false
        super.start(stage)
    }
}

fun main(args: Array<String>) {
    Application.launch(RussianCheckersApp::class.java, *args)
}
