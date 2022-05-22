package view

import javafx.geometry.Pos
import javafx.scene.text.TextAlignment
import controller.*
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import tornadofx.*
import kotlin.system.exitProcess

class RussianCheckersView : View() {
    private val board = Board()
    override val root = borderpane {
        top = menubar {
            menu("Menu") {
                style = "-fx-font-size: 16"
                item("New Game") {
                    setOnAction { board.newGame() }
                }
                item("Change White player") {
                    setOnAction { board.changeWhite() }
                }
                item("Change Black player") {
                    setOnAction { board.changeBlack() }
                }
                menu("Sketches..") {
                    for (i in 1..9){
                        item("Sketch №$i") {
                            setOnAction { board.loadSamples(-i) }
                        }
                    }
                }
                separator()
                item("About") {
                    setOnAction { about() }
                }
                separator()
                item("Exit") {
                    setOnAction { exitProcess(0) }
                }
            }
        }
        center = board.root
    }

    private fun about(){
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = "About"
        alert.isResizable = false
        alert.headerText = """
This program was written as part of a course project 
on practical work on the "Algorithms and data structures".

   student - Ilya Zhuravsky
   curator - Mikhail Belyaev

Institute of Computer Science and Technology,
Peter the Great St. Petersburg Polytechnic University
        """.trimIndent()
        val buttonOk = ButtonType("Ok", ButtonBar.ButtonData.OK_DONE)
        alert.buttonTypes.setAll(buttonOk)
        alert.showAndWait()
    }

    init {
        title = "Russian checkers by Ilya Zhuravsky"
        primaryStage.width = 800.0
        primaryStage.height = 800.0+60.0
    }
}

class GameOverWindow : Fragment() {
    val message: String by param()
    val board: Board by param()
    init {
        title = "Russian checkers"
    }
    override val root = vbox {
        alignment = Pos.CENTER

        label {
            text = "GAME OVER!\n\n$message\n\n  Press 'Restart' to continue  \n\n"
            style = "-fx-font-size: 22; -fx-font-weight: bold"
            textAlignment = TextAlignment.CENTER
            textFill = javafx.scene.paint.Color.ORANGERED
        }

        hbox {
            alignment = Pos.CENTER

            button {
                text = " Restart "
                style = "-fx-font-size: 20"
                imageview{
                    fitHeight = 25.0
                    fitWidth = 25.0
                    image = resources.image("/reset.png")
                }
                setOnAction {
                    board.newGame()
                    close()
                }
            }
            label {
                text = "   "
            }
            button {
                text = "  Exit  "
                imageview{
                        fitHeight = 25.0
                        fitWidth = 25.0
                        image = resources.image("/exit.png")
                    }
                style = "-fx-font-size: 20"
                setOnAction { exitProcess(0) }
            }
        }
        label {
            text = "   "
        }
    }
}
