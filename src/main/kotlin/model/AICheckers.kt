package model

// http://journal-shkolniku.ru/shashki.html
// https://www.kombinashki.ru/osnov_page1.php
// =============================================================================================
enum class Kind(private val symbol: Char) {
    NONE(' '),
    WHITE('w'),
    BLACK('b'),
    WHITE_KING('W'),
    BLACK_KING('B'),
    WHITE_CAPTURED('ẉ'),
    BLACK_CAPTURED('ḇ'),
    WHITE_KING_CAPTURED('Ẉ'),
    BLACK_KING_CAPTURED('Ḇ');

    override fun toString(): String = symbol.toString()

    val isWhite: Boolean
        get() = this == WHITE || this == WHITE_KING
    val isBlack: Boolean
        get() = this == BLACK || this == BLACK_KING
    val isFree: Boolean
        get() = this == NONE
}

// =============================================================================================
class AIBoard {
    val squares: Array<Kind> = Array(32) { Kind.NONE }
    private var curDepth = 0
    private var maxDepth = 4
    // ============================================================================
    // функции отображения и начальной расстановки
    // ============================================================================
    // вывод позиции в виде строки 32 символов
    override fun toString(): String {
        val sb = StringBuilder()
        for (cell in squares) {
            sb.append(cell.ordinal)
        }
        return sb.toString()
    }
    // вывод позиции в псевдографическом виде для терминалки
    fun toPlan(): String {
        val sb = StringBuilder()
        sb.append("   a b c d e f g h\n")
        for (i in 8 downTo 1) {
            sb.append(i).append(" |")
            for (j in 0..7) {
                if ((i + j) % 2 == 0) sb.append("·|")
                else sb.append(squares[(i - 1) * 4 + j / 2]).append("|")
            }
            sb.append(" ").append(i).append("\n")
        }
        sb.append("   a b c d e f g h\n")
        return sb.toString()
    }
    // инициализация позиции из строки с 32-мя символами (toString)
    private fun strToBoard(line: String) {
        if (line.length != 32) return
        for (i in 0..31) {
            squares[i] = when (line[i]) {
                '1' -> Kind.WHITE
                '2' -> Kind.BLACK
                '3' -> Kind.WHITE_KING
                '4' -> Kind.BLACK_KING
                '5' -> Kind.WHITE_CAPTURED
                '6' -> Kind.BLACK_CAPTURED
                '7' -> Kind.WHITE_KING_CAPTURED
                '8' -> Kind.BLACK_KING_CAPTURED
                else -> Kind.NONE
            }
        }
    }
    // ----------------------
    // очистка доски от шашек
    private fun clear() {
        for (i in 0..31) {
            squares[i] = Kind.NONE
        }
    }
    // --------------------------------
    // снятие с доски захваченных шашек
    fun clearCaptured() {
        for (i in 0..31) {
            if (squares[i] in Kind.WHITE_CAPTURED..Kind.BLACK_KING_CAPTURED) squares[i] = Kind.NONE
        }
    }
    // ----------------------------------------------
    // расстановка из строки с именами ячеек
    fun setBoard(pos: String) {
        // очистить всю доску
        clear()
        // если строка пустая, то стандартная расстановка
        if (pos.isEmpty()) {
            for (i in 0..11) {
                squares[i] = Kind.WHITE
                squares[31 - i] = Kind.BLACK
            }
            return
        }
        // если есть строка, то расстановка этюда
        var c = Kind.NONE
        for (word in pos.split(",", " ", ";")) {
            when (word.trim()) {
                "w" -> c = Kind.WHITE
                "W" -> c = Kind.WHITE_KING
                "b" -> c = Kind.BLACK
                "B" -> c = Kind.BLACK_KING
                else -> {
                    val i = nameToIdx(word)
                    if (i in 0..31) squares[i] = c
                }
            }
        }
    }
    // ============================================================================
    //  функции изменения статуса клетки
    // ============================================================================
    // перевод клетки в захваченное состояние
    private fun capture(n: Int) {
        when (squares[n]) {
            Kind.WHITE -> squares[n] = Kind.WHITE_CAPTURED
            Kind.WHITE_KING -> squares[n] = Kind.WHITE_KING_CAPTURED
            Kind.BLACK -> squares[n] = Kind.BLACK_CAPTURED
            Kind.BLACK_KING -> squares[n] = Kind.BLACK_KING_CAPTURED
            else -> {}
        }
    }

