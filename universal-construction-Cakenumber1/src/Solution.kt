/**
 * @author : Nikolaev Mikhail
 */
class Solution : AtomicCounter {
    // объявите здесь нужные вам поля
    private val root = Node()
    private val last = ThreadLocal.withInitial { root }

    override fun getAndAdd(x: Int): Int {
        // напишите здесь код
        var node: Node
        var old: Int
        do {
            old = last.get().v
            node = Node(old + x)
            last.set(last.get().next.decide(node))
        } while (node !== last.get())
        return old
    }

    // вам наверняка потребуется дополнительный класс
    private class Node(val v: Int = 0) {
        val next: Consensus<Node> = Consensus()

    }
}