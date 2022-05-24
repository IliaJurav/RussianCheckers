package controller

import javafx.animation.AnimationTimer
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.shape.StrokeType
import tornadofx.*
import view.*
import model.*
import kotlin.math.abs
import kotlin.system.exitProcess


class Board : View() {
    private val boardSize = 8
    private var moveCells = mutableListOf<Triple<Int, Int, Int>>()
    private var grid: GridPane? = null
    private var ai = AIBoard()
    private var cells: Array<Triple<Node?, Rectangle?, ImageView?>> =
        Array(32) { Triple(null, null, null) }
    private var sideMove = Kind.WHITE
    private var sideAI = Kind.BLACK
    private lateinit var imageWhitePlayer: ImageView
    private lateinit var imageBlackPlayer: ImageView
    // поля для анимации
    private var processedAnimation = false
    // собственно аниматор
    private var aiAnimation: AnimationTimer = object : AnimationTimer() {
        private var timAni: Long = 0
        private var thrUIMove = mutableListOf<Triple<Int, Int, Int>>()
        private var subStepAni = 0
        private val delayAni: Long = 250_000_000 // 250 мсек
        private var stepAni = -1
        override fun start() {
            super.start()
            processedAnimation = true
            stepAni = 0
        }
        override fun stop() {
            super.stop()
            processedAnimation = false
        }
        override fun handle(now: Long) {
            if ((timAni - now) in 0..delayAni) return
            timAni = now + delayAni
            when (stepAni) {
                //------------------------------------------
                0 -> {// инициализация
                    stepAni = 1
                    subStepAni = 0
                    Thread { //Runnable
                        kotlin.run {
                            // ходы есть - уже проверено
                            thrUIMove = ai.getAIMoves(sideAI).toMutableList()
                            if (stepAni != -1) stepAni = 2
                        }
                    }.start()
                }
                //------------------------------------------
                // мигание - пока AI раздумывает
                1 -> {
                    if (subStepAni == 0) { //шаг 1 всё погасить
                        subStepAni = 1
                        for (mv in moveCells) {
                            cells[mv.first].second?.strokeWidth = 0.0
                        }
                    } else { // шаг 2 всё зажечь
                        subStepAni = 0
                        for (mv in moveCells) {
                            cells[mv.first].second?.strokeWidth = 3.0
                        }
                    }
                }
                //------------------------------------------
                // выполнение шагов - подготовка
                2 -> {
                    // снять все выделения
                    for (i in 0..31) {
                        cells[i].second?.strokeWidth = 0.0
                    }
                    // выделить по ходу
                    // подсвечиваем клетки для хода
                    val move = thrUIMove.first()
                    cells[move.first].second?.stroke = Color.LIME
                    cells[move.first].second?.strokeWidth = 5.0
                    cells[move.second].second?.stroke = Color.LIME
                    cells[move.second].second?.strokeWidth = 5.0
                    if (move.third >= 0) {
                        // захватываемая клетка
                        cells[move.third].second?.stroke = Color.RED
                        cells[move.third].second?.strokeWidth = 5.0
                    }
                    stepAni = 3
                }
                //------------------------------------------
                3 -> {
                    // выполнить ход перенести шашку
                    // переносим шашку
                    val move = thrUIMove.first()
                    ai.moveChecker(move)
                    // перерисовать клетки
                    setCellImage(move.first)
                    setCellImage(move.second)
                    setCellImage(move.third)
                    stepAni = 4
                }
                //------------------------------------------
                4 -> {
                    // подготовка к следующему переносу шашки
                    thrUIMove.removeAt(0)
                    if (thrUIMove.size > 0) {// остались ходы
                        // подсвечиваем следующий ход
                        val move = thrUIMove.first()
                        cells[move.second].second?.stroke = Color.LIME
                        cells[move.second].second?.strokeWidth = 5.0
                        if (move.third >= 0) {
                            cells[move.third].second?.stroke = Color.RED
                            cells[move.third].second?.strokeWidth = 5.0
                        }
                        stepAni = 3
                    } else {
                        // убираем сбитые шашки
                        ai.clearCaptured()
                        moveCells.clear()
                        // перерисовываем всю доску
                        updateBoard()
                        stepAni = 5
                    }
                }
                //------------------------------------------
                5 -> {// завершение анимации
                    sideMove = if (sideMove == Kind.WHITE) Kind.BLACK else Kind.WHITE
                    moveCells = ai.getMoves(-1, sideMove)
                    if (moveCells.isEmpty()) {
                        val mess =
                            if (sideMove == Kind.WHITE) "Black is win!" else "White is win!"
                        find<GameOverWindow>(mapOf(GameOverWindow::message to mess, GameOverWindow::board to this@Board))
                            .openModal(resizable = false)
                    }
                    // нарисовать изменения
                    updateBoard()
                    stop()
                }
                //------------------------------------------
                else -> {// заглушка
                    stop()
                }
            }

        }
    }
    // генерация шашечной доски
    override val root = stackpane {
        fitToParentSize()
        alignment = Pos.CENTER
        val fontStyle = "-fx-font-size: 20; -fx-font-weight: bold"

        val mainStackPane = this@stackpane

        grid = gridpane {
            maxHeightProperty().bind(mainStackPane.widthProperty())
            maxWidthProperty().bind(mainStackPane.heightProperty())

            prefHeightProperty().bind(mainStackPane.heightProperty())
            prefWidthProperty().bind(mainStackPane.widthProperty())

            val bs = boardSize + 1
            for (idxRow in 0..bs) { // столбец
                row {
                    for (idxCol in 0..bs) { // колонка
                        stackpane {
                            fitToParentSize()
                            val stackPane = this
                            val rect = rectangle {
                                heightProperty().bind(stackPane.heightProperty())
                                widthProperty().bind(stackPane.widthProperty())
                                strokeType = StrokeType.INSIDE
                                strokeWidth = 0.0
                            }
                            val imv = imageview {
                                fitHeightProperty().bind(stackPane.heightProperty().multiply(0.8))
                                fitWidthProperty().bind(stackPane.widthProperty().multiply(0.8))
                            }
                            // ----------------------------------------
                            // игровые клетки
                            if (idxCol in 1..8 && idxRow in 1..8)
                            {
                                if ((idxRow + idxCol) % 2 == 1){
                                    val idx = (idxCol - 1) / 2 + (boardSize - idxRow) * 4
                                    onMouseClicked = EventHandler { selectCell(idx) }
                                    cells[idx] = Triple(stackPane, rect, imv)
                                    rect.fill = Color.rgb(128, 128, 16)
                                } else {
                                    rect.fill = Color.IVORY
                                }
                            }
                            // -----------------
                            // клетки обрамления
                            // цифры
                            if (idxCol in arrayOf(0,9) && idxRow in 1..8)
                            {
                                label{
                                    text = "${9 - idxRow}"
                                    style = fontStyle
                                }
                                // скрытые примеры в первом столбце
                                if (idxCol == 0) {
                                    onMouseClicked = EventHandler { loadSamples(9 - idxRow) }
                                }
                                rect.fill = Color.rgb(160, 160, 160)
                            }
                            // буквы доски
                            if (idxRow in arrayOf(0,9) && idxCol in 1..8)
                            {
                                label{
                                    text ="${(idxCol - 1 + 'a'.toInt()).toChar()}"
                                    style = fontStyle
                                }
                                rect.fill = Color.rgb(160, 160, 160)
                            }
                            // -----------------
                            // функциональные клетки
                            // верхний левый - запуск новой игры
                            if (idxCol == 0 && idxRow == 0){
                                onMouseClicked = EventHandler { newGame() }
                                rect.fill = Color.IVORY
                                imv.image = resources.image("/reset.png")
                            }
                            // верхний правый - игрок за черных
                            if (idxCol == 9 && idxRow == 0){
                                imageBlackPlayer = imv
                                onMouseClicked = EventHandler { changeBlack() }
                                rect.fill = Color.IVORY
                                imv.image = resources.image("/ai.png")
                            }
                            // нижний левый - игрок за белых
                            if (idxCol == 0 && idxRow == 9){
                                imageWhitePlayer = imv
                                onMouseClicked = EventHandler { changeWhite() }
                                rect.fill = Color.IVORY
                                imv.image = resources.image("/man.png")
                            }
                            // нижний правый - завершение игры
                            if (idxCol == 9 && idxRow == 9){
                                onMouseClicked = EventHandler { exitProcess(0) }
                                imv.image = resources.image("/exit.png")
                                rect.fill = Color.rgb(160, 160, 160)
                            }
                        }
                    }
                }
            }
            constraintsForRow(0).percentHeight = 4.0
            constraintsForRow(bs).percentHeight = 4.0
            constraintsForColumn(0).percentWidth = 4.0
            constraintsForColumn(bs).percentWidth = 4.0

            for (i in 1..boardSize) {
                constraintsForRow(i).percentHeight = 11.5
                constraintsForColumn(i).percentWidth = 11.5
            }
            isGridLinesVisible = true
        }
    }

    init {
        newGame()
    }

    // настроить картинку в клетке
    fun setCellImage(idx: Int) {
        if (idx in 0..31)
            cells[idx].third!!.image =
                when (ai.squares[idx]) {
                    Kind.WHITE -> resources.image("/WhiteMan.png")
                    Kind.BLACK -> resources.image("/BlackMan.png")
                    Kind.WHITE_KING -> resources.image("/WhiteKing.png")
                    Kind.BLACK_KING -> resources.image("/BlackKing.png")
                    Kind.WHITE_CAPTURED -> resources.image("/WhiteManCapture.png")
                    Kind.BLACK_CAPTURED -> resources.image("/BlackManCapture.png")
                    Kind.WHITE_KING_CAPTURED -> resources.image("/WhiteKingCapture.png")
                    Kind.BLACK_KING_CAPTURED -> resources.image("/BlackKingCapture.png")
                    else -> null
                }
    }
    // перерисовка доски по данным из AI
    private fun updateBoard() {
        for (i in 0..31) {
            // убрать окантовки
            cells[i].second?.strokeWidth = 0.0
            // загрузить правильные картинки
            setCellImage(i)
        }
        for (mv in moveCells) {
            if (mv.second < 0) {
                // не выбранная клетка
                cells[mv.first].second?.strokeWidth = 3.0
                cells[mv.first].second?.stroke =
                    if (sideAI != sideMove) Color.MAGENTA else Color.AQUA
            } else {
                // выбранная клетка
                cells[mv.first].second?.strokeWidth = 5.0
                cells[mv.first].second?.stroke = Color.YELLOW
                cells[mv.second].second?.strokeWidth = 5.0
                cells[mv.second].second?.stroke = Color.LIME
                if (mv.third >= 0) {
                    cells[mv.third].second?.strokeWidth = 5.0
                    cells[mv.third].second?.stroke = Color.RED
                }
            }
        }
    }

    private fun runGame(){
        moveCells = ai.getMoves(-1, sideMove)
        updateBoard()
        // если начинает робот, то запустить AI
        if (sideAI == sideMove) {
            //timAni = System.nanoTime()
            aiAnimation.start()
        }
    }

    fun loadSamples(num:Int){
        // только начало
        if (num > 0) {
            if (ai.toString() != "11111111111100000000222222222222") return
        }
        if(processedAnimation){
            aiAnimation.stop()
        }
        // этюды
        when(abs(num)){
            1-> ai.setBoard("B,C1,E1;w,F2,E3,G3,F4;b,F8,H8,C7,E7,G7,F6,H6")
            2-> ai.setBoard("W,B3,G1,H2,H6;B,A1,A7")
            3-> ai.setBoard("w,G1,H2;W,b8;b,D4,F6")
            4-> ai.setBoard("w,E1,G1,A5,F6;b,H2,F4,A7,F8,H8")
            5-> ai.setBoard("w,A1,B2,C1,G1,H2;b,A5,B4,E3,F2,H4")
            6-> ai.setBoard("w,C1,C3,E1,G1,G3;b,A5,C5,E5,E7,H4")
            7-> ai.setBoard("w,A3,B4,C3,E3,G1;b,A5,C7,E5,E7,H6")
            8-> ai.setBoard("w,A5,B4,B6,H2;b,B8,D4,E3,F4,G3")
            9-> ai.setBoard("w,B4,D6,H4,H6;b,A5,D4,F4,F8,G7")
            else -> ai.setBoard("")
        }
        sideMove = Kind.WHITE
        if (sideAI == Kind.WHITE) changeWhite()
        runGame()
    }

    fun newGame() { // новая игра - начальная расстановка
        if(processedAnimation){
            aiAnimation.stop()
        }
        ai.setBoard("")
        sideMove = Kind.WHITE
        moveCells = ai.getMoves(-1, sideMove)
        updateBoard()
        if (sideAI == Kind.WHITE) changeWhite()
        runGame()
    }

    fun changeWhite() {
        if(processedAnimation){
            aiAnimation.stop()
        }
        if (sideAI == Kind.WHITE) {
            // был "робот" стал "человек"
            sideAI = Kind.NONE
            imageWhitePlayer.image = resources.image("/man.png")
        } else {
            // был "человек" стал "робот"
            sideAI = Kind.WHITE
            imageBlackPlayer.image = resources.image("/man.png")
            imageWhitePlayer.image = resources.image("/ai.png")
        }
        runGame()
    }

    fun changeBlack() {
        if(processedAnimation){
            aiAnimation.stop()
        }
        if (sideAI == Kind.BLACK) {
            // был "робот" стал "человек"
            sideAI = Kind.NONE
            imageBlackPlayer.image = resources.image("/man.png")
        } else {
            // был "человек" стал "робот"
            sideAI = Kind.BLACK
            imageWhitePlayer.image = resources.image("/man.png")
            imageBlackPlayer.image = resources.image("/ai.png")
        }
        runGame()
    }

    // кликнули по клетке
    private fun selectCell(idx_click: Int) {
        // работает интеллект - не вмешиваться
        if(processedAnimation) return
        // проверить выбран ли доступный ход
        val moveCell = moveCells.filter { it.second == idx_click }
        //
        if (moveCell.isNotEmpty()) {
            // да, ход есть, выполнить его
            ai.moveChecker(moveCell[0])
            // проверка на окончание взятия
            val ss = ai.getCapture(idx_click)
            if (ss.isNotEmpty() && moveCell[0].third >= 0) {
                // если есть, то перерисовать и выйти
                moveCells = ai.getMoves(idx_click, sideMove)
                // нарисовать изменения
                updateBoard()
                return
            }
            ai.clearCaptured()
            // смена хода
            sideMove = if (sideMove == Kind.WHITE) Kind.BLACK else Kind.WHITE

            moveCells = ai.getMoves(-1, sideMove)
            if (moveCells.isEmpty()) {
                updateBoard()
                val mess =
                    if (sideMove == Kind.WHITE) "Black is win!" else "White is win!"
                find<GameOverWindow>(mapOf(GameOverWindow::message to mess, GameOverWindow::board to this@Board))
                    .openModal(resizable = false)
                return
            }
            // нарисовать изменения
            updateBoard()
            // продолжаем
            runGame()
        } else {
            // выбрана другая клетка - хода нет
            if (moveCells.isNotEmpty() && moveCells[0].third >= 0) {
                // нужно обязательно ходить
                if (ai.getMoves(idx_click, sideMove).isEmpty()) return
            }
            // снять выделения
            for (idx in 0..31) {
                cells[idx].second?.strokeWidth = 0.0
            }
            // получить ходы для новой клетки и для всех возможных
            moveCells = ai.getMoves(idx_click, sideMove)
            // нарисовать изменения
            updateBoard()
        }
    }
}