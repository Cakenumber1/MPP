import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

open class Node {
    val next = AtomicReference<Node>(null)
}

class ConsumerThr<E>(
    val action: Continuation<E>
) : Node()

class ProducerThr<E>(
    val element: E,
    val action: Continuation<Unit>
) : Node()

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    private val head: AtomicReference<Node>
    private val tail: AtomicReference<Node>

    init {
        val temp = Node()
        head = AtomicReference(temp)
        tail = AtomicReference(temp)
    }

    override suspend fun send(element: E) {
        while (true) {
            val curTail = tail.get()
            if ((curTail == head.get()) || (curTail is ProducerThr<*>)) {
                val res = suspendCoroutine<Any?> sc@{ c ->
                    val newTail = ProducerThr(element, c)
                    val oldTail = tail.get()
                    if (oldTail != null) {
                        if (((oldTail == head.get()) || oldTail is ProducerThr<*>) && oldTail.next.compareAndSet(
                                null,
                                newTail
                            )
                        ) {
                            tail.compareAndSet(oldTail, newTail)
                        } else {
                            c.resume("failed")
                            return@sc
                        }
                    }
                }
                if (res != "failed") return
            } else {
                val curHead = head.get()
                if (curHead != tail.get() || curHead.next.get() != null) {
                    val headNext = curHead.next.get()
                    if (headNext is ConsumerThr<*> && head.compareAndSet(curHead, headNext)) {
                        @Suppress("UNCHECKED_CAST")
                        (headNext.action as Continuation<E>).resume(element)
                        return
                    }
                }
            }
        }
    }

    override suspend fun receive(): E {
        while (true) {
            val curTail = tail.get()
            if (curTail == head.get() || curTail is ConsumerThr<*>) {
                val res = suspendCoroutine<Any?> sc@{ c ->
                    val newTail = ConsumerThr(c)
                    val oldTail = tail.get()
                    if (oldTail != null) {
                        if ((oldTail == head.get() || oldTail is ConsumerThr<*>) && oldTail.next.compareAndSet(
                                null,
                                newTail
                            )
                        ) {
                            tail.compareAndSet(oldTail, newTail)
                        } else {
                            c.resume(null)
                            return@sc
                        }
                    }
                }
                @Suppress("UNCHECKED_CAST")
                if (res != null) return res as E
            } else {
                val curHead = head.get()
                if (curHead != tail.get() || curHead.next.get() != null) {
                    val headNext = curHead.next.get()
                    if (curHead != tail.get() && headNext is ProducerThr<*> && head.compareAndSet(curHead, headNext)) {
                        headNext.action.resume(Unit)
                        @Suppress("UNCHECKED_CAST")
                        return (headNext.element as E)
                    }
                }
            }
        }
    }
}
