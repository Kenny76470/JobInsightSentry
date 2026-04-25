package com.job.hunter.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_configs")
public class UserConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String searchKeyword;

    private int minSalary;

    private String lineNotifyToken;

    // --- Getter / Setter ---
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getSearchKeyword() { return searchKeyword; }
    public void setSearchKeyword(String searchKeyword) { this.searchKeyword = searchKeyword; }
    public int getMinSalary() { return minSalary; }
    public void setMinSalary(int minSalary) { this.minSalary = minSalary; }
    public String getLineNotifyToken() { return lineNotifyToken; }
    public void setLineNotifyToken(String lineNotifyToken) { this.lineNotifyToken = lineNotifyToken; }
}