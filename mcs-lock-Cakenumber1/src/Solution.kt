import java.util.concurrent.atomic.AtomicReference


class Solution(private val env: Environment) : Lock<Solution.Node?> {
    private val tail: AtomicReference<Node?> = AtomicReference()
    override fun lock(): Node {
        val temp = Node()
        temp.locked.set(true)
        val pred = tail.getAndSet(temp)
        if (pred != null) {
            pred.next.set(temp)
            while (temp.locked.get()) {
                env.park()
            }
        }
        return temp
    }

    override fun unlock(node: Node?) {
        if (node != null) {
            if (node.next.get() == null) {
                if (tail.compareAndSet(node, null)) {
                    return
                } else {
                    while (node.next.get() == null) {
                        //DO NOTHING
                    }
                }
            }
            node.next.get()!!.locked.set(false)
            env.unpark(node.next.get()!!.thread)
        }
    }

    class Node {
        val thread: Thread = Thread.currentThread()
        val locked = AtomicReference(false)
        val next = AtomicReference<Node?>()
    }

}