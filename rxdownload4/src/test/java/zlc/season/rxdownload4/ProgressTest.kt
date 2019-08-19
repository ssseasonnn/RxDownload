package zlc.season.rxdownload4

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.assertThrows

@TestInstance(PER_CLASS)
class ProgressTest {

    @Test
    fun `percent should throw exception when chunked is true`() {
        val progress = Progress(10, 100, true)
        assertThrows<IllegalStateException> {
            progress.percent()
        }
    }

    @Test
    fun `get total size should throw exception when chunked is true`() {
        val progress = Progress(0, 0, true)
        assertThrows<IllegalStateException> {
            progress.totalSize
        }
        assertThrows<IllegalStateException> {
            progress.totalSizeStr()
        }
    }

    @Test
    fun percentTest() {
        val progress = Progress(10, 100)
        val percent = progress.percent()
        assertThat(percent).isEqualTo(10)
    }
}