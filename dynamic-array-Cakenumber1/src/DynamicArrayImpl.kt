import kotlinx.atomicfu.*

private abstract class Node<E>(val value: E)

private class Core<E>(
    val capacity: Int
) {
    val array: AtomicArray<Node<E>?> = atomicArrayOfNulls(capacity)
    val size: AtomicInt = atomic(0)
    val isMoving: AtomicBoolean = atomic(false)
    //fun handleMoving(current : Boolean) = isMoving.compareAndSet(current, !current);
    fun setIsMoving() = isMoving.compareAndSet(expect = false, update = true)
    fun setIsNotMoving() = isMoving.getAndSet(false)
}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<E>(INITIAL_CAPACITY))
    override val size: Int get() = core.value.size.value
    private class V<E>(value: E) : Node<E>(value) // Do need to move
    private class S<E>(value: E) : Node<E>(value) // Do not need to move

    override fun get(index: Int): E {
        require(index < size)
        return core.value.array[index].value!!.value
    }

    override fun put(index: Int, element: E) {
        require(index < size)
        while (true) {
            val core = core.value
            val newNode = S(element)
            val currentNode = core.array[index].value
            if (core.array[index].value is V<*>)
                continue
            if (core.array[index].compareAndSet(currentNode, newNode))
                return
        }
    }

    override fun pushBack(element: E) {
        while (true) {
            val core = core.value
            val newNode = S(element)
            val size = core.size.value
            if (core.capacity > size)
                if (core.array[size].compareAndSet(null, newNode))
                    if (core.size.compareAndSet(size, size + 1))
                        return
            moveCore(core, size)
        }
    }

    private fun moveCore(core: Core<E>, size: Int) {
        if (core.capacity <= size && core.setIsMoving()) {
            val newCore = Core<E>(2 * core.capacity)
            newCore.size.getAndSet(size)
            var i = 0
            while(i < size) {
                val node = core.array[i].value
                if (core.array[i].compareAndSet(node, V(node!!.value))) {
                    newCore.array[i].getAndSet(node)
                    i++
                }
            }
            this.core.compareAndSet(core, newCore)
            core.setIsNotMoving()
        }
    }
}
