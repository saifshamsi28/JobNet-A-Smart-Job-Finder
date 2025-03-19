package com.saif.jobnet;

import androidx.annotation.NonNull;

public class Course {
    private String name;
    private String programme;
    private String levell;
    private String _department;
    private String discipline;
    private String duration_year;

    public Course(String name, String programme, String levell, String _department, String discipline, String duration_year) {
        this.name = name;
        this.programme = programme;
        this.levell = levell;
        this._department = _department;
        this.discipline = discipline;
        this.duration_year = duration_year;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProgramme() {
        return programme;
    }

    public void setProgramme(String programme) {
        this.programme = programme;
    }

    public String getLevell() {
        return levell;
    }

    public void setLevell(String levell) {
        this.levell = levell;
    }

    public String get_department() {
        return _department;
    }

    public void set_department(String _department) {
        this._department = _department;
    }

    public String getDiscipline() {
        return discipline;
    }

    public void setDiscipline(String discipline) {
        this.discipline = discipline;
    }

    public String getDuration_year() {
        return duration_year;
    }

    public void setDuration_year(String duration_year) {
        this.duration_year = duration_year;
    }

    @NonNull
    @Override
    public String toString() {
        return "Course{" + "name=" + name + ", programme=" + programme + ", levell=" + levell + ", _department=" + _department + ", discipline=" + discipline + ", duration_year=" + duration_year + "}";
    }
}
