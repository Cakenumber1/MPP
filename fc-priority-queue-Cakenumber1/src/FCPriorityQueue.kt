import kotlinx.atomicfu.*
import java.util.*

class FCPriorityQueue<E : Comparable<E>> {
    val locked = atomic(false)

    fun tryLock() = locked.compareAndSet(false, true)

    fun unLock() {
        locked.value = false
    }

    private val q = PriorityQueue<E>()


    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        return command("poll", null)
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        return command("peek", null)
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        command("add", element)
    }

    private val fc_array = atomicArrayOfNulls<TreadCom>(100)

    inner class TreadCom(
        val command: String,
        val argument: E?,
    ) {
        val result: AtomicRef<E?> = atomic(null)
        val flag = atomic(false)
    }

    private fun commandReader(command: TreadCom) {
        when (command.command) {
            "add" -> q.add(command.argument)
            "peek" -> command.result.value = q.peek()
            "poll" -> command.result.value = q.poll()
        }
        command.flag.value = true
    }

    private fun command(commandName: String, element: E?): E? {
        val c = TreadCom(commandName, element)
        val r = Random()

        if (tryLock()) {
            for (i in 0 until 100) {
                val value = fc_array[i].value
                if (value != null) {
                    commandReader(value)
                    fc_array[i].value = null
                }
            }
            commandReader(c)
            unLock()
            return c.result.value
        }
        var index = r.nextInt(100)
        while (true) {
            if (fc_array[index].compareAndSet(null, c)) {
                while (!c.flag.value) {
                    if (tryLock()) {
                        if (c.flag.value) {
                            unLock()
                            break
                        }
                        for (i in 0 until 100) {
                            val value = fc_array[i].value
                            if (value != null) {
                                commandReader(value)
                                fc_array[i].value = null
                            }
                        }
                        unLock()
                    }
                }
                return c.result.value
            } else {
                index = (index + 1) % 100
            }
        }
    }


}