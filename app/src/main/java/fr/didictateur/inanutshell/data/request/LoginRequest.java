package fr.didictateur.inanutshell.data.request;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("username")
    private String username;
    
    @SerializedName("password")
    private String password;

    // Constructeurs
    public LoginRequest() {}
    
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters et Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
