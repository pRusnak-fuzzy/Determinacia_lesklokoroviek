package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import android.text.Html;

import com.fuzzylite.Engine;
import com.fuzzylite.imex.FllImporter;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FuzzyLiteApp";

    // Deklarácie pre všetky vstupné UI prvky
    private EditText input1_dlzka_S;
    private EditText input2_sirka_S;
    private RadioGroup radioGroup3_hlubik;
    private RadioGroup radioGroup4_povrh_KL;
    private RadioGroup radioGroup5_zivic_Vr;
    private RadioGroup radioGroup6_tvar_KL;
    private RadioGroup radioGroup7_farba_KL;
    private Spinner spinner8_kresba_KL;
    private Spinner spinner9_farba_HL;
    private Spinner spinner10_farba_DU;
    private Spinner spinner11_farba_RU;
    private RadioGroup radioGroup12_okraj_PL;
    private RadioGroup radioGroup13_lahka_PL;

    private TextView textViewResult;
    private Button buttonCalculate;
    private Button buttonSaveResult;
    private Button buttonHistory;

    private Engine engine; // FuzzyLite Engine
    private HubaDatabaseHelper dbHelper;

    // Mapovanie číselných výstupov na textové popisy
    private final Map<Double, String> hubaOutputMap = new HashMap<>();

    // Premenné na dočasné uloženie aktuálnych dát pre uloženie do DB
    private String currentLatinName;
    private String currentCommonName;
    private double currentFuzzyValue;
    private String currentPhotoPath = null;
    private Map<String, Double> currentInputValues; // Mapa pre uloženie všetkých vstupných hodnôt
    private ImageView imageViewSelectedPhoto; // NOVÉ
    private Button buttonAddPhoto;         // NOVÉ

    // Pre obsluhu výsledku z Intentu (galérie/kamery)
    // Toto je moderný spôsob, nahradzuje staršie onActivityResult
    private ActivityResultLauncher<Intent> pickImageLauncher; // Pre galériu
    private ActivityResultLauncher<Uri> takePhotoLauncher;   // Pre kameru (vyžaduje FileProvider)
    private ActivityResultLauncher<String[]> requestPermissionLauncher; // Pre povolenia

    // URI pre dočasný súbor, ak používame kameru
    private Uri photoUri; // NOVÉ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializácia UI elementov
        input1_dlzka_S = findViewById(R.id.input_1_dlzka_S);
        input2_sirka_S = findViewById(R.id.input_2_sirka_S);
        radioGroup3_hlubik = findViewById(R.id.radioGroup_3_hlubik);
        radioGroup4_povrh_KL = findViewById(R.id.radioGroup_4_povrh_KL);
        radioGroup5_zivic_Vr = findViewById(R.id.radioGroup_5_zivic_Vr);
        radioGroup6_tvar_KL = findViewById(R.id.radioGroup_6_tvar_KL);
        radioGroup7_farba_KL = findViewById(R.id.radioGroup_7_farba_KL);
        spinner8_kresba_KL = findViewById(R.id.spinner_8_kresba_KL);
        spinner9_farba_HL = findViewById(R.id.spinner9_farba_HL);
        spinner10_farba_DU = findViewById(R.id.spinner_10_farba_DU);
        spinner11_farba_RU = findViewById(R.id.spinner_11_farba_RU);
        radioGroup12_okraj_PL = findViewById(R.id.radioGroup_12_okraj_PL);
        radioGroup13_lahka_PL = findViewById(R.id.radioGroup_13_lahka_PL);

        textViewResult = findViewById(R.id.textViewResult);
        buttonCalculate = findViewById(R.id.buttonCalculate);
        buttonSaveResult = findViewById(R.id.buttonSaveResult);
        buttonHistory = findViewById(R.id.buttonHistory);

        dbHelper = new HubaDatabaseHelper(this);

        imageViewSelectedPhoto = findViewById(R.id.imageViewSelectedPhoto); // NOVÉ
        buttonAddPhoto = findViewById(R.id.buttonAddPhoto);

        imageViewSelectedPhoto.setVisibility(View.GONE);

        // Inicializácia ActivityResultLauncher pre galériu
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            currentPhotoPath = saveImageToInternalStorage(selectedImageUri);
                            if (currentPhotoPath != null) {
                                imageViewSelectedPhoto.setImageURI(Uri.parse(currentPhotoPath)); // Načítaj z internej cesty
                                imageViewSelectedPhoto.setVisibility(View.VISIBLE);
                                Log.d(TAG, "Selected image URI: " + selectedImageUri.toString());
                                Log.d(TAG, "Saved to internal storage path: " + currentPhotoPath);
                            } else {
                                Toast.makeText(MainActivity.this, "Chyba pri ukladaní obrázka.", Toast.LENGTH_SHORT).show();
                                imageViewSelectedPhoto.setVisibility(View.GONE); // Skryť, ak sa nepodarilo uložiť
                                imageViewSelectedPhoto.setImageDrawable(null);
                            }
                        }
                    } else {
                        Log.d(TAG, "Image pick cancelled or failed.");
                        Toast.makeText(MainActivity.this, "Výber obrázka zrušený.", Toast.LENGTH_SHORT).show();
                        imageViewSelectedPhoto.setVisibility(View.GONE);
                        imageViewSelectedPhoto.setImageDrawable(null);
                    }
                });

        // Inicializácia ActivityResultLauncher pre kameru
        takePhotoLauncher = registerForActivityResult(new ActivityPhotoContract(),
                result -> {
                    if (result) { // result je boolean - true ak fotka bola uložená
                        if (photoUri != null) {
                            String savedPath = saveImageToInternalStorage(photoUri);
                            if (savedPath != null) {
                                currentPhotoPath = savedPath; // Uložte cestu k trvalo uloženému súboru
                                imageViewSelectedPhoto.setImageURI(Uri.parse(currentPhotoPath)); // Zobrazte z trvalého úložiska
                                imageViewSelectedPhoto.setVisibility(View.VISIBLE);
                                Log.d(TAG, "Photo taken and saved permanently to: " + currentPhotoPath);
                            } else {
                                Toast.makeText(MainActivity.this, "Chyba pri ukladaní fotky z fotoaparátu.", Toast.LENGTH_SHORT).show();
                                imageViewSelectedPhoto.setVisibility(View.GONE);
                                imageViewSelectedPhoto.setImageDrawable(null);
                            }
                        }
                    } else {
                        Log.d(TAG, "Photo capture cancelled or failed.");
                        Toast.makeText(MainActivity.this, "Zhotovenie fotky zrušené.", Toast.LENGTH_SHORT).show();
                        imageViewSelectedPhoto.setVisibility(View.GONE);
                        imageViewSelectedPhoto.setImageDrawable(null);
                    }
                });

        // Inicializácia ActivityResultLauncher pre povolenia
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            boolean allGranted = true;
            for (Boolean granted : permissions.values()) {
                if (!granted) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                // Skontrolujeme, či bol požiadavok na galériu alebo kameru
                if (permissions.containsKey(Manifest.permission.READ_MEDIA_IMAGES) ||
                        permissions.containsKey(Manifest.permission.READ_EXTERNAL_STORAGE)) { // Pre staršie API
                    pickImageFromGallery(); // Opätovne spustíme, ak boli povolenia pre galériu
                } else if (permissions.containsKey(Manifest.permission.CAMERA)) {
                    takePhotoFromCamera(); // Opätovne spustíme, ak boli povolenia pre kameru
                }
            } else {
                Toast.makeText(this, "Povolenia boli odmietnuté.", Toast.LENGTH_SHORT).show();
            }
        });


        // Nastav listener pre tlačidlo "Pridať fotku"
        buttonAddPhoto.setOnClickListener(v -> showPhotoSelectionDialog());

        // Nastavenie Spinnerov
        // 8. Kresba klobúka
        String[] kresbaKLOptions = {
                "Vyberte možnosť...", // Voliteľné, ak chcete "prázdny" prvý výber
                "Béžová - šedá - hnedobéžová - šedobéžová - šedohnedá",
                "Žltohnedá svetlá - žltohnedá - antuková hnedá - zeleno hnedá - olivovo hnedá",
                "Okrová žltá - oranžovohnedá - perlovooranžová - okrovo hnedá - červenooranžová",
                "Čokoládovo hnedá - orieškovo hnedá - gaštanovo hnedá",
                "Červenohnedá - mahagónová hnedá - purpurovo fialová",
                "Čiernočervená - čierna"
        };
        setupSpinner(spinner8_kresba_KL, kresbaKLOptions);

        // 9. Farba hlúbika
        String[] farbaHLOptions = {
                "Vyberte možnosť...",
                "Červená-červenohnedá-mahagón",
                "Nedá sa určiť",
                "Čokoládová-čiernočierna"
        };
        setupSpinner(spinner9_farba_HL, farbaHLOptions);

        // 10. Farba dužiny
        String[] farbaDUOptions = {
                "Vyberte možnosť...", // Voliteľné
                "Béžová až do pieskova",
                "Piesková žltá - ltohnedá svetlá",
                "Béžovohnedá - hnedobéžová, oranžovohnedá",
                "Oranžovohnedá - okrová hnedá",
                "Oriešková hnedá - antuková hnedá",
                "Oriešková - gaštanová - mahagónová"
        };
        setupSpinner(spinner10_farba_DU, farbaDUOptions);

        // 11. Farba rúrok
        String[] farbaRUOptions = {
                "Vyberte možnosť...", // Voliteľné
                "Béžová - pieskovo žltá",
                "Hnedobéžová - slonova kosť",
                "Okrová hnedá",
                "Signálna hnedá - žltohnedá svetlá",
                "Oriešková - gaštanová - antuková"
        };
        setupSpinner(spinner11_farba_RU, farbaRUOptions);
        // Inicializácia mapy pre preklad výsledkov
        hubaOutputMap.put(0.000, "Ganoderma resinaceum (Leskokôrovka živicovitá)");
        hubaOutputMap.put(0.200, "Ganoderma pfeifferi (Leskokôrovka Pfeifferova)");
        hubaOutputMap.put(0.400, "Ganoderma lucidum (Leskokôrovka obyčajná / Reishi)");
        hubaOutputMap.put(0.600, "Ganoderma carnosum (Leskokôrovka ihličnanová)");
        hubaOutputMap.put(0.800, "Ganoderma applanatum (Leskokôrovka plochá)");
        hubaOutputMap.put(1.000, "Ganoderma adspersum (Leskokôrovka tmavá)");

        // Nastavenie počúvača dotykov pre skrytie klávesnice
        setupUI(findViewById(R.id.parent_layout));

        // Načítanie FLL súboru a inicializácia FuzzyLite enginu
        loadFuzzyEngine();

        // Nastavenie poslucháča na tlačidlo
        buttonCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateFuzzyHuba();
            }
        });

        // Nastavenie poslucháča na tlačidlo Uložiť výsledok
        buttonSaveResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentResult();
            }
        });

        // Nastavenie poslucháča na tlačidlo Zobraziť históriu
        buttonHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Pomocná metóda pre nastavenie Spinneru.
     * @param spinner Spinner, ktorý sa má nastaviť.
     * @param values Pole reťazcov, ktoré budú zobrazené v Spinneri.
     */
    private void setupSpinner(Spinner spinner, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.custom_spinner_item, values); // Používame náš vlastný layout pre zatvorený spinner
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item); // Používame náš vlastný layout pre rozbalený zoznam
        spinner.setAdapter(adapter);
    }

    private void loadFuzzyEngine() {
        try {
            InputStream inputStream = getAssets().open("FIS_pokus7.fll");
            String fllContent = new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining("\n"));

            FllImporter importer = new FllImporter();
            engine = importer.fromString(fllContent);

            if (engine == null) {
                Log.e(TAG, "Chyba načítania FLL súboru: engine je null.");
                Toast.makeText(this, "Chyba: Nepodarilo sa načítať fuzzy systém.", Toast.LENGTH_LONG).show();
                return;
            }

            engine.setName("system");

            StringBuilder status = new StringBuilder();
            if (!engine.isReady(status)) {
                Log.e(TAG, "Chyba pri validácii fuzzy enginu: " + status.toString());
                Toast.makeText(this, "Chyba: Fuzzy systém nie je platný: " + status.toString(), Toast.LENGTH_LONG).show();
                engine = null;
                return;
            }

            Log.d(TAG, "Fuzzy engine načítaný a pripravený.");

        } catch (IOException e) {
            Log.e(TAG, "Chyba pri čítaní FIS_pokus7.fll z assets: " + e.getMessage());
            Toast.makeText(this, "Chyba: FLL súbor nenájdený alebo nečitateľný.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "Všeobecná chyba pri načítavaní fuzzy systému: " + e.getMessage());
            Toast.makeText(this, "Chyba: Problém s fuzzy systémom.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void calculateFuzzyHuba() {
        if (engine == null) {
            Toast.makeText(this, "Fuzzy systém nie je načítaný alebo je neplatný.", Toast.LENGTH_SHORT).show();
            textViewResult.setVisibility(View.GONE);
            buttonSaveResult.setVisibility(View.GONE);
            return;
        }

        try {
            currentInputValues = new HashMap<>();

            double val1_dlzka_S = parseEditTextInput(input1_dlzka_S, "Dĺžka spór");
            currentInputValues.put(HubaDatabaseHelper.COLUMN_DLZKA_SPOR, val1_dlzka_S);
            double val2_sirka_S = parseEditTextInput(input2_sirka_S, "Šírka spór");
            currentInputValues.put(HubaDatabaseHelper.COLUMN_SIRKA_SPOR, val2_sirka_S);

            double val3_hlubik = getRadioGroupValue(radioGroup3_hlubik, R.id.radio_hlubik_0, R.id.radio_hlubik_1, "Hlúbik");
            currentInputValues.put(HubaDatabaseHelper.COLUMN_HLUBIK, val3_hlubik);
            double val4_povrh_KL = getRadioGroupValue(radioGroup4_povrh_KL, R.id.radio_povrh_0, R.id.radio_povrh_1, "Povrch klobúka");
            currentInputValues.put(HubaDatabaseHelper.COLUMN_POVRCH_KLOBUKA, val4_povrh_KL);
            double val5_zivic_Vr = getRadioGroupValue(radioGroup5_zivic_Vr, R.id.radio_zivic_0, R.id.radio_zivic_1, "Živicová vrstva");
            currentInputValues.put(HubaDatabaseHelper.COLUMN_ZIVICOVA_VRSTVA, val5_zivic_Vr);
            double val6_tvar_KL = getRadioGroupValue(radioGroup6_tvar_KL, R.id.radio_tvar_0, R.id.radio_tvar_0_5, R.id.radio_tvar_1, "Tvar klobúka");
            currentInputValues.put(HubaDatabaseHelper.COLUMN_TVAR_KLOBUKA, val6_tvar_KL);
            double val7_farba_KL = getRadioGroupValue(radioGroup7_farba_KL, R.id.radio_farbakl_0, R.id.radio_farbakl_1, "Farba klobúka");
            currentInputValues.put(HubaDatabaseHelper.COLUMN_FARBA_KLOBUKA, val7_farba_KL);
            double val12_okraj_PL = getRadioGroupValue(radioGroup12_okraj_PL, R.id.radio_okraj_0, R.id.radio_okraj_0_5, R.id.radio_okraj_1, "Tvar okraja plodnice");
            currentInputValues.put(HubaDatabaseHelper.COLUMN_TVAR_OKRAJA, val12_okraj_PL);
            double val13_lahka_PL = getRadioGroupValue(radioGroup13_lahka_PL, R.id.radio_lahka_0, R.id.radio_lahka_1, "Bokom prirastená plodnica");
            currentInputValues.put(HubaDatabaseHelper.COLUMN_LAHKA_PLODNICA, val13_lahka_PL);

            double val8_kresba_KL = parseSpinnerInput(spinner8_kresba_KL, "Kresba klobúka");
            currentInputValues.put(HubaDatabaseHelper.COLUMN_KRESBA_KLOBUKA, val8_kresba_KL);
            double val9_farba_HL = parseFarbaHlubikaSpinnerInput(spinner9_farba_HL, "Farba hlúbika");
            currentInputValues.put(HubaDatabaseHelper.COLUMN_FARBA_HLUBIKA, val9_farba_HL);
            double val10_farba_DU = parseSpinnerInput(spinner10_farba_DU, "Farba dužiny");
            currentInputValues.put(HubaDatabaseHelper.COLUMN_FARBA_DUZINY, val10_farba_DU);
            double val11_farba_RU = parseSpinnerInput(spinner11_farba_RU, "Farba rúrok");
            currentInputValues.put(HubaDatabaseHelper.COLUMN_FARBA_RURIEK, val11_farba_RU);


            // Nastavenie hodnôt všetkým vstupným premenným v engine
            engine.getInputVariable("1_dlzka__S").setValue(val1_dlzka_S);
            engine.getInputVariable("2_sirka__S").setValue(val2_sirka_S);
            engine.getInputVariable("3__hlubik").setValue(val3_hlubik);
            engine.getInputVariable("4__povrh_KL").setValue(val4_povrh_KL);
            engine.getInputVariable("5_zivic__Vr").setValue(val5_zivic_Vr);
            engine.getInputVariable("6_tvar__KL").setValue(val6_tvar_KL);
            engine.getInputVariable("7__farba__KL").setValue(val7_farba_KL);
            engine.getInputVariable("8__kresba__KL").setValue(val8_kresba_KL);
            engine.getInputVariable("9__farba__HL").setValue(val9_farba_HL);
            engine.getInputVariable("10__farba_DU").setValue(val10_farba_DU);
            engine.getInputVariable("11__farba_RU").setValue(val11_farba_RU);
            engine.getInputVariable("12__okraj__PL").setValue(val12_okraj_PL);
            engine.getInputVariable("13__lahka__PL").setValue(val13_lahka_PL);


            // Spustenie inferencie
            engine.process();

            // Získanie výstupnej premennej "huba"
            OutputVariable outputHuba = engine.getOutputVariable("huba");

            if (outputHuba != null) {
                double hubaValue = outputHuba.getValue();

                // Preklad číselnej hodnoty na textový popis
                String hubaName = hubaOutputMap.get(hubaValue);
                if (hubaName == null) {
                    Log.w(TAG, "Výstupná hodnota '" + hubaValue + "' sa nenašla v mape. Hľadá sa najbližšia.");
                    hubaName = findClosestHubaName(hubaValue);
                    if (hubaName == null) {
                        hubaName = "Morfologické dvojičky";
                    }
                }

                currentFuzzyValue = hubaValue;
                String[] parts = hubaName.split("\\s*\\("); // Rozdelí podľa "(", prípadne s medzerami okolo
                currentLatinName = parts[0].trim();
                // Spojíme späť zvyšné časti, ak by bol slovenský názov zložený a obsahoval ďalšie zátvorky (čo je menej pravdepodobné, ale bezpečnejšie)
                this.currentCommonName = (parts.length > 1) ? parts[1].replace(")", "").trim() : "";

                // Vytvorenie formátovaného výsledku
                String formattedResult;
                if (currentCommonName != null && !currentCommonName.isEmpty()) {
                    // Ak existuje slovenský názov, zahrni ho do zátvoriek
                    formattedResult = String.format("Typ Huby: <i>%s</i> (%s)", currentLatinName, currentCommonName);
                } else {
                    // Ak slovenský názov neexistuje (napr. "Morfologické dvojičky"), zobraz len latinský
                    formattedResult = String.format("Typ Huby: <i>%s</i>", currentLatinName);
                }


                // Nastavenie textu s HTML formátovaním
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    textViewResult.setText(Html.fromHtml(formattedResult, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    textViewResult.setText(Html.fromHtml(formattedResult));
                }

                // Zobraziť výsledok po úspešnom výpočte
                textViewResult.setVisibility(View.VISIBLE);
                buttonSaveResult.setVisibility(View.VISIBLE);

                Log.d(TAG, "Výsledok fuzzy logiky: " + hubaName + " (" + hubaValue + ")");

            } else {
                Log.e(TAG, "Výstupná premenná 'huba' nenájdená v engine.");
                Toast.makeText(this, "Chyba: Výstupná premenná 'huba' nie je definovaná.", Toast.LENGTH_LONG).show();
                // Skryť výsledok, ak je chyba
                textViewResult.setVisibility(View.GONE);
                buttonSaveResult.setVisibility(View.GONE);
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Chyba: Zadajte platné číselné hodnoty pre všetky vstupy.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Chyba formátu čísla: " + e.getMessage());
            // Skryť výsledok, ak je chyba
            textViewResult.setVisibility(View.GONE);
            buttonSaveResult.setVisibility(View.GONE);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Chyba vstupu fuzzy logiky: " + e.getMessage());
            // Skryť výsledok, ak je chyba
            textViewResult.setVisibility(View.GONE);
            buttonSaveResult.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "Všeobecná chyba pri výpočte fuzzy logiky: " + e.getMessage());
            Toast.makeText(this, "Chyba pri výpočte.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            // Skryť výsledok, ak je chyba
            textViewResult.setVisibility(View.GONE);
            buttonSaveResult.setVisibility(View.GONE);
        }
    }

    /**
     * Pomocná metóda na parsovanie vstupov z EditTextu a overenie, či nie sú prázdne.
     */
    private double parseEditTextInput(EditText editText, String variableName) throws NumberFormatException, IllegalArgumentException {
        String inputStr = editText.getText().toString();
        if (inputStr.isEmpty()) {
            throw new IllegalArgumentException("Zadajte hodnotu pre " + variableName + ".");
        }
        return Double.parseDouble(inputStr);
    }

    private double getRadioGroupValue(RadioGroup radioGroup, int idForZero, int idForOne, String variableName) throws IllegalArgumentException {
        int checkedId = radioGroup.getCheckedRadioButtonId();
        if (checkedId == -1) {
            throw new IllegalArgumentException("Vyberte hodnotu pre " + variableName + ".");
        } else if (checkedId == idForZero) {
            return 0.0;
        } else if (checkedId == idForOne) {
            return 1.0;
        }
        // Toto by sa nemalo stať, ak sú ID správne a existujú len 2 možnosti
        throw new IllegalArgumentException("Neplatný výber pre " + variableName + ".");
    }

    /**
     * Pomocná metóda na získanie hodnoty z RadioGroup (pre 0.0 alebo 1.0),
     * bez ohľadu na text RadioButtonu.
     * @param radioGroup RadioGroup, z ktorej sa má získať hodnota.
     * @param idForZero ID RadioButtonu, ktorý reprezentuje 0.0.
     * @param idForOne ID RadioButtonu, ktorý reprezentuje 1.0.
     * @param variableName Názov premennej pre chybové hlásenia.
     * @return 0.0 alebo 1.0.
     * @throws IllegalArgumentException Ak nie je vybraný žiadny RadioButton.
     */
    private double getRadioGroupValue(RadioGroup radioGroup, int idForZero, int idForHalf, int idForOne, String variableName) throws IllegalArgumentException {
        int checkedId = radioGroup.getCheckedRadioButtonId();
        if (checkedId == -1) {
            throw new IllegalArgumentException("Vyberte hodnotu pre " + variableName + ".");
        } else if (checkedId == idForZero) {
            return 0.0;
        } else if (checkedId == idForHalf) { // Kontrola pre 0.5
            return 0.5;
        } else if (checkedId == idForOne) {
            return 1.0;
        }
        // Toto by sa nemalo stať, ak sú ID správne a existujú len 3 možnosti
        throw new IllegalArgumentException("Neplatný výber pre " + variableName + ".");
    }


    /**
     * Pomocná metóda na získanie hodnoty zo Spinneru.
     */
    private double parseSpinnerInput(Spinner spinner, String variableName) throws IllegalArgumentException {
        int selectedPosition = spinner.getSelectedItemPosition();
        if (selectedPosition == AdapterView.INVALID_POSITION || selectedPosition == 0) { // Ak je vybraný prvý element ("Vyberte možnosť...") alebo nič
            throw new IllegalArgumentException("Vyberte platnú možnosť pre " + variableName + ".");
        }
        // Vráti pozíciu + 0.0, aby sa dostala hodnota 1.0 pre prvú možnosť, 2.0 pre druhú atď.
        return (double) selectedPosition; // Ak "Vyberte možnosť..." je na indexe 0, potom 1. možnosť (index 1) sa stane 1.0, 2. možnosť (index 2) sa stane 2.0 atď.
    }

    /**
     * Pomocná metóda na získanie hodnoty zo Spinneru pre Farbu Hlúbika (mapuje indexy na 0.0, 0.5, 1.0).
     */
    private double parseFarbaHlubikaSpinnerInput(Spinner spinner, String variableName) throws IllegalArgumentException {
        int selectedPosition = spinner.getSelectedItemPosition();

        if (selectedPosition == AdapterView.INVALID_POSITION || selectedPosition == 0) {
            throw new IllegalArgumentException("Vyberte platnú možnosť pre " + variableName + ".");
        }

        // Mapovanie indexov na konkrétne hodnoty pre fuzzy engine:
        switch (selectedPosition) {
            case 1: // "Červená-červenohnedá-mahagón"
                return 0.0;
            case 2: // "Nedá sa určiť"
                return 0.5;
            case 3: // "Čokoládová-čiernočierna"
                return 1.0;
            default:
                // Toto by sa nemalo stať, ak sú možnosti správne definované a validované
                throw new IllegalArgumentException("Neplatný výber pre " + variableName + ".");
        }
    }

    /**
     * Pomocná metóda na nájdenie najbližšej huby, ak sa presná zhoda nenájde.
     */
    private String findClosestHubaName(double value) {
        String closestName = null;
        double minDifference = Double.MAX_VALUE;

        for (Map.Entry<Double, String> entry : hubaOutputMap.entrySet()) {
            double diff = Math.abs(value - entry.getKey());
            if (diff < minDifference) {
                minDifference = diff;
                closestName = entry.getValue();
            }
        }
        // Ak je rozdiel väčší ako 0.05, nemusí to byť dostatočne blízke
        if (minDifference > 0.05) {
            return null; // Nenašla sa dostatočne blízka hodnota
        }
        return closestName;
    }

    public void setupUI(View view) {
        // Ak View nie je EditText, nastav naň dotykový listener
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideKeyboard();
                    // Ak je fokus na nejakom EditText, po kliknutí mimo neho sa fokus vymaže
                    View currentFocusedView = MainActivity.this.getCurrentFocus();
                    if (currentFocusedView instanceof EditText) {
                        currentFocusedView.clearFocus();
                    }
                    return false; // Dôležité: vráť false, aby sa udalosť ďalej spracovala
                }
            });
        }

        // Ak je to ViewGroup (napr. LinearLayout, RelativeLayout), rekurzívne nastav na jeho deti
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    /**
     * Pomocná metóda na programové skrytie softvérovej klávesnice.
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Metóda na uloženie aktuálneho výsledku do databázy.
     */
    private void saveCurrentResult() {
        Log.d(TAG, "saveCurrentResult() volaná.");
        if (currentLatinName == null || currentLatinName.isEmpty() || // Pridal som .isEmpty() pre robustnosť
                currentCommonName == null || currentCommonName.isEmpty() || // Pridal som .isEmpty()
                currentInputValues == null || currentInputValues.isEmpty()) { // Pridal som .isEmpty()
            Log.e(TAG, "Chyba: Niektoré dáta na uloženie sú null alebo mapa vstupov je prázdna.");
            Log.e(TAG, "  currentLatinName: '" + currentLatinName + "'"); // Pre lepšie debug logy
            Log.e(TAG, "  currentCommonName: '" + currentCommonName + "'");
            Log.e(TAG, "  currentFuzzyValue: " + currentFuzzyValue);
            Log.e(TAG, "  currentInputValues: " + (currentInputValues == null ? "NULL" : "Size: " + currentInputValues.size()));
            Toast.makeText(this, "Nie je k dispozícii žiadny výsledok na uloženie.", Toast.LENGTH_SHORT).show();
            return;
        }

        long id = dbHelper.insertHubaRecord(
                currentInputValues,
                currentLatinName,
                currentCommonName,
                currentFuzzyValue,
                currentPhotoPath
        );

        if (id != -1) {
            Toast.makeText(this, "Výsledok úspešne uložený do histórie!", Toast.LENGTH_SHORT).show();
            // Po uložení môžeš tlačidlo uložiť znova skryť alebo ho nechať viditeľné
            // buttonSaveResult.setVisibility(View.GONE);
            currentPhotoPath = null;
        } else {
            Toast.makeText(this, "Chyba pri ukladaní výsledku.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPhotoSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Vybrať fotku");
        builder.setItems(new CharSequence[]{"Z galérie", "Z fotoaparátu"}, (dialog, which) -> {
            switch (which) {
                case 0: // Z galérie
                    pickImageFromGallery();
                    break;
                case 1: // Z fotoaparátu
                    takePhotoFromCamera();
                    break;
            }
        });
        builder.show();
    }

    private void pickImageFromGallery() {
        if (checkAndRequestPermissionsForGallery()) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        } else {
            // Permissions are requested by checkAndRequestPermissionsForGallery(), so this else might not be strictly necessary
            // or could just be a fallback message.
            Toast.makeText(this, "Pre výber fotky z galérie sú potrebné povolenia.", Toast.LENGTH_SHORT).show();
        }
    }

    private void takePhotoFromCamera() {
        if (checkAndRequestPermissionsForCamera()) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Zabezpeč, aby bol k dispozícii Camera app
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Log.e(TAG, "Error creating image file", ex);
                    Toast.makeText(this, "Chyba pri vytváraní súboru pre fotku.", Toast.LENGTH_SHORT).show();
                }
                // Pokračovať len vtedy, ak bol súbor úspešne vytvorený
                if (photoFile != null) {
                    photoUri = FileProvider.getUriForFile(
                            this,
                            "com.example.myapplication.fileprovider", // MUSÍ ZODPOVEDAŤ AUTHORITIES V MANIFESTE A resources/xml/file_paths.xml
                            photoFile
                    );
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    takePhotoLauncher.launch(photoUri); // Používame photoUri, pretože ActivityPhotoContract to očakáva
                }
            } else {
                Toast.makeText(this, "Žiadna aplikácia pre fotoaparát nie je dostupná.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Permissions are requested by checkAndRequestPermissionsForCamera(), so this else might not be strictly necessary
            // or could just be a fallback message.
            Toast.makeText(this, "Pre použitie fotoaparátu sú potrebné povolenia.", Toast.LENGTH_SHORT).show();
        }
    }

    //region Permission Handling
    private boolean checkAndRequestPermissionsForGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                requestPermissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_IMAGES});
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                requestPermissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
                return false;
            }
        }
    }

    private boolean checkAndRequestPermissionsForCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
            return false;
        }
    }
    //endregion

    //region Image File Operations

    /**
     * Vytvorí dočasný súbor obrázka v externom úložisku aplikácie.
     *
     * @return Súbor, do ktorého sa má uložiť obrázok z fotoaparátu.
     * @throws IOException Ak sa nepodarí vytvoriť súbor.
     */
    private File createImageFile() throws IOException {
        // Vytvorte názov súboru obrázka
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            throw new IOException("External storage directory not available.");
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    /**
     * Uloží vybraný obrázok z Uri (galérie) do interného úložiska aplikácie
     * a vráti cestu k novému súboru.
     *
     * @param sourceUri Uri vybraného obrázka.
     * @return Cesta k uloženému súboru v internom úložisku, alebo null ak sa uloženie nepodarilo.
     */
    private String saveImageToInternalStorage(Uri sourceUri) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";

        File destinationFile = new File(getFilesDir(), fileName); // Uloženie do interného úložiska

        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(destinationFile)) {

            if (inputStream == null) {
                Log.e(TAG, "Input stream for URI is null: " + sourceUri);
                return null;
            }

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            Log.d(TAG, "Image saved to: " + destinationFile.getAbsolutePath());
            return destinationFile.getAbsolutePath(); // Vraciame absolútnu cestu k súboru
        } catch (IOException e) {
            Log.e(TAG, "Error saving image to internal storage: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    //endregion

    /**
     * Vlastný kontrakt pre ActivityResultLauncher, ktorý namiesto Intentu prijíma Uri.
     * Je to len ukážka pre demonštráciu, že ActivityResultContract možno prispôsobiť.
     * V reálnej aplikácii by sa pre kameru často používal len Intent s EXTRA_OUTPUT
     * a potom by sa URI použilo priamo.
     */
    private static class ActivityPhotoContract extends ActivityResultContract<Uri, Boolean> {

        // input je Uri, ktorý predstavuje cestu, kam sa má uložiť fotografia
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, Uri input) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, input);
            return takePictureIntent;
        }

        // result je boolean, true ak bolo úspešne uložené, false inak
        @Override
        public Boolean parseResult(int resultCode, @Nullable Intent intent) {
            // Pre MediaStore.ACTION_IMAGE_CAPTURE s EXTRA_OUTPUT, dáta v Intent sú často null.
            // Úspech sa posudzuje podľa resultCode.
            return resultCode == Activity.RESULT_OK;
        }
    }
}