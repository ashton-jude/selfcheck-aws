package org.ashtonjude;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Student {
    String uuid;
    String firstName;
    String lastName;
    int grade;
    String photo;
    Boolean isRegistered;

    String emotion;

    public Student() {}

    public Student(String json) {
        Gson gson = new Gson();
        Student request = gson.fromJson(json, Student.class);
        this.uuid = request.uuid;
        this.firstName = request.firstName;
        this.lastName = request.lastName;
        this.grade = request.grade;
        this.photo = request.photo;
        this.isRegistered = request.isRegistered;
    }

    public String toString() {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}