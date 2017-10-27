package zlc.season.rxdownload.java_demo;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import org.jetbrains.annotations.NotNull;

import zlc.season.rxdownload3.core.Mission;
import zlc.season.rxdownload3.core.RealMission;
import zlc.season.rxdownload3.database.SQLiteActor;

public class CustomSqliteActor extends SQLiteActor {
    private String IMG = "img";

    public CustomSqliteActor(@NotNull Context context) {
        super(context);
    }

    @NotNull
    @Override
    public String provideCreateSql() {
        String formatStr =
                "CREATE TABLE %s (\n" +
                        "%s TEXT PRIMARY KEY NOT NULL,\n" +
                        "%s TEXT NOT NULL,\n" +
                        "%s TEXT,\n" +
                        "%s TEXT,\n" +
                        "%s INTEGER,\n" +
                        "%s TEXT,\n" +
                        "%s TEXT,\n" +
                        "%s INTEGER,\n" +
                        "%s TEXT)";

        return String.format(formatStr, getTABLE_NAME(),
                getTAG(),
                getURL(),
                getSAVE_NAME(),
                getSAVE_PATH(),
                getRANGE_FLAG(),
                getCURRENT_SIZE(),
                getTOTAL_SIZE(),
                getSTATUS_FLAG(),
                IMG);
    }

    @NotNull
    @Override
    public ContentValues onCreate(@NotNull RealMission mission) {
        ContentValues cv = super.onCreate(mission);
        if (mission.getActual() instanceof CustomMission) {
            CustomMission customMission = (CustomMission) mission.getActual();
            cv.put(IMG, customMission.getImg());
        }
        return cv;
    }

    @NotNull
    @Override
    public Mission onGetAllMission(@NotNull Cursor cursor) {
        Mission mission = super.onGetAllMission(cursor);
        String img = cursor.getString(cursor.getColumnIndexOrThrow(IMG));
        if (img == null || img.isEmpty()) {
            img = "no img";
        }
        return new CustomMission(mission, img);
    }
}
