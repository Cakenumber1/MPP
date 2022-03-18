import kotlinx.atomicfu.*

class FAAQueue<T> {
    private val head: AtomicRef<Segment> // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private val tail: AtomicRef<Segment> // Tail pointer, similarly to the Michael-Scott queue

    init {
        val firstNode = Segment()
        head = atomic(firstNode)
        tail = atomic(firstNode)
    }

    /**
     * Adds the specified element [x] to the queue.
     */
    fun enqueue(x: T) {
        if (x == null) throw NullPointerException();
        while (true) {
            val tailTemp: Segment = this.tail.value;
            val index: Int = tailTemp.enqIdx.getAndIncrement();
            if (index > SEGMENT_SIZE - 1) {
                val nextTemp = tailTemp.next;
                if (nextTemp.value == null) {
                    val newSegment = Segment(x);
                    if (nextTemp.compareAndSet(null, newSegment)) {
                        this.tail.compareAndSet(tailTemp, newSegment);
                        return;
                    }
                } else {
                    this.tail.compareAndSet(tailTemp, nextTemp.value!!);
                }
                continue;
            }
            if (tailTemp.elements[index].compareAndSet(null, x))
                return
        }
    }

    /**
     * Retrieves the first element from the queue
     * and returns it; returns `null` if the queue
     * is empty.
     */
    fun dequeue(): T? {
        while (true) {
            val headTemp = this.head.value
            if (headTemp.deqIdx.value >= headTemp.enqIdx.value && headTemp.next.value == null)
                return null;
            val index = headTemp.deqIdx.getAndIncrement();
            if (index > SEGMENT_SIZE - 1) {
                if (headTemp.next.value == null)
                    return null;
                this.head.compareAndSet(headTemp, headTemp.next.value!!);
                continue;
            }
            val res = headTemp.elements[index].getAndSet(DONE) ?: continue
            @Suppress("UNCHECKED_CAST")
            return res as T?
        }
    }

    /**
     * Returns `true` if this queue is empty;
     * `false` otherwise.
     */
    val isEmpty: Boolean
        get() {
            while (true) {
                val tempHead : Segment = head.value
                if (tempHead.isEmpty) {
                    if (tempHead.next.value == null) return true
                    this.head.compareAndSet(tempHead, tempHead.next.value!!);
                    continue;
                } else {
                    return false
                }
            }
        }
}

private class Segment {
    val next: AtomicRef<Segment?> = atomic(null)
    val enqIdx = atomic(0) // index for the next enqueue operation
    val deqIdx = atomic(0) // index for the next dequeue operation
    val elements = atomicArrayOfNulls<Any>(SEGMENT_SIZE)

    constructor() // for the first segment creation

    constructor(x: Any?) { // each next new segment should be constructed with an element
        enqIdx.value = 1
        elements[0].getAndSet(x)
    }

    val isEmpty: Boolean get() = deqIdx.value >= enqIdx.value || deqIdx.value >= SEGMENT_SIZE
}

private val DONE = Any() // Marker for the "DONE" slot state; to avoid memory leaks
const val SEGMENT_SIZE = 2 // DO NOT CHANGE, IMPORTANT FOR TESTS