    // ============================================================================
    private fun unCapture(n: Int) {
    // перевод клетки из захваченного состояния
        when (squares[n]) {
            Kind.WHITE_CAPTURED -> squares[n] = Kind.WHITE
            Kind.WHITE_KING_CAPTURED -> squares[n] = Kind.WHITE_KING
            Kind.BLACK_CAPTURED -> squares[n] = Kind.BLACK
            Kind.BLACK_KING_CAPTURED -> squares[n] = Kind.BLACK_KING
            else -> {}
        }
    }
    // ============================================================================
    // переместить шашку на заданное поле со взятием или без
    // first - с какой ячейки
    // second - в какую ячейку
    // third - какую захватить
    fun moveChecker(mv: Triple<Int, Int, Int>) {
        squares[mv.second] = squares[mv.first]
        if (mv.second > 27 && squares[mv.second] == Kind.WHITE) squares[mv.second] = Kind.WHITE_KING
        if (mv.second < 4 && squares[mv.second] == Kind.BLACK) squares[mv.second] = Kind.BLACK_KING
        squares[mv.first] = Kind.NONE
        if (mv.third >= 0)
            capture(mv.third)
    }
    // ============================================================================
    //  функции реализующие правила игры
    // ============================================================================
    // получение ходов для указанного цвета шашек
    private fun getNextMoves(actKind: Kind): Map<String, ArrayList<String>> {
        val res: MutableMap<String, ArrayList<String>> = HashMap()
        var captureMode = false
        if (actKind.isWhite) // ходы белых
        {
            for (idx in 0..31) {
                val cell = squares[idx]
                if (!cell.isWhite) continue
                val listCapt = getCapture(idx)
                if (listCapt.size > 0) {
                    if (!captureMode) {
                        captureMode = true
                        res.clear()
                    }
                    res[nameCeil(idx)] = listCapt
                }
                if (captureMode) continue
                // добавление в map
                res[nameCeil(idx)] = listCapt
                for (dir in 0..3) {
                    var nxt = corners[idx][dir]
                    while (nxt != -1 && squares[nxt].isFree) {
                        listCapt.add(nameCeil(nxt))
                        if (cell == Kind.WHITE) break // не дамка только один шаг
                        nxt = corners[nxt][dir]
                    }
                    if (dir == 1) if (cell == Kind.WHITE) break // не дамка только вперёд
                }
            }
        } else { // ходы чёрных
            for (i in 0..31) {
                val c = squares[i]
                if (!c.isBlack) continue
                val lst = getCapture(i)
                if (lst.size > 0) {
                    if (!captureMode) {
                        captureMode = true
                        res.clear()
                    }
                    res[nameCeil(i)] = lst
                }
                if (captureMode) continue
                res[nameCeil(i)] = lst
                for (dir in 3 downTo 0) {
                    var nxt = corners[i][dir]
                    while (nxt != -1 && squares[nxt].isFree) {
                        lst.add(nameCeil(nxt))
                        if (c == Kind.BLACK) break // не дамка один шаг
                        nxt = corners[nxt][dir]
                    }
                    if (dir == 2 && c == Kind.BLACK) break // не дамка только вперёд
                }
            }
        }
        res.entries.removeIf { (_, value): Map.Entry<String, ArrayList<String>> -> value.isEmpty() }
        return res
    }
    // -----------------------------------------------------------------
    // получение списка ходов для указанной клетки
    // если клетка не указана, то для всех которые не блокированы
    fun getMoves(n: Int, self: Kind): MutableList<Triple<Int, Int, Int>> {
        val res = mutableListOf<Triple<Int, Int, Int>>()
        if (self !in Kind.WHITE..Kind.BLACK) return res
        val idx = if (n in 0..31) n else -1
        val mv = getNextMoves(self)
        if (mv.isEmpty()) return res
        for ((k, v) in mv.entries) {
            val id = nameToIdx(k)
            if (id != idx) {
                // клетка не выбрана, но неё есть ходы
                res.add(Triple(id, -1, -1))
                continue
            }
            // выбранная клетка
            for (name in v) {
                //println(name)
                if (name.substring(0, 1) == "x") {
                    val cc = name.split(":", "->")
                    res.add(Triple(idx, nameToIdx(cc[1]), nameToIdx(cc[0].substring(2, 4))))
                } else {
                    res.add(Triple(idx, nameToIdx(name), -1))
                }
            }
        }
        return res
    }
    // --------------------------------------------------------------------
    // поиск шашек которые можно захватить
    fun getCapture(n: Int, self: Kind = Kind.NONE): ArrayList<String> {
        val res = ArrayList<String>()
        var cell = squares[n]
        if (self != Kind.NONE) cell = self
        if (cell == Kind.NONE) return res
        when (cell) {
            Kind.WHITE -> {
                var dir = 0
                while (dir < 4) {
                    val nxt1 = corners[n][dir]
                    if (nxt1 == -1) {
                        dir++
                        continue
                    }
                    val nxt2 = corners[nxt1][dir]
                    if (nxt2 == -1) {
                        dir++
                        continue
                    }
                    if (squares[nxt2].isFree && squares[nxt1].isBlack) {
                        capture(nxt1)
                        val svSq = squares[n]
                        squares[n] = Kind.NONE
                        val nxtCap = getCapture(nxt2, if (nxt2 > 27) Kind.WHITE_KING else Kind.WHITE)
                        squares[n] = svSq
                        unCapture(nxt1)
                        val s = "x(" + nameCeil(nxt1) + ")" + squares[nxt1].toString() + ":" + nameCeil(nxt2)
                        if (nxtCap.isNotEmpty()) {
                            for (nxt in nxtCap) {
                                res.add("$s->$nxt")
                            }
                        } else res.add(s)
                    }
                    dir++
                }
            }
            Kind.WHITE_KING -> {
                var dir = 0
                while (dir < 4) {
                    var nxt1 = corners[n][dir]
                    while (nxt1 != -1 && squares[nxt1].isFree) nxt1 = corners[nxt1][dir]
                    if (nxt1 == -1 || !squares[nxt1].isBlack) {
                        dir++
                        continue
                    }
                    var nxt2 = corners[nxt1][dir]
                    if (nxt2 == -1) {
                        dir++
                        continue
                    }
                    if (squares[nxt2].isFree && squares[nxt1].isBlack) {
                        while (nxt2 != -1 && squares[nxt2].isFree) {
                            capture(nxt1)
                            val svSq = squares[n]
                            squares[n] = Kind.NONE
                            val nxtCap = getCapture(nxt2, Kind.WHITE_KING)
                            squares[n] = svSq
                            unCapture(nxt1)
                            val s = "x(" + nameCeil(nxt1) + ")" + squares[nxt1].toString() + ":" + nameCeil(nxt2)
                            if (nxtCap.isNotEmpty()) {
                                for (nxt in nxtCap) {
                                    res.add("$s->$nxt")
                                }
                            } else res.add(s)
                            nxt2 = corners[nxt2][dir]
                        }
                    }
                    dir++
                }
            }
            Kind.BLACK -> {
                var dir = 0
                while (dir < 4) {
                    val nxt1 = corners[n][dir]
                    if (nxt1 == -1) {
                        dir++
                        continue
                    }
                    val nxt2 = corners[nxt1][dir]
                    if (nxt2 == -1) {
                        dir++
                        continue
                    }
                    if (squares[nxt2].isFree && squares[nxt1].isWhite) {
                        capture(nxt1)
                        val svSq = squares[n]
                        squares[n] = Kind.NONE
                        val nxtCap = getCapture(nxt2, if (nxt2 < 4) Kind.BLACK_KING else Kind.BLACK)
                        squares[n] = svSq
                        unCapture(nxt1)
                        val s = "x(" + nameCeil(nxt1) + ")" + squares[nxt1].toString() + ":" + nameCeil(nxt2)
                        if (nxtCap.isNotEmpty()) {
                            for (nxt in nxtCap) {
                                res.add("$s->$nxt")
                            }
                        } else res.add(s)
                    }
                    dir++
                }
            }
            Kind.BLACK_KING -> {
                var dir = 0
                while (dir < 4) {
                    var nxt1 = corners[n][dir]
                    while (nxt1 != -1 && squares[nxt1].isFree) nxt1 = corners[nxt1][dir]
                    if (nxt1 == -1 || !squares[nxt1].isWhite) {
                        dir++
                        continue
                    }
                    var nxt2 = corners[nxt1][dir]
                    if (nxt2 == -1) {
                        dir++
                        continue
                    }
                    if (squares[nxt2].isFree && squares[nxt1].isWhite) {
                        while (nxt2 != -1 && squares[nxt2].isFree) {
                            capture(nxt1)
                            val svSq = squares[n]
                            squares[n] = Kind.NONE
                            val nxtCap = getCapture(nxt2, Kind.BLACK_KING)
                            squares[n] = svSq
                            unCapture(nxt1)
                            val s = "x(" + nameCeil(nxt1) + ")" + squares[nxt1].toString() + ":" + nameCeil(nxt2)
                            if (nxtCap.isNotEmpty()) {
                                for (nxt in nxtCap) {
                                    res.add("$s->$nxt")
                                }
                            } else res.add(s)
                            nxt2 = corners[nxt2][dir]
                        }
                    }
                    dir++
                }
            }
            else -> {}
        }
        return res
    }
    // ============================================================================
    // функции интеллекта
    // ============================================================================
    // проверка шашки на возможность двигаться
    fun canMove(n: Int): Boolean {
        when (squares[n]) {
            Kind.WHITE -> {
                for (dir in 0..1) {
                    val nxt1 = corners[n][dir]
                    if (nxt1 == -1) continue
                    if (squares[nxt1].isFree) return true
                    if (squares[nxt1].isWhite) continue
                    val nxt2 = corners[nxt1][dir]
                    if (nxt2 == -1) continue
                    if (squares[nxt2].isFree) return true
                }
                return false
            }
            Kind.WHITE_KING -> {
                for (dir in 0..3) {
                    val nxt1 = corners[n][dir]
                    if (nxt1 == -1) continue
                    if (squares[nxt1].isFree) return true
                    if (squares[nxt1].isWhite) continue
                    val nxt2 = corners[nxt1][dir]
                    if (nxt2 == -1) continue
                    if (squares[nxt2].isFree) return true
                }
                return false
            }
            Kind.BLACK -> {
                for (dir in 2..3) {
                    val nxt1 = corners[n][dir]
                    if (nxt1 == -1) continue
                    if (squares[nxt1].isFree) return true
                    if (squares[nxt1].isBlack) continue
                    val nxt2 = corners[nxt1][dir]
                    if (nxt2 == -1) continue
                    if (squares[nxt2].isFree) return true
                }
                return false
            }
            Kind.BLACK_KING -> {
                for (dir in 0..3) {
                    val nxt1 = corners[n][dir]
                    if (nxt1 == -1) continue
                    if (squares[nxt1].isFree) return true
                    if (squares[nxt1].isBlack) continue
                    val nxt2 = corners[nxt1][dir]
                    if (nxt2 == -1) continue
                    if (squares[nxt2].isFree) return true
                }
                return false
            }
            else -> return false
        }
    }
    // --------------------------------------------
    // оценка текущей позиции со стороны белых
    fun score(): Int {
        var cntWhite = 0
        var cntBlack = 0
        var res = 0
        for (idx in 0..31) {
            //if (!canMove(idx)) continue
            if (squares[idx].isWhite) cntWhite++
            else if (squares[idx].isBlack) cntBlack++
            when (squares[idx]) {
                Kind.WHITE -> res += if (idx in arrayOf(13, 14, 17, 18)) 25 else 10
                Kind.BLACK -> res -= if (idx in arrayOf(13, 14, 17, 18)) 25 else 10
                Kind.WHITE_KING -> res += when (idx) {
                    // 13, 14, 17, 18 -> 70
                    0, 4, 9, 22, 27, 31 -> 60
                    else -> 50
                }
                Kind.BLACK_KING -> res -= when (idx) {
                    // 13, 14, 17, 18 -> 70
                    0, 4, 9, 22, 27, 31 -> 60
                    else -> 50
                }
                else -> {}
            }
        }
        if (cntWhite == 0) res -= 1000
        if (cntBlack == 0) res += 1000
        return res
    }
    //------------------------------------
    // расчет цены хода
    private fun scoreOfMove(n: Int, move: String): Int {
        var rate = 100010 - curDepth * 10
        val self = if (squares[n].isWhite) Kind.WHITE else
            if (squares[n].isBlack) Kind.BLACK else Kind.NONE
        if (self == Kind.NONE) return -10000000
        val enemyKind = if (self == Kind.WHITE) Kind.BLACK else Kind.WHITE
        val mul = if (self == Kind.WHITE) 1 else -1
        curDepth++
        val savePos = toString()
        // -----------------------------------------
        // выполнить свой ход
        var mySqCur = n
        for (mvs in move.split("->")) {
            val cc = mvs.split(":")
            if (cc.size == 2) {// взятие
                moveChecker(Triple(mySqCur, nameToIdx(cc[1]), nameToIdx(cc[0].substring(2, 4))))
                mySqCur = nameToIdx(cc[1])
            } else { // простой ход
                moveChecker(Triple(mySqCur, nameToIdx(cc[0]), -1))
                mySqCur = nameToIdx(cc[0])
            }
        }
        clearCaptured() // убрать захваченные
        // ******************
        val curPos = toString()
        // получаем ходы противника
        val enemyMoves = getNextMoves(enemyKind)
        // перебираем шашки имеющие возможность ходить у противника
        for ((eSq, eMoves) in enemyMoves.entries) {
            // перебираем ходы этой шашки противника
            strToBoard(curPos)// восстанавливаем доску
            var eSqCur = eSq
            for (eMoveSq in eMoves) {
                // выполняем ход(цепочку) проверяемой шашки противника
                for (eMove in eMoveSq.split("->")) {
                    val cc = eMove.split(":")
                    if (cc.size == 2) {// взятие
                        moveChecker(Triple(nameToIdx(eSqCur), nameToIdx(cc[1]), nameToIdx(cc[0].substring(2, 4))))
                        eSqCur = cc[1]
                    } else { // простой ход
                        moveChecker(Triple(nameToIdx(eSqCur), nameToIdx(cc[0]), -1))
                        eSqCur = cc[0]
                    }
                }
                // удаляем убитые шашки
                clearCaptured()
                // проверяем на достижение глубины
                if (curDepth >= maxDepth) {
                    val r = mul * score()
                    if (r < rate) rate = r
                } else { // продолжаем поиск в глубину
                    // готовим список
                    var score = -100001 // будем искать лучший
                    // получаем свои возможные ходы в новой позиции
                    val myMoveSq = getNextMoves(self)
                    // перебираем наши шашки с возможными ходами
                    for ((k, v) in myMoveSq.entries) {
                        for (name in v) {
                            val lvl = scoreOfMove(nameToIdx(k), name)
                            if (lvl > score) score = lvl
                        }
                    }
                    if (score == -100001) score = -100000 + curDepth
                    if (score < rate) rate = score
                }
            }
        }
        // -----------------------------------------
        strToBoard(savePos)
        curDepth--
        return rate
    }
    // ============================================================================
    // продумывание ходов указанного цвета
    fun getAIMoves(self: Kind): List<Triple<Int, Int, Int>> {
        val result = mutableListOf<Triple<Int, Int, Int>>()
        val mv = getNextMoves(self)
        if (mv.isEmpty()) return result
        //val mul = if (self == Kind.WHITE) 1 else -1
        val scoreList = mutableListOf<Triple<Int, String, String>>()
        // перебираем возможные ходы
        val thrList: ArrayList<Thread> = arrayListOf()
        for ((k, v) in mv.entries) {
            for (name in v) {
                if (mv.size == 1 && v.size == 1) {// если один ход, то думать незачем
                    scoreList.add(Triple(0, k, name))
                } else {
                    // формирование потока для расчета
                    val thr = Thread { // Runnable
                        val threadBoard = AIBoard()
                        threadBoard.strToBoard(toString())
                        kotlin.run {
                            val res = threadBoard.scoreOfMove(nameToIdx(k), name)
                            synchronized(scoreList) {
                                scoreList.add(Triple(res, k, name))
                            }
                        }
                    }
                    // запуск потока для расчета
                    thr.start()
                    // занесение потока в список
                    thrList.add(thr)
                    // вызов при однопоточном режиме
                    //scoreList.add(Triple(scoreOfMove(nameToIdx(k), name), k, name))
                }
            }
        }
        // ожидание завершения всех потоков
        for (thr in thrList) {
            thr.join()
        }
        // ищем самый лучший ход
        // println("$self $scoreList")
        val res = scoreList.maxBy { it.first }!!
        var mySq = res.second
        for (move in res.third.split("->")) {
            val cc = move.split(":")
            if (cc.size == 2) {// взятие
                result.add(Triple(nameToIdx(mySq), nameToIdx(cc[1]), nameToIdx(cc[0].substring(2, 4))))
                mySq = cc[1]
            } else { // простой ход
                result.add(Triple(nameToIdx(mySq), nameToIdx(cc[0]), -1))
                mySq = cc[0]
            }
        }
        return result
    }
    // ===============================================================
    // вспомогательные static функции
    companion object {
        // таблица угловых переходов на соседние клетки
        // UpLeft, UpRight, DownLeft, DownRight
        val corners: Array<IntArray> = arrayOf(
            intArrayOf(-1, 4, -1, -1), // 0
            intArrayOf(4, 5, -1, -1),  // 1
            intArrayOf(5, 6, -1, -1),  // 2
            intArrayOf(6, 7, -1, -1),  // 3
            intArrayOf(8, 9, 0, 1),    // 4
            intArrayOf(9, 10, 1, 2),   // 5
            intArrayOf(10, 11, 2, 3),  // 6
            intArrayOf(11, -1, 3, -1), // 7
            intArrayOf(-1, 12, -1, 4), // 8
            intArrayOf(12, 13, 4, 5),  // 9
            intArrayOf(13, 14, 5, 6),  //10
            intArrayOf(14, 15, 6, 7),  //11
            intArrayOf(16, 17, 8, 9),  //12
            intArrayOf(17, 18, 9, 10), //13
            intArrayOf(18, 19, 10, 11),//14
            intArrayOf(19, -1, 11, -1),//15
            intArrayOf(-1, 20, -1, 12),//16
            intArrayOf(20, 21, 12, 13),//17
            intArrayOf(21, 22, 13, 14),//18
            intArrayOf(22, 23, 14, 15),//19
            intArrayOf(24, 25, 16, 17),//20
            intArrayOf(25, 26, 17, 18),//21
            intArrayOf(26, 27, 18, 19),//22
            intArrayOf(27, -1, 19, -1),//23
            intArrayOf(-1, 28, -1, 20),//24
            intArrayOf(28, 29, 20, 21),//25
            intArrayOf(29, 30, 21, 22),//26
            intArrayOf(30, 31, 22, 23),//27
            intArrayOf(-1, -1, 24, 25),//28
            intArrayOf(-1, -1, 25, 26),//29
            intArrayOf(-1, -1, 26, 27),//30
            intArrayOf(-1, -1, 27, -1) //31
        )
       // преобразование имени в индекс: "A1" -> 0
        fun nameToIdx(str: String): Int {
            if (str.length != 2) return -1
            val letter = str[0].toUpperCase().toInt() - 'A'.toInt()
            val dig = str[1].toInt() - '1'.toInt()
            return if (letter !in 0..7 || dig !in 0..7) -1 else dig * 4 + letter / 2
        }
        // получение имени клетки из индекса: 1 -> "C1"
        fun nameCeil(idx: Int): String {
            if (idx < 0) return "--"
            if (idx > 31) return "++"
            val letter = idx % 4 * 2 + (idx shr 2) % 2 + 'A'.toInt()
            val dig = idx / 4 + '1'.toInt()
            return StringBuilder().append(letter.toChar()).append(dig.toChar()).toString()
        }
    }
}

