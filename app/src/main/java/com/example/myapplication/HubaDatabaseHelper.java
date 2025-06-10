package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class HubaDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "huba_db";
    private static final int DATABASE_VERSION = 3;

    // Názov tabuľky
    public static final String TABLE_HUBA_RECORDS = "huba_records";

    // Názvy stĺpcov
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DATUM_URCENIA = "datum_urcenia";
    public static final String COLUMN_LATINSKY_NAZOV = "latinsky_nazov";
    public static final String COLUMN_SLOVENSKY_NAZOV = "slovensky_nazov";
    public static final String COLUMN_HODNOTA_FUZZY = "hodnota_fuzzy";

    // Vstupné hodnoty z fuzzy systému
    public static final String COLUMN_DLZKA_SPOR = "dlzka_spor";
    public static final String COLUMN_SIRKA_SPOR = "sirka_spor";
    public static final String COLUMN_HLUBIK = "hlubik";
    public static final String COLUMN_POVRCH_KLOBUKA = "povrch_klobuka";
    public static final String COLUMN_ZIVICOVA_VRSTVA = "zivicova_vrstva";
    public static final String COLUMN_TVAR_KLOBUKA = "tvar_klobuka";
    public static final String COLUMN_FARBA_KLOBUKA = "farba_klobuka";
    public static final String COLUMN_KRESBA_KLOBUKA = "kresba_klobuka";
    public static final String COLUMN_FARBA_HLUBIKA = "farba_hlubika";
    public static final String COLUMN_FARBA_DUZINY = "farba_duziny";
    public static final String COLUMN_FARBA_RURIEK = "farba_ruriek";
    public static final String COLUMN_TVAR_OKRAJA = "tvar_okraja";
    public static final String COLUMN_LAHKA_PLODNICA = "lahka_plodnica";
    public static final String COLUMN_FOTO_PATH = "foto_path";


    // SQL dotaz na vytvorenie tabuľky
    private static final String CREATE_TABLE_HUBA_RECORDS = "CREATE TABLE " + TABLE_HUBA_RECORDS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_DATUM_URCENIA + " INTEGER,"
            + COLUMN_LATINSKY_NAZOV + " TEXT,"
            + COLUMN_SLOVENSKY_NAZOV + " TEXT,"
            + COLUMN_HODNOTA_FUZZY + " REAL,"
            + COLUMN_DLZKA_SPOR + " REAL,"
            + COLUMN_SIRKA_SPOR + " REAL,"
            + COLUMN_HLUBIK + " REAL,"
            + COLUMN_POVRCH_KLOBUKA + " REAL,"
            + COLUMN_ZIVICOVA_VRSTVA + " REAL,"
            + COLUMN_TVAR_KLOBUKA + " REAL,"
            + COLUMN_FARBA_KLOBUKA + " REAL,"
            + COLUMN_KRESBA_KLOBUKA + " REAL,"
            + COLUMN_FARBA_HLUBIKA + " REAL,"
            + COLUMN_FARBA_DUZINY + " REAL,"
            + COLUMN_FARBA_RURIEK + " REAL,"
            + COLUMN_TVAR_OKRAJA + " REAL,"
            + COLUMN_LAHKA_PLODNICA + " REAL,"
            + COLUMN_FOTO_PATH + " TEXT"
            + ")";

    public HubaDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_HUBA_RECORDS);
        Log.d("HubaDatabaseHelper", "Database table created: " + TABLE_HUBA_RECORDS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {}

    /**
     * Vloží nový záznam o určenej hube do databázy.
     * @param hubaData Mapa obsahujúca všetky stĺpce a ich hodnoty.
     * @return ID nového riadku, alebo -1 ak došlo k chybe.
     */
    public long insertHubaRecord(Map<String, Double> hubaData, String latinName, String commonName, double fuzzyValue, String fotoPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        long currentTimeMillis = new Date().getTime();
        values.put(COLUMN_DATUM_URCENIA, currentTimeMillis);

        values.put(COLUMN_LATINSKY_NAZOV, latinName);
        values.put(COLUMN_SLOVENSKY_NAZOV, commonName);
        values.put(COLUMN_HODNOTA_FUZZY, fuzzyValue);

        // Prejdi všetky vstupné dáta z fuzzy systému
        values.put(COLUMN_DLZKA_SPOR, hubaData.get(COLUMN_DLZKA_SPOR));
        values.put(COLUMN_SIRKA_SPOR, hubaData.get(COLUMN_SIRKA_SPOR));
        values.put(COLUMN_HLUBIK, hubaData.get(COLUMN_HLUBIK));
        values.put(COLUMN_POVRCH_KLOBUKA, hubaData.get(COLUMN_POVRCH_KLOBUKA));
        values.put(COLUMN_ZIVICOVA_VRSTVA, hubaData.get(COLUMN_ZIVICOVA_VRSTVA));
        values.put(COLUMN_TVAR_KLOBUKA, hubaData.get(COLUMN_TVAR_KLOBUKA));
        values.put(COLUMN_FARBA_KLOBUKA, hubaData.get(COLUMN_FARBA_KLOBUKA));
        values.put(COLUMN_KRESBA_KLOBUKA, hubaData.get(COLUMN_KRESBA_KLOBUKA));
        values.put(COLUMN_FARBA_HLUBIKA, hubaData.get(COLUMN_FARBA_HLUBIKA));
        values.put(COLUMN_FARBA_DUZINY, hubaData.get(COLUMN_FARBA_DUZINY));
        values.put(COLUMN_FARBA_RURIEK, hubaData.get(COLUMN_FARBA_RURIEK));
        values.put(COLUMN_TVAR_OKRAJA, hubaData.get(COLUMN_TVAR_OKRAJA));
        values.put(COLUMN_LAHKA_PLODNICA, hubaData.get(COLUMN_LAHKA_PLODNICA));
        values.put(COLUMN_FOTO_PATH, fotoPath);

        long newRowId = db.insert(TABLE_HUBA_RECORDS, null, values);
        db.close();
        Log.d("HubaDatabaseHelper", "Inserted new record with ID: " + newRowId);
        return newRowId;
    }

    /**
     * Získa všetky záznamy z databázy.
     * @return Zoznam objektov HubaRecord.
     */
    public List<HubaRecord> getAllHubaRecords() {
        List<HubaRecord> hubaRecords = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_HUBA_RECORDS,
                null,
                null,
                null,
                null,
                null,
                COLUMN_ID + " DESC" // Zotriediť od najnovších
        );

        if (cursor.moveToFirst()) {
            do {
                HubaRecord record = new HubaRecord();
                record.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));

                // Načítame dátum ako long (milisekundy) a konvertujeme na Date
                long timestampMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATUM_URCENIA));
                if (timestampMillis > 0) { // Overenie platnosti, 0 je často default pre nenastavené long
                    record.setDatumUrcenia(new Date(timestampMillis));
                } else {
                    record.setDatumUrcenia(null); // Ak je 0 alebo neplatné, nastavíme na null
                }

                record.setLatinskyNazov(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LATINSKY_NAZOV)));
                record.setSlovenskyNazov(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SLOVENSKY_NAZOV)));
                record.setHodnotaFuzzy(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HODNOTA_FUZZY)));
                record.setDlzkaSpor(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DLZKA_SPOR)));
                record.setSirkaSpor(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SIRKA_SPOR)));
                record.setHlubik(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HLUBIK)));
                record.setPovrchKlobuka(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_POVRCH_KLOBUKA)));
                record.setZivicovaVrstva(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ZIVICOVA_VRSTVA)));
                record.setTvarKlobuka(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TVAR_KLOBUKA)));
                record.setFarbaKlobuka(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_FARBA_KLOBUKA)));
                record.setKresbaKlobuka(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_KRESBA_KLOBUKA)));
                record.setFarbaHlubika(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_FARBA_HLUBIKA)));
                record.setFarbaDuziny(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_FARBA_DUZINY)));
                record.setFarbaRuriek(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_FARBA_RURIEK)));
                record.setTvarOkraja(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TVAR_OKRAJA)));
                record.setLahkaPlodnica(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LAHKA_PLODNICA)));

                try {
                    record.setFotoPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOTO_PATH)));
                } catch (IllegalArgumentException e) {
                    Log.e("HubaDatabaseHelper", "Column " + COLUMN_FOTO_PATH + " not found. Database might be outdated.");
                    record.setFotoPath(null); // Nastav na null, ak stĺpec neexistuje
                }

                hubaRecords.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        Log.d("HubaDatabaseHelper", "Loaded " + hubaRecords.size() + " records from database.");
        return hubaRecords;
    }

    /**
     * Zmaže záznam huby z databázy na základe jeho ID.
     * @param recordId ID záznamu, ktorý sa má zmazať.
     * @return Počet zmazaných riadkov (1 pre úspech, 0 ak sa záznam nenašiel).
     */
    public int deleteHubaRecord(long recordId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_HUBA_RECORDS,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(recordId)});
        db.close();
        return rowsAffected;
    }
}