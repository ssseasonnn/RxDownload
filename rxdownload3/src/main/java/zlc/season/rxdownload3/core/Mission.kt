package zlc.season.rxdownload3.core

open class Mission(var url: String) {
    var saveName: String = ""
    var savePath: String = ""
    var rangeFlag: Boolean? = null
    var tag: String = url
    var overwrite: Boolean = false

    constructor(url: String, saveName: String, savePath: String, overwrite: Boolean = false) : this(url) {
        this.saveName = saveName
        this.savePath = savePath
        this.overwrite = overwrite
    }

    constructor(url: String, saveName: String, savePath: String, rangeFlag: Boolean?, tag: String, overwrite: Boolean = false) : this(url) {
        this.saveName = saveName
        this.savePath = savePath
        this.rangeFlag = rangeFlag
        this.tag = tag
        this.overwrite = overwrite
    }

    constructor(mission: Mission) : this(mission.url) {
        this.saveName = mission.saveName
        this.savePath = mission.savePath
        this.rangeFlag = mission.rangeFlag
        this.tag = mission.tag
        this.overwrite = mission.overwrite
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