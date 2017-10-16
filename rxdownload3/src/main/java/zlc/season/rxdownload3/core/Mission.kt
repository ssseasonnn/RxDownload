package zlc.season.rxdownload3.core

open class Mission(var url: String) {
    var saveName: String = ""
    var savePath: String = ""
    var rangeFlag: Boolean? = null
    var tag: String = url

    constructor(url: String, saveName: String, savePath: String) : this(url) {
        this.saveName = saveName
        this.savePath = savePath
    }

    constructor(url: String, saveName: String, savePath: String, rangeFlag: Boolean?, tag: String) : this(url) {
        this.saveName = saveName
        this.savePath = savePath
        this.rangeFlag = rangeFlag
        this.tag = tag
    }

    constructor(mission: Mission) : this(mission.url) {
        this.saveName = mission.saveName
        this.savePath = mission.savePath
        this.rangeFlag = mission.rangeFlag
        this.tag = mission.tag
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        other as Mission

        if (tag != other.tag) return false

        return true
    }

    override fun hashCode(): Int {
        return tag.hashCode()
    }
}