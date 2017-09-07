package zlc.season.rxdownload3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.core.RangeTmpFile

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    @Test
    @Throws(Exception::class)
    fun addition_isCorrect() {
        assertEquals(4, (2 + 2).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun filterTest() {
        val testList = mutableListOf<RangeTmpFile.Segment>()
        val s1 = RangeTmpFile.Segment(0, 0, 1, 0)
        val s2 = RangeTmpFile.Segment(1, 0, 0, 10)
        testList.add(s1)
        testList.add(s2)

        testList.filter { !it.isComplete() }
                .forEach { println("${it.start}-${it.current}-${it.end}") }
    }

    @Test
    @Throws(Exception::class)
    fun missionEqualTest() {
        val mission1 = Mission("test1")
        val mission2 = Mission("test2")
        val mission3 = Mission("test3")

        val set = mutableSetOf<Mission>()
        set.add(mission1)
        set.add(mission2)
        set.add(mission3)

        val testMission = Mission("test1")
        val result = set.find { it == testMission }

        assertTrue(result != null)
    }
}