package zlc.season.rxdownload4

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.assertThrows
import java.io.File

@TestInstance(PER_CLASS)
class StatusTest {
    private val status = Status(0, 0, file = File(""))

    @Test
    fun `percent should throw exception when chunked is true`() {
        val status = Status(10, 100, true, File(""))
        assertThrows<IllegalStateException> {
            status.percent()
        }
    }

    @Test
    fun `get total size should throw exception when chunked is true`() {
        val status = Status(0, 0, true, File(""))
        assertThrows<IllegalStateException> {
            status.totalSize
        }
        assertThrows<IllegalStateException> {
            status.totalSizeStr()
        }
    }

    @Test
    fun percentTest() {
        val status = Status(10, 100, file = File(""))
        val percent = status.percent()
        assertThat(percent).isEqualTo(10)
    }
}