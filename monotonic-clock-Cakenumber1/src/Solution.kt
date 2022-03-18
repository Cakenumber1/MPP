/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author : Nikolaev Mikhail
 */
class Solution : MonotonicClock {
    private val d1 = RegularInt(0)
    private val d2 = RegularInt(0)
    private val d3 = RegularInt(0)

    private val c1 = RegularInt(0)
    private val c2 = RegularInt(0)
    override fun write(time: Time) {
        // write left-to-right
        d1.value = time.d1
        d2.value = time.d2
        d3.value = time.d3

        // write right-to-left
        c2.value = time.d2
        c1.value = time.d1
    }

    override fun read(): Time {
        // read left-to-right
        val c1Value = c1.value
        val c2Value = c2.value

        // read right-to-left
        val d3Value = d3.value
        val d2Value = d2.value
        val d1Value = d1.value
        return Time(
            d1Value,
            if (c1Value == d1Value) d2Value else 0,
            if (c1Value == d1Value && c2Value == d2Value) d3Value else 0
        )
    }
}