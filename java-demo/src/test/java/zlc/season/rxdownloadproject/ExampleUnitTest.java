package zlc.season.rxdownloadproject;

import org.junit.Test;

import zlc.season.rxdownload3.core.Mission;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testMissionConstructor() throws Exception {
        Mission mission = new Mission("url", "saveName", "savePath", false);
        System.out.println(mission.getTag());

        Mission mission1 = new Mission("url", "saveName", "savePath", null, "tag", false);
        System.out.println(mission1.getTag());

        Mission mission2 = new Mission("url");
        System.out.println(mission2.getTag());
    }
}