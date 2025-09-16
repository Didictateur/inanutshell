package fr.didictateur.inanutshell.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Instruction implements Parcelable {
    private int stepNumber;
    private String text;
    private String title;
    private String imageUrl;
    private String description;
    private int estimatedTime; // en minutes
    private boolean completed;
    
    // Constructors
    public Instruction() {}
    
    public Instruction(int stepNumber, String text) {
        this.stepNumber = stepNumber;
        this.text = text;
        this.completed = false;
    }
    
    public Instruction(int stepNumber, String text, String title, String description) {
        this.stepNumber = stepNumber;
        this.text = text;
        this.title = title;
        this.description = description;
        this.completed = false;
    }
    
    // Getters and Setters
    public int getStepNumber() { return stepNumber; }
    public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getEstimatedTime() { return estimatedTime; }
    public void setEstimatedTime(int estimatedTime) { this.estimatedTime = estimatedTime; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    // Utility methods
    public String getDisplayText() {
        if (title != null && !title.isEmpty()) {
            return stepNumber + ". " + title + " - " + text;
        }
        return stepNumber + ". " + text;
    }
    
    @Override
    public String toString() {
        return getDisplayText();
    }
    
    // Parcelable implementation
    protected Instruction(Parcel in) {
        stepNumber = in.readInt();
        text = in.readString();
        title = in.readString();
        imageUrl = in.readString();
        description = in.readString();
        estimatedTime = in.readInt();
        completed = in.readByte() != 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(stepNumber);
        dest.writeString(text);
        dest.writeString(title);
        dest.writeString(imageUrl);
        dest.writeString(description);
        dest.writeInt(estimatedTime);
        dest.writeByte((byte) (completed ? 1 : 0));
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<Instruction> CREATOR = new Creator<Instruction>() {
        @Override
        public Instruction createFromParcel(Parcel in) {
            return new Instruction(in);
        }
        
        @Override
        public Instruction[] newArray(int size) {
            return new Instruction[size];
        }
    };
}
