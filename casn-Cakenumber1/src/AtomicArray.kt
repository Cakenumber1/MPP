import kotlinx.atomicfu.*

open class Descriptor<E> {
    open fun complete() {}

    fun applyRDCSS(
        ref1: Ref<E>, expected1: E, update1: Any?,
        caSNDescriptor: CASNDescriptor<E>
    ): Boolean {
        val descriptor = RDCSSDescriptor(ref1, expected1, update1, caSNDescriptor)
        if (ref1.ref.value != null) {
            if ((ref1.ref.value!! == update1) || (ref1.cas(expected1, descriptor))) {
                descriptor.complete()
                return descriptor.status.value == "SUCCEEDED"
            }
            return false
        } else {
            return false
        }
    }
}

class CASNDescriptor<E>(
    private val ref1: Ref<E>, private val expected1: E, private val update1: E,
    private val ref2: Ref<E>, private val expected2: E, private val update2: E
) : Descriptor<E>() {
    val status: AtomicRef<String> = atomic("UNDECIDED")

    override fun complete() {
        if (applyRDCSS(ref2, expected2, this, this)) {
            this.status.compareAndSet("UNDECIDED", "SUCCEEDED")
        } else {
            val outcome = if (ref2.ref.value != this) "FAILED" else "SUCCEEDED"
            this.status.compareAndSet("UNDECIDED", outcome)
        }

        if (this.status.value == "FAILED") {
            ref1.ref.compareAndSet(this, expected1)
            ref2.ref.compareAndSet(this, expected2)
        } else {
            ref1.ref.compareAndSet(this, update1)
            ref2.ref.compareAndSet(this, update2)
        }
    }
}

class RDCSSDescriptor<E>(
    private val ref1: Ref<E>, private val expected1: E, private val update1: Any?,
    private val caSNDescriptor: CASNDescriptor<E>
) : Descriptor<E>() {
    val status: AtomicRef<String> = atomic("UNDECIDED")

    override fun complete() {
        val outcome = if (caSNDescriptor.status.value != "UNDECIDED") {
            "FAILED"
        } else "SUCCEEDED"
        status.compareAndSet("UNDECIDED", outcome)
        val update = if (status.value == "SUCCEEDED") {
            update1
        } else expected1
        ref1.ref.compareAndSet(this, update)
    }
}

class Ref<E>(initialValue: E) {
    val ref = atomic<Any?>(initialValue)

    @Suppress("UNCHECKED_CAST")
    var value: E
        get() {
            while (true) {
                val cur = ref.value
                if (cur is Descriptor<*>) {
                    cur.complete()
                } else return cur as E
            }
        }
        set(value) {
            while (true) {
                val cur = ref.value
                if (cur is Descriptor<*>) {
                    cur.complete()
                } else if (ref.compareAndSet(cur, value)) return
            }
        }

    fun cas(expected: Any?, update: Any?): Boolean {
        while (true) {
            val cur = ref.value
            if (cur is Descriptor<*>) {
                cur.complete()
            } else if (cur == expected) {
                val res = ref.compareAndSet(cur, update)
                if (res) {
                    return true
                }
            } else return false
        }
    }
}


class AtomicArray<E>(size: Int, initialValue: E) {
    private val a = arrayOfNulls<Ref<E>>(size)

    init {
        for (i in 0 until size) a[i] = Ref(initialValue)
    }

    fun get(index: Int): E? = a[index]?.value

    fun set(index: Int, value: E) {
        a[index]?.value = value
    }

    fun cas(index: Int, expected: E, update: E): Boolean{
        return if(a[index] == null) false
        else a[index]?.cas(expected, update) as Boolean
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {


        return if (index1 == index2) {
            if (expected1 == expected2) cas(index1, expected1, update2) else false
        } else {
            val index: Int
            val expected: E
            val descriptor: CASNDescriptor<E>
            if (index1 > index2) {
                index = index2
                expected = expected2
                descriptor = CASNDescriptor(a[index2]!!, expected2, update2, a[index1]!!, expected1, update1)
            } else {
                index = index1
                expected = expected1
                descriptor = CASNDescriptor(a[index1]!!, expected1, update1, a[index2]!!, expected2, update2)
            }
            @Suppress("UNCHECKED_CAST")
            if (a[index]!!.cas(expected, descriptor)) {
                descriptor.complete()
                descriptor.status.value == "SUCCEEDED"
            } else {
                false
            }
        }
    }
}
