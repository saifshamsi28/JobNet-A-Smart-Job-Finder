package com.saif.jobnet;

import androidx.annotation.NonNull;

public class Course {
    private String name;
    private String programme;
    private String levell;
    private String _department;
    private String discipline;

    public Course(String name, String programme, String levell, String _department, String discipline) {
        this.name = name;
        this.programme = programme;
        this.levell = levell;
        this._department = _department;
        this.discipline = discipline;
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

    @NonNull
    @Override
    public String toString() {
        return "Course{" + "name=" + name + ", programme=" + programme + ", levell=" + levell + ", _department=" + _department + ", discipline=" + discipline + "}";
    }
}
