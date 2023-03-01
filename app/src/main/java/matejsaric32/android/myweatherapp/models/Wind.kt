package matejsaric32.android.myweatherapp.models

import android.os.Parcel
import android.os.Parcelable

data class Wind(
    val speed: Double,
    val deg: Int,
    val gust: Double
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readInt(),
        parcel.readDouble()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(speed)
        parcel.writeInt(deg)
        parcel.writeDouble(gust)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Wind> {
        override fun createFromParcel(parcel: Parcel): Wind {
            return Wind(parcel)
        }

        override fun newArray(size: Int): Array<Wind?> {
            return arrayOfNulls(size)
        }
    }
}