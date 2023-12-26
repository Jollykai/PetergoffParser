package org.example.Models;

import lombok.Data;

@Data
public class Volunteer {
    private String id;
    private String name;
    private Integer counter;

    public Volunteer(String id, String name) {
        this.id = id;
        this.name = name;
        this.counter = 1;
    }

    public void incrementCounter() {
        this.counter++;
    }

    @Override
    public String toString() {
        return id + ";" + name + ";" + counter;
    }

}
