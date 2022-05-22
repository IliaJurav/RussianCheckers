import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import model.*

class Tests{

    @Test
    fun checkConvertFun() {
        // проверка корректности работы функций преобразования
        // строкового и числового названия клеток друг в друга
        val nameCell = listOf(
            "A1","C1","E1","G1","B2","D2","F2","H2",
            "A3","C3","E3","G3","B4","D4","F4","H4",
            "A5","C5","E5","G5","B6","D6","F6","H6",
            "A7","C7","E7","G7","B8","D8","F8","H8"
        )
        for (i in 0..31)
        {
            val s = AIBoard.nameCeil(i)
            assertEquals(nameCell[i],s)
            assertEquals(i,AIBoard.nameToIdx(s))
        }
    }

    @Test
    fun createBoard() {
        val ai = AIBoard()
            // после создания доска пуста
            assertEquals("00000000000000000000000000000000", ai.toString())
            // стандартная начальная расстановка
            ai.setBoard("")
            assertEquals("11111111111100000000222222222222", ai.toString())
            // Загрузка этюда
            ai.setBoard("B,C1,E1;w,F2,E3,G3,F4;b,F8,H8,C7,E7,G7,F6,H6")
            assertEquals("04400010001100100000002202220022", ai.toString())
        }

    @Test
    fun checkGetAvailsMoves() {
        val ai = AIBoard()
        ai.setBoard("")
        // поиск разрешённых клеток для хода у белых
        // при модификации алгоритма порядок может отличаться,
        // поэтому упорядочиваем, так как результат фиксирован
        val res = ai.getMoves(-1, Kind.WHITE).sortedBy { it.first }
        assertEquals(
                listOf(
                    Triple(AIBoard.nameToIdx("A3"), -1, -1),
                    Triple(AIBoard.nameToIdx("C3"), -1, -1),
                    Triple(AIBoard.nameToIdx("E3"), -1, -1),
                    Triple(AIBoard.nameToIdx("G3"), -1, -1)
                ), res
            )
        // поиск разрешённых клеток для хода у чёрных
        val res2 = ai.getMoves(-1,Kind.BLACK).sortedBy { it.first }
        assertEquals(
            listOf(
                Triple(AIBoard.nameToIdx("B6"), -1, -1),
                Triple(AIBoard.nameToIdx("D6"), -1, -1),
                Triple(AIBoard.nameToIdx("F6"), -1, -1),
                Triple(AIBoard.nameToIdx("H6"), -1, -1)
            ), res2
        )
    }

    @Test
    fun score() {
        val ai = AIBoard()
        ai.setBoard("")
        // проверка балансировки оценки позиции
        // исходная позиция должна всегда давать ноль
        assertEquals(0, ai.score())
    }

    @Test
    fun checkGetCaptureAndAI() {
        val ai = AIBoard()
        ai.setBoard("b,E3,G3,C5,E5,C7,E7;w,D4")
        val listCap = ai.getCapture(AIBoard.nameToIdx("D4")).maxBy { it.length  }
        // самый длинный ход, должны быть сбиты (6) все черные шашки
        assertEquals(
            "x(C5)b:B6->x(C7)b:D8->x(E7)b:F6->x(E5)b:D4->x(E3)b:F2->x(G3)b:H4",
             listCap
        )
        // аналогичную последовательность должен выбрать AI,
        // последовательность должна состоять из шести ходов,
        // порядок ходов может отличаться в зависимости
        // изменений в оценке позиции (AI.score)
        // и разном времени работы потоков поиска
        val aiMoves = ai.getAIMoves(Kind.WHITE)
        assertEquals(6 , aiMoves.size)
    }
}