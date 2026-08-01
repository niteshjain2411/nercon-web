package org.example.model;

import java.io.Serializable;

public class Workshop implements Serializable {
    private String id;
    private long bookedSlots;
    private long maxSlots;
    private String content;
    private String durationType;
    private String resourcePerson;

    // Constructors
    public Workshop() {
    }

    public Workshop(String id, long bookedSlots, long maxSlots, String content, String durationType, String resourcePerson) {
        this.id = id;
        this.bookedSlots = bookedSlots;
        this.maxSlots = maxSlots;
        this.content = content;
        this.durationType = durationType;
        this.resourcePerson = resourcePerson;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getBookedSlots() { return bookedSlots; }
    public void setBookedSlots(long bookedSlots) { this.bookedSlots = bookedSlots; }

    public long getMaxSlots() { return maxSlots; }
    public void setMaxSlots(long maxSlots) { this.maxSlots = maxSlots; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDurationType() { return durationType; }
    public void setDurationType(String durationType) { this.durationType = durationType; }

    public String getResourcePerson() { return resourcePerson; }
    public void setResourcePerson(String resourcePerson) { this.resourcePerson = resourcePerson; }
}
