package fr.didictateur.inanutshell.data.response;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("access_token")
    private String accessToken;
    
    @SerializedName("token")
    private String token;
    
    @SerializedName("token_type")
    private String tokenType;

    // Constructeurs
    public LoginResponse() {}

    // Getters et Setters
    public String getAccessToken() { 
        // Try access_token first, then token
        return accessToken != null ? accessToken : token; 
    }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    
    public String getFullToken() {
        return (tokenType != null ? tokenType + " " : "Bearer ") + accessToken;
    }
}
