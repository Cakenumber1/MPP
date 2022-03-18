package dijkstra

import kotlinx.atomicfu.atomic
import java.util.*
import java.util.concurrent.Phaser
import kotlin.Comparator
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val q = Queue(workers, NODE_DISTANCE_COMPARATOR)
    q.inc(start)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (true) {
                val cur: Node = q.poll() ?: if (q.isEmpty()) break else continue
                for (e in cur.outgoingEdges) {
                    while (true) {
                        val oldDistance = e.to.distance
                        val newDistance = cur.distance + e.weight
                        if (oldDistance > newDistance) {
                            if (e.to.casDistance(oldDistance, newDistance)) {
                                q.inc(e.to)
                            } else continue
                        }
                        break
                    }
                }
                q.dec()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}

private class Queue(val workers: Int, comparator: Comparator<Node>) {
    val queueList: MutableList<PriorityQueue<Node>> = Collections.nCopies(workers, PriorityQueue(comparator))
    val size = atomic(0)
    val random = Random()

    fun poll(): Node? {
        var i = 0
        var j = 0
        while (i == j) {
            i = random.nextInt(workers)
            j = random.nextInt(workers)
        }
        synchronized(queueList[i]) {
            synchronized(queueList[j]) {
                val isFirstEmpty = queueList[i].isEmpty()
                val isSameEmptiness = isFirstEmpty == queueList[j].isEmpty()
                val pollIndex = if (isSameEmptiness)
                    if (!isFirstEmpty)
                        if (queueList[i].peek().distance < queueList[j].peek().distance) i else j
                    else -1
                else if (isFirstEmpty) j else i
                return if (pollIndex != -1) queueList[pollIndex].poll() else null
            }
        }
    }

    fun isEmpty(): Boolean {
        return size.compareAndSet(0, 0)
    }

    fun inc(element: Node) {
        val randomIndex = random.nextInt(workers)
        synchronized(queueList[randomIndex]) {
            queueList[randomIndex].add(element)
        }

        size.incrementAndGet()
    }

    fun dec() {
        size.decrementAndGet()
    }
}
