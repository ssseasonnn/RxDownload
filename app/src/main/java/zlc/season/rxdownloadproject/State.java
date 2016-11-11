package zlc.season.rxdownloadproject;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/11
 * Time: 13:53
 * FIXME
 */
public enum State {
    START(0), PAUSE(1), DONE(2);

    private int nCode;

    State(int _nCode) {
        this.nCode = _nCode;
    }

    public int getValue() {
        return this.nCode;
    }
}