object AICheckers {
    @JvmStatic
    fun main(args: Array<String>) {
        val brd = AIBoard()
        //for (i in 0..31) print(" " + AIBoard.nameCeil(i))
        //println()
        brd.setBoard("")
        println(brd)
        //System.out.println(brd.toPlan());
        //1. ed4 с:g5 2. de5 f:d4 3. gh4 е:gЗ 4. h:h4.
//        brd.setBoard("w,F2,E3,G3,F4;b,F8,H8,C7,E7,G7,F6,H6;B,C1,E1");
        //1. bс5 g:а7 2. cd4 а:g1 3. de3 g:а1 4. аb4 а:сЗ 5. cb2 cd2 6 е:сЗ.
        //brd.setBoard("w,A3,B2,B4,C1,C3,D2,E1;b,A5;B,G1")
        //brd.setBoard("w,B6;b,C7,F6;B,D2");
        println(brd.toPlan())
        for (i in 0..40) {
            println("$i White move")
            var mm = brd.getAIMoves(Kind.WHITE)
            if (mm.isEmpty()) {
                println("Black win!")
                break
            }
            println("W $mm")
            for (move in mm) {
                brd.moveChecker(move)
            }
            brd.clearCaptured()
            println(brd.toPlan())
            println(brd.score())
            println("$i Black move")
            mm = brd.getAIMoves(Kind.BLACK)
            if (mm.isEmpty()) {
                println("White win!")
                break
            }
            println("B $mm")
            for (move in mm) {
                brd.moveChecker(move)
            }
            brd.clearCaptured()
            println(brd.toPlan())
            println(brd.score())
        }

        //println(brd.getNextMoves(Kind.WHITE))
        //println(brd.getNextMoves(Kind.BLACK))
        //println(brd.getCapture(nameToIdx("B6")));
        //System.out.println(brd.getCapture(Board.nameToIdx("D2")));
    }
}
/* */