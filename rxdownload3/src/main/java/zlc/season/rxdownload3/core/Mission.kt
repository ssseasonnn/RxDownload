package zlc.season.rxdownload3.core

import android.os.Parcel
import android.os.Parcelable


class Mission(val url: String,
              var saveName: String = "",
              var savePath: String = "",
              var rangeFlag: Boolean? = null,
              var tag: String = url) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mission

        if (tag != other.tag) return false

        return true
    }

    override fun hashCode(): Int {
        return tag.hashCode()
    }

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        parcel.writeString(saveName)
        parcel.writeString(savePath)
        parcel.writeValue(rangeFlag)
        parcel.writeString(tag)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Mission> {
        override fun createFromParcel(parcel: Parcel): Mission {
            return Mission(parcel)
        }

        override fun newArray(size: Int): Array<Mission?> {
            return arrayOfNulls(size)
        }
    }
}