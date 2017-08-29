package zlc.season.rxdownload3.core


class Segment(val index: Long, val start: Long, val end: Long) {
    companion object {
        val SEGMENT_SIZE = 24L //each Long is 8 bytes
    }

    fun isComplete(): Boolean {
        return (start - end) == 1L
    }
}