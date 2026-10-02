package com.college.library.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "publishers")
public class Publisher extends BaseEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(length = 255)
    private String website;

    // ----- getters & setters -----
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
