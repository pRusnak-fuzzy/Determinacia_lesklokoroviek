package com.example.myapplication;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity implements HubaRecordAdapter.OnExportClickListener{

    private RecyclerView recyclerViewHistory;
    private HubaRecordAdapter adapter;
    private List<HubaRecord> hubaRecords; // Deklarácia List<HubaRecord> pre celé activity
    private HubaDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializácia RecyclerView
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));

        // Inicializácia databázového pomocníka
        dbHelper = new HubaDatabaseHelper(this);

        // Načítanie dát z databázy (inicializácia hubaRecords)
        hubaRecords = dbHelper.getAllHubaRecords(); // Toto je teraz členská premenná
        Log.d("HistoryActivity", "Loaded " + hubaRecords.size() + " records for display.");

        // Inicializácia adaptéra a nastavenie pre RecyclerView
        adapter = new HubaRecordAdapter(hubaRecords, this);
        recyclerViewHistory.setAdapter(adapter);

        // Nastavenie poslucháča pre mazanie z adaptéra
        adapter.setOnItemClickListener(new HubaRecordAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(int position) {
                // Získanie ID záznamu na zmazanie
                // Používame recordIdToDelete, pretože position sa môže zmeniť, ak medzitým zmažeš iný záznam
                long recordIdToDelete = hubaRecords.get(position).getId();

                // Potvrdenie mazania (odporúčané)
                showDeleteConfirmationDialog(position, recordIdToDelete);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Načítanie dát znova, ak sa aktivita vráti do popredia
        // To zabezpečí, že ak sa niečo uloží v MainActivity, objaví sa to aj tu.
        // A tiež, aby sa aktualizoval zoznam po zmazaní, ak sa nejakým spôsobom neaktualizoval správne
        loadHubaRecords(); // Znovu načítame záznamy
        if (adapter != null) {
            adapter.updateData(hubaRecords); // Nastavíme nové záznamy do adaptéra
            adapter.notifyDataSetChanged(); // Povie adaptéru, aby prekreslil celý zoznam
            Log.d("HistoryActivity", "Refreshed records onResume: " + hubaRecords.size());
        }
    }

    /**
     * Načíta záznamy huby z databázy a aktualizuje hubaRecords zoznam.
     */
    private void loadHubaRecords() {
        if (dbHelper != null) {
            hubaRecords = dbHelper.getAllHubaRecords();
        }
    }

    /**
     * Zobrazí potvrdzujúci dialóg pred zmazaním záznamu.
     * @param position Pozícia záznamu v zozname (pre aktualizáciu UI).
     * @param recordId ID záznamu v databáze.
     */
    private void showDeleteConfirmationDialog(final int position, final long recordId) {
        new AlertDialog.Builder(this)
                .setTitle("Zmazať záznam")
                .setMessage("Naozaj chcete zmazať tento záznam histórie?")
                .setPositiveButton("Áno", (dialog, which) -> {
                    // Používateľ potvrdil mazanie
                    deleteRecord(position, recordId);
                })
                .setNegativeButton("Nie", null) // Používateľ zrušil, nič nerobíme
                .show();
    }

    /**
     * Vykoná zmazanie záznamu z databázy a aktualizuje zoznam v RecyclerView.
     * @param position Pozícia záznamu v zozname (pre odstránenie z UI).
     * @param recordId ID záznamu v databáze.
     */
    private void deleteRecord(int position, long recordId) {
        int rowsAffected = dbHelper.deleteHubaRecord(recordId);

        if (rowsAffected > 0) {
            Toast.makeText(this, "Záznam úspešne zmazaný.", Toast.LENGTH_SHORT).show();
            // Odstránenie záznamu zo zoznamu dát adaptéra
            if (position < hubaRecords.size()) { // Ochrana proti IndexOutOfBoundsException
                hubaRecords.remove(position);
                adapter.notifyItemRemoved(position);
                // Je dôležité notifikovať adaptér, aby sa prepočítali pozície pre zvyšné prvky
                adapter.notifyItemRangeChanged(position, hubaRecords.size());
            } else {
                // Ak bola pozícia neplatná (napr. záznam bol už zmazaný iným spôsobom),
                // načítame celý zoznam znova.
                loadHubaRecords();
                adapter.updateData(hubaRecords);
                adapter.notifyDataSetChanged();
            }
        } else {
            Toast.makeText(this, "Chyba pri mazaní záznamu.", Toast.LENGTH_SHORT).show();
        }
    }

    // Implementácia metódy z rozhrania OnExportClickListener
    @Override
    public void onExportClick(HubaRecord record) {
        // Táto metóda sa zavolá, keď používateľ klikne na tlačidlo exportu pre konkrétny záznam
        exportRecordToCsv(record);
    }

    private void exportRecordToCsv(HubaRecord record) {
        // Definovanie názvu súboru pre export
        String fileName = "huba_zaznam_" + record.getId() + ".csv";
        File tempFile = new File(getExternalFilesDir(null), fileName);

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(tempFile))) {
            // Hlavička CSV (názvy stĺpcov)
            writer.println("Datum Urcenia,Latinsky Nazov,Slovensky Nazov,Hodnota Fuzzy," +
                    "Dlzka Spor,Sirka Spor,Hlubik,Povrch Klobuka,Zivicova Vrstva,Tvar Klobuka," +
                    "Farba Klobuka,Kresba Klobuka,Farba Hlubika,Farba Duziny,Farba Ruriek," +
                    "Tvar Okraja,Lahka Plodnica");

            // Riadok s dátami pre konkrétny záznam
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String formattedDate = (record.getDatumUrcenia() != null) ? sdf.format(record.getDatumUrcenia()) : "";

            writer.printf(Locale.US, "\"%s\",\"%s\",\"%s\",%.3f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                    formattedDate,
                    record.getLatinskyNazov().replace("\"", "\"\""), // Escapovanie úvodzoviek
                    record.getSlovenskyNazov().replace("\"", "\"\""),
                    record.getHodnotaFuzzy(),
                    record.getDlzkaSpor(),
                    record.getSirkaSpor(),
                    record.getHlubik(),
                    record.getPovrchKlobuka(),
                    record.getZivicovaVrstva(),
                    record.getTvarKlobuka(),
                    record.getFarbaKlobuka(),
                    record.getKresbaKlobuka(),
                    record.getFarbaHlubika(),
                    record.getFarbaDuziny(),
                    record.getFarbaRuriek(),
                    record.getTvarOkraja(),
                    record.getLahkaPlodnica()
            );

            writer.flush();

            Log.d("HistoryActivity", "Temporary CSV created at: " + tempFile.getAbsolutePath());

            // Ponúknuť zdieľanie súboru
            saveCsvToDownloads(tempFile, fileName);

        } catch (IOException e) {
            Log.e("HistoryActivity", "Chyba pri exporte do CSV: " + e.getMessage(), e);
            Toast.makeText(this, "Chyba pri exporte záznamu: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Ak hodnota obsahuje čiarku, úvodzovky alebo nový riadok, uzavrieme ju do úvodzoviek a zdvojíme vnútorné úvodzovky
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Uloží CSV súbor do verejného priečinka Downloads.
     * Používa MediaStore pre Android 10+ a priamy File API pre staršie verzie.
     * @param sourceFile Dočasný CSV súbor, ktorý sa má presunúť/skopírovať.
     * @param fileName Názov súboru, pod ktorým sa má uložiť.
     */
    private void saveCsvToDownloads(File sourceFile, String fileName) {
        OutputStream os = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 (API 29) a vyššie
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (uri != null) {
                    os = getContentResolver().openOutputStream(uri);
                } else {
                    throw new IOException("Failed to create new MediaStore file.");
                }
            } else { // Android 9 (API 28) a nižšie
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs(); // Vytvorí priečinok, ak neexistuje
                }
                File destFile = new File(downloadsDir, fileName);
                os = new FileOutputStream(destFile);
            }

            if (os != null) {
                // Skopírujeme obsah zo zdrojového súboru do cieľového streamu
                byte[] buffer = new byte[1024];
                int length;
                try (java.io.FileInputStream fis = new java.io.FileInputStream(sourceFile)) {
                    while ((length = fis.read(buffer)) > 0) {
                        os.write(buffer, 0, length);
                    }
                }
                Toast.makeText(this, "CSV súbor uložený do priečinka Downloads: " + fileName, Toast.LENGTH_LONG).show();
                Log.d("HistoryActivity", "CSV saved to Downloads: " + fileName);
            } else {
                Toast.makeText(this, "Chyba: Nepodarilo sa otvoriť výstupný stream pre súbor.", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Log.e("HistoryActivity", "Chyba pri ukladaní do Downloads: " + e.getMessage(), e);
            Toast.makeText(this, "Chyba pri ukladaní súboru: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e("HistoryActivity", "Error closing output stream: " + e.getMessage());
                }
            }
            // Zmažeme dočasný súbor po dokončení kopírovania
            if (sourceFile.exists()) {
                sourceFile.delete();
            }
        }
    }
}