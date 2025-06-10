package com.example.myapplication;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Button;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HubaRecordAdapter extends RecyclerView.Adapter<HubaRecordAdapter.HubaViewHolder> {

    private List<HubaRecord> hubaRecords;
    private OnItemClickListener listener;
    private OnExportClickListener exportClickListener;

    public interface OnExportClickListener {
        void onExportClick(HubaRecord record);
    }

    // Rozhranie pre spracovanie kliknutí
    public interface OnItemClickListener {
        void onDeleteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public HubaRecordAdapter(List<HubaRecord> hubaRecords, OnExportClickListener listener) {
        this.hubaRecords = hubaRecords;
        this.exportClickListener = listener;
    }

    public void updateData(List<HubaRecord> newRecords) {
        this.hubaRecords = newRecords;
    }

    @NonNull
    @Override
    public HubaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_huba_record, parent, false);
        return new HubaViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull HubaViewHolder holder, int position) {
        HubaRecord record = hubaRecords.get(position);

        // Zmena z tvLatinName, tvCommonName na tvHubaName
        holder.tvHubaName.setText(String.format("<i>%s</i> (%s)", record.getLatinskyNazov(), record.getSlovenskyNazov()));
        // Nastavenie textu s HTML formátovaním (pre <i> tag)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            holder.tvHubaName.setText(android.text.Html.fromHtml(holder.tvHubaName.getText().toString(), android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            holder.tvHubaName.setText(android.text.Html.fromHtml(holder.tvHubaName.getText().toString()));
        }


        holder.tvFuzzyValue.setText(String.format(Locale.getDefault(), "Fuzzy hodnota: %.3f", record.getHodnotaFuzzy()));

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        if (record.getDatumUrcenia() != null) {
            holder.tvDate.setText("Dátum: " + sdf.format(record.getDatumUrcenia()));
        } else {
            holder.tvDate.setText("Dátum: N/A");
        }

        // Nastavenie vstupných hodnôt (s volaním pomocných metód pre preklad)
        holder.tvDlzkaSpor.setText(String.format(Locale.getDefault(), "Dĺžka spór: %.2f", record.getDlzkaSpor()));
        holder.tvSirkaSpor.setText(String.format(Locale.getDefault(), "Šírka spór: %.2f", record.getSirkaSpor()));

        // RadioGroup hodnoty (0.0/1.0 a 0.0/0.5/1.0)
        holder.tvHlubik.setText("Hlúbik: " + (record.getHlubik() == 1.0 ? "Áno" : "Nie"));
        holder.tvPovrchKlobuka.setText("Povrch klobúka: " + (record.getPovrchKlobuka() == 1.0 ? "Áno" : "Nie"));
        holder.tvZivicovaVrstva.setText("Živicová vrstva: " + (record.getZivicovaVrstva() == 1.0 ? "Áno" : "Nie"));
        holder.tvTvarKlobuka.setText("Tvar klobúka: " + getTvarKlobukaText(record.getTvarKlobuka()));
        holder.tvFarbaKlobuka.setText("Farba klobúka: " + getFarbaKlobukaText(record.getFarbaKlobuka()));
        holder.tvTvarOkraja.setText("Tvar okraja plodnice: " + getOkrajPlodniceText(record.getTvarOkraja()));
        holder.tvLahkaPlodnica.setText("Bokom prirastená plodnica: " + (record.getLahkaPlodnica() == 1.0 ? "Áno" : "Nie"));

        // Spinner hodnoty (kde hodnota v DB je pozícia spinnera)
        holder.tvKresbaKlobuka.setText("Kresba klobúka: " + getKresbaKlobukaText(record.getKresbaKlobuka()));
        holder.tvFarbaHlubika.setText("Farba hlúbika: " + getFarbaHlubikaText(record.getFarbaHlubika())); // OPRAVENÉ N/A
        holder.tvFarbaDuziny.setText("Farba dužiny: " + getFarbaDuzinyText(record.getFarbaDuziny()));
        holder.tvFarbaRuriek.setText("Farba rúrok: " + getFarbaRurokText(record.getFarbaRuriek()));


        // Zobrazenie fotky
        if (record.getFotoPath() != null && !record.getFotoPath().isEmpty()) {
            File imgFile = new File(record.getFotoPath());
            if (imgFile.exists()) {
                holder.imageViewHistoryPhoto.setImageURI(Uri.fromFile(imgFile));
                holder.imageViewHistoryPhoto.setVisibility(View.VISIBLE);
            } else {
                holder.imageViewHistoryPhoto.setVisibility(View.GONE);
                holder.imageViewHistoryPhoto.setImageDrawable(null);
            }
        } else {
            holder.imageViewHistoryPhoto.setVisibility(View.GONE);
            holder.imageViewHistoryPhoto.setImageDrawable(null);
        }

        // Nastavenie OnClickListener pre tlačidlo exportu
        holder.buttonExportRecord.setOnClickListener(v -> {
            if (exportClickListener != null) {
                exportClickListener.onExportClick(record);
            }
        });

        holder.buttonDeleteRecord.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return hubaRecords.size();
    }

    // --- Pomocné metódy pre preklad číselných hodnôt na text ---

    // Pre 6. Tvar klobúka, 12. Okraj plodnice (0.0/0.5/1.0)
    private String getTvarKlobukaText(double value) {
        if (value == 0.0) return "guľovitý";
        if (value == 0.5) return "polguľovitý";
        if (value == 1.0) return "plochý";
        return "N/A";
    }

    // Pre 7. Farba klobúka (0.0/1.0)
    private String getFarbaKlobukaText(double value) {
        if (value == 0.0) return "svetlá";
        if (value == 1.0) return "tmavá";
        return "N/A";
    }

    // Pre 12. Okraj plodnice (0.0/0.5/1.0) - už definované ako getTvarKlobukaText, môžeme premenovať alebo použiť rovnaké
    private String getOkrajPlodniceText(double value) {
        if (value == 0.0) return "rovný";
        if (value == 0.5) return "vlnitý";
        if (value == 1.0) return "podvinutý";
        return "N/A";
    }

    // Pre 9. Farba hlúbika (0.0, 0.5, 1.0 - špecifické mapovanie)
    private String getFarbaHlubikaText(double value) {
        if (value == 0.0) return "Červená-červenohnedá-mahagón";
        if (value == 0.5) return "Nedá sa určiť";
        if (value == 1.0) return "Čokoládová-čiernočierna";
        return "N/A"; // Pre prípad neznámej hodnoty
    }

    // Pre 8. Kresba klobúka (pozície spinnera 1-6)
    private String getKresbaKlobukaText(double value) {
        switch ((int) value) { // Pretypujeme na int, keďže sú to celočíselné pozície
            case 1: return "Béžová - šedá - hnedobéžová - šedobéžová - šedohnedá";
            case 2: return "Žltohnedá svetlá - žltohnedá - antuková hnedá - zeleno hnedá - olivovo hnedá";
            case 3: return "Okrová žltá - oranžovohnedá - perlovooranžová - okrovo hnedá - červenooranžová";
            case 4: return "Čokoládovo hnedá - orieškovo hnedá - gaštanovo hnedá";
            case 5: return "Červenohnedá - mahagónová hnedá - purpurovo fialová";
            case 6: return "Čiernočervená - čierna";
            default: return "N/A";
        }
    }

    // Pre 10. Farba dužiny (pozície spinnera 1-6)
    private String getFarbaDuzinyText(double value) {
        switch ((int) value) {
            case 1: return "Béžová až do pieskova";
            case 2: return "Piesková žltá - žltohnedá svetlá";
            case 3: return "Béžovohnedá - hnedobéžová, oranžovohnedá";
            case 4: return "Oranžovohnedá - okrová hnedá";
            case 5: return "Oriešková hnedá - antuková hnedá";
            case 6: return "Oriešková - gaštanová - mahagónová";
            default: return "N/A";
        }
    }

    // Pre 11. Farba rúrok (pozície spinnera 1-5)
    private String getFarbaRurokText(double value) {
        switch ((int) value) {
            case 1: return "Béžová - pieskovo žltá";
            case 2: return "Hnedobéžová - slonova kosť";
            case 3: return "Okrová hnedá";
            case 4: return "Signálna hnedá - žltohnedá svetlá";
            case 5: return "Oriešková - gaštanová - antuková";
            default: return "N/A";
        }
    }


    public static class HubaViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvHubaName;
        TextView tvFuzzyValue;
        ImageView imageViewHistoryPhoto;
        ImageButton buttonDeleteRecord;

        Button buttonExportRecord;

        // Vstupné parametre TextViews
        TextView tvDlzkaSpor;
        TextView tvSirkaSpor;
        TextView tvHlubik;
        TextView tvPovrchKlobuka;
        TextView tvZivicovaVrstva;
        TextView tvTvarKlobuka;
        TextView tvFarbaKlobuka;
        TextView tvKresbaKlobuka;
        TextView tvFarbaHlubika;
        TextView tvFarbaDuziny;
        TextView tvFarbaRuriek;
        TextView tvTvarOkraja;
        TextView tvLahkaPlodnica;


        public HubaViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvHubaName = itemView.findViewById(R.id.tvHubaName);
            tvFuzzyValue = itemView.findViewById(R.id.tvFuzzyValue);
            imageViewHistoryPhoto = itemView.findViewById(R.id.imageViewHistoryPhoto);
            buttonDeleteRecord = itemView.findViewById(R.id.buttonDeleteRecord);
            buttonExportRecord = itemView.findViewById(R.id.buttonExportRecord);

            // Inicializácia vstupných parametrov
            tvDlzkaSpor = itemView.findViewById(R.id.tvDlzkaSpor);
            tvSirkaSpor = itemView.findViewById(R.id.tvSirkaSpor);
            tvHlubik = itemView.findViewById(R.id.tvHlubik);
            tvPovrchKlobuka = itemView.findViewById(R.id.tvPovrchKlobuka);
            tvZivicovaVrstva = itemView.findViewById(R.id.tvZivicovaVrstva);
            tvTvarKlobuka = itemView.findViewById(R.id.tvTvarKlobuka);
            tvFarbaKlobuka = itemView.findViewById(R.id.tvFarbaKlobuka);
            tvKresbaKlobuka = itemView.findViewById(R.id.tvKresbaKlobuka);
            tvFarbaHlubika = itemView.findViewById(R.id.tvFarbaHlubika);
            tvFarbaDuziny = itemView.findViewById(R.id.tvFarbaDuziny);
            tvFarbaRuriek = itemView.findViewById(R.id.tvFarbaRuriek);
            tvTvarOkraja = itemView.findViewById(R.id.tvTvarOkraja);
            tvLahkaPlodnica = itemView.findViewById(R.id.tvLahkaPlodnica);

            // Nastavenie poslucháča pre tlačidlo zmazania
            buttonDeleteRecord.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onDeleteClick(position);
                    }
                }
            });
        }
    }
}