package com.meeting.common.model;

import java.io.Serializable;

public class Room implements Serializable {
    private int id;
    private String name;
    private int capacity;
    private String location;
    private String equipment;
    private String status; // "AVAILABLE", "MAINTENANCE"

    public Room() {}

    public Room(int id, String name, int capacity, String location, String equipment, String status) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.location = location;
        this.equipment = equipment;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return name + " (" + capacity + " người - " + location + ")";
    }
}
