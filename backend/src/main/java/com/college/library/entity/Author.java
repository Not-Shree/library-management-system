package com.college.library.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "authors")
public class Author extends BaseEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 1000)
    private String biography;

    // ----- getters & setters -----
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBiography() { return biography; }
    public void setBiography(String biography) { this.biography = biography; }
}
