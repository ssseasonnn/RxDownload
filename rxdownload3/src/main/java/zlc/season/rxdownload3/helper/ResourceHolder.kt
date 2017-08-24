package zlc.season.rxdownload3.helper

import java.io.Closeable


class ResourceHolder : Closeable {
    val resources = arrayListOf<Closeable>()

    fun <T : Closeable> T.autoClose(): T {
        resources.add(this)
        return this
    }

    override fun close() {
        resources.reversed().forEach { it.close() }
    }
}

fun <R> using(block: ResourceHolder.() -> R): R {
    val holder = ResourceHolder()
    holder.use {
        return it.block()
    }
}