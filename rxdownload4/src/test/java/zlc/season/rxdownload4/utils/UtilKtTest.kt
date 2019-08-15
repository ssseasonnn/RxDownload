package zlc.season.rxdownload4.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.assertThrows

@TestInstance(PER_CLASS)
class UtilKtTest {

    @Nested
    inner class FormatSizeTest {
        @Test
        fun `format -1 should throw Exception`() {
            assertThrows<IllegalArgumentException> {
                (-1L).formatSize()
            }
        }

        @Test
        fun `format 0 should 0_0 B`() {
            val formatSize = 0L.formatSize()
            assertThat(formatSize).isEqualTo("0.0 B")
        }

        @Test
        fun `format 1KB should be 1_0 KB`() {
            assertThat(1024L.formatSize()).isEqualTo("1.0 KB")
        }

        @Test
        fun `format 1M should be 1_0 MB`() {
            1_048_576L.formatSize()
                    .let {
                        assertThat(it).isEqualTo("1.0 MB")
                    }
        }

        @Test
        fun `format 1GB should be 1_0 GB`() {
            1_073_741_824L.formatSize()
                    .let {
                        assertThat(it).isEqualTo("1.0 GB")
                    }
        }

        @Test
        fun `format 1TB should be 1_0 TB`() {
            1_099_511_627_776L.formatSize()
                    .let {
                        assertThat(it).isEqualTo("1.0 TB")
                    }
        }
    }

    @Nested
    inner class DecimalTest {

        @Test
        fun `0_00 with 2 digits should be 0_0`() {
            val result = 0.00.decimal(2)
            assertThat(result).isEqualTo(0.0)
        }

        @Test
        fun `0_00001 with 2 digits should be 0_0`() {
            val result = 0.00001.decimal(2)
            assertThat(result).isEqualTo(0.0)
        }

        @Test
        fun `0_123 with 2 digits should be 0_12`() {
            0.123.decimal(2)
                    .let {
                        assertThat(it).isEqualTo(0.12)
                    }
        }
    }
}