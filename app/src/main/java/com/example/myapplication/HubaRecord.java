package com.example.myapplication;

import java.util.Date;

// Toto je dátový model pre záznamy o hube, ktoré sa budú ukladať/načítavať z databázy
public class HubaRecord {
    private int id;
    private Date datumUrcenia;
    private String latinskyNazov;
    private String slovenskyNazov;
    private double hodnotaFuzzy;

    // Vstupné hodnoty (fuzzy)
    private double dlzkaSpor;
    private double sirkaSpor;
    private double hlubik;
    private double povrchKlobuka;
    private double zivicovaVrstva;
    private double tvarKlobuka;
    private double farbaKlobuka;
    private double kresbaKlobuka;
    private double farbaHlubika;
    private double farbaDuziny;
    private double farbaRuriek;
    private double tvarOkraja;
    private double lahkaPlodnica;
    private String fotoPath;

    // Konštruktory (môžeš pridať aj konštruktor s parametrami, ak potrebuješ)
    public HubaRecord() {
    }

    // Gettery a Settery pre všetky vlastnosti

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDatumUrcenia() {
        return datumUrcenia;
    }

    public void setDatumUrcenia(Date datumUrcenia) {
        this.datumUrcenia = datumUrcenia;
    }

    public String getLatinskyNazov() {
        return latinskyNazov;
    }

    public void setLatinskyNazov(String latinskyNazov) {
        this.latinskyNazov = latinskyNazov;
    }

    public String getSlovenskyNazov() {
        return slovenskyNazov;
    }

    public void setSlovenskyNazov(String slovenskyNazov) {
        this.slovenskyNazov = slovenskyNazov;
    }

    public double getHodnotaFuzzy() {
        return hodnotaFuzzy;
    }

    public void setHodnotaFuzzy(double hodnotaFuzzy) {
        this.hodnotaFuzzy = hodnotaFuzzy;
    }

    // Gettery a Settery pre vstupné hodnoty

    public double getDlzkaSpor() {
        return dlzkaSpor;
    }

    public void setDlzkaSpor(double dlzkaSpor) {
        this.dlzkaSpor = dlzkaSpor;
    }

    public double getSirkaSpor() {
        return sirkaSpor;
    }

    public void setSirkaSpor(double sirkaSpor) {
        this.sirkaSpor = sirkaSpor;
    }

    public double getHlubik() {
        return hlubik;
    }

    public void setHlubik(double hlubik) {
        this.hlubik = hlubik;
    }

    public double getPovrchKlobuka() {
        return povrchKlobuka;
    }

    public void setPovrchKlobuka(double povrchKlobuka) {
        this.povrchKlobuka = povrchKlobuka;
    }

    public double getZivicovaVrstva() {
        return zivicovaVrstva;
    }

    public void setZivicovaVrstva(double zivicovaVrstva) {
        this.zivicovaVrstva = zivicovaVrstva;
    }

    public double getTvarKlobuka() {
        return tvarKlobuka;
    }

    public void setTvarKlobuka(double tvarKlobuka) {
        this.tvarKlobuka = tvarKlobuka;
    }

    public double getFarbaKlobuka() {
        return farbaKlobuka;
    }

    public void setFarbaKlobuka(double farbaKlobuka) {
        this.farbaKlobuka = farbaKlobuka;
    }

    public double getKresbaKlobuka() {
        return kresbaKlobuka;
    }

    public void setKresbaKlobuka(double kresbaKlobuka) {
        this.kresbaKlobuka = kresbaKlobuka;
    }

    public double getFarbaHlubika() {
        return farbaHlubika;
    }

    public void setFarbaHlubika(double farbaHlubika) {
        this.farbaHlubika = farbaHlubika;
    }

    public double getFarbaDuziny() {
        return farbaDuziny;
    }

    public void setFarbaDuziny(double farbaDuziny) {
        this.farbaDuziny = farbaDuziny;
    }

    public double getFarbaRuriek() {
        return farbaRuriek;
    }

    public void setFarbaRuriek(double farbaRuriek) {
        this.farbaRuriek = farbaRuriek;
    }

    public double getTvarOkraja() {
        return tvarOkraja;
    }

    public void setTvarOkraja(double tvarOkraja) {
        this.tvarOkraja = tvarOkraja;
    }

    public double getLahkaPlodnica() {
        return lahkaPlodnica;
    }

    public void setLahkaPlodnica(double lahkaPlodnica) {
        this.lahkaPlodnica = lahkaPlodnica;
    }

    // Prepísanie toString pre ľahšie ladenie (voliteľné)
    @Override
    public String toString() {
        return "ID: " + id +
                ", Dátum: " + datumUrcenia +
                ", Huba: " + latinskyNazov + " (" + slovenskyNazov + ")" +
                ", Fuzzy hodnota: " + hodnotaFuzzy;
    }

    public String getFotoPath() {
        return fotoPath;
    }

    public void setFotoPath(String fotoPath) {
        this.fotoPath = fotoPath;
    }
}