package zlc.season.rxdownload.kotlin_demo

import zlc.season.rxdownload3.core.Mission


class CustomMission : Mission {
    var img: String = ""
    var introduce: String = ""

    constructor(url: String, introduce: String, img: String) : super(url) {
        this.introduce = introduce
        this.img = img
    }

    constructor(mission: Mission, img: String) : super(mission) {
        this.img = img
    }
}

