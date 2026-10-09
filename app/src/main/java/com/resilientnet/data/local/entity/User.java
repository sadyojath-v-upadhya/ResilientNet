package com.resilientnet.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "users",
        indices = {
                @Index(value = {"email"}, unique = true)
        }
)
public class User {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "full_name")
    private String fullName;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "phone_number")
    private String phoneNumber;

    @ColumnInfo(name = "password")
    private String password;

    @ColumnInfo(name = "emergency_contact")
    private String emergencyContact;

    @ColumnInfo(name = "blood_group")
    private String bloodGroup;

    @ColumnInfo(name = "age")
    private Integer age;

    @ColumnInfo(name = "gender")
    private String gender;

    @ColumnInfo(name = "home_base")
    private String homeBase;

    @ColumnInfo(name = "profile_image_uri")
    private String profileImageUri;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public User() {
    }

    public User(String fullName,
                String email,
                String phoneNumber,
                String password,
                String emergencyContact,
                long createdAt) {

        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.emergencyContact = emergencyContact;
        this.createdAt = createdAt;
    }

    public User(String fullName,
                String email,
                String phoneNumber,
                String password,
                String emergencyContact,
                String bloodGroup,
                Integer age,
                String gender,
                String homeBase,
                String profileImageUri,
                long createdAt) {

        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.emergencyContact = emergencyContact;

        this.bloodGroup = bloodGroup;
        this.age = age;
        this.gender = gender;
        this.homeBase = homeBase;
        this.profileImageUri = profileImageUri;

        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getHomeBase() {
        return homeBase;
    }

    public void setHomeBase(String homeBase) {
        this.homeBase = homeBase;
    }

    public String getProfileImageUri() {
        return profileImageUri;
    }

    public void setProfileImageUri(String profileImageUri) {
        this.profileImageUri = profileImageUri;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}