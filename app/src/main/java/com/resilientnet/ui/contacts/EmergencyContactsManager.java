package com.resilientnet.ui.contacts;

import android.content.Context;
import android.content.SharedPreferences;

import com.resilientnet.ui.contacts.model.EmergencyContact;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class EmergencyContactsManager {

    private static final String PREF_NAME = "resilientnet_emergency_contacts";
    private static final String KEY_PERSONAL_CONTACTS = "personal_contacts_json";

    private final SharedPreferences preferences;
    private final List<EmergencyContact> preloadedContacts;

    public EmergencyContactsManager(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.preloadedContacts = initPreloadedContacts();
    }

    private List<EmergencyContact> initPreloadedContacts() {
        List<EmergencyContact> list = new ArrayList<>();
        list.add(new EmergencyContact(
                "pre_112",
                "National Emergency Helpline",
                "112",
                "National Helpline",
                "Unified Emergency Services (Police, Medical, Fire, Disaster)",
                true
        ));
        list.add(new EmergencyContact(
                "pre_108",
                "Disaster Medical & Ambulance",
                "108",
                "Medical Helpline",
                "Emergency Medical Assistance & Rapid Evacuation",
                true
        ));
        list.add(new EmergencyContact(
                "pre_101",
                "Fire & Rescue Services",
                "101",
                "Rescue Helpline",
                "Disaster Rescue, Fire Suppression & Evacuation",
                true
        ));
        list.add(new EmergencyContact(
                "pre_100",
                "Police Emergency",
                "100",
                "Security Helpline",
                "Law Enforcement, Search & Security Operations",
                true
        ));
        list.add(new EmergencyContact(
                "pre_1070",
                "State Disaster Management (SDMA)",
                "1070",
                "Disaster Operations",
                "Central Disaster Operations & Relief Coordination Centre",
                true
        ));
        list.add(new EmergencyContact(
                "pre_1078",
                "National Disaster Authority (NDMA)",
                "1078",
                "Disaster Operations",
                "National Disaster Crisis & Warning Operations",
                true
        ));
        list.add(new EmergencyContact(
                "pre_ndrf",
                "NDRF Control Room HQ",
                "01124363260",
                "Specialized Rescue",
                "National Disaster Response Force HQ Operations",
                true
        ));
        return Collections.unmodifiableList(list);
    }

    public List<EmergencyContact> getPreloadedContacts() {
        return preloadedContacts;
    }

    public List<EmergencyContact> getPersonalContacts() {
        List<EmergencyContact> list = new ArrayList<>();
        String json = preferences.getString(KEY_PERSONAL_CONTACTS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new EmergencyContact(
                        obj.optString("id", UUID.randomUUID().toString()),
                        obj.optString("name", "Unknown Contact"),
                        obj.optString("phoneNumber", ""),
                        obj.optString("category", "Personal Contact"),
                        obj.optString("description", "Emergency Family / Local Volunteer"),
                        false
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<EmergencyContact> getAllContacts() {
        List<EmergencyContact> all = new ArrayList<>();
        all.addAll(getPersonalContacts());
        all.addAll(preloadedContacts);
        return all;
    }

    public void addPersonalContact(String name, String phone, String category, String description) {
        List<EmergencyContact> existing = getPersonalContacts();
        existing.add(0, new EmergencyContact(
                UUID.randomUUID().toString(),
                name,
                phone,
                category != null && !category.isEmpty() ? category : "Personal Contact",
                description != null && !description.isEmpty() ? description : "Emergency Contact",
                false
        ));
        savePersonalContacts(existing);
    }

    public void deletePersonalContact(String contactId) {
        List<EmergencyContact> existing = getPersonalContacts();
        for (int i = 0; i < existing.size(); i++) {
            if (existing.get(i).getId().equals(contactId)) {
                existing.remove(i);
                break;
            }
        }
        savePersonalContacts(existing);
    }

    private void savePersonalContacts(List<EmergencyContact> contacts) {
        JSONArray array = new JSONArray();
        for (EmergencyContact contact : contacts) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", contact.getId());
                obj.put("name", contact.getName());
                obj.put("phoneNumber", contact.getPhoneNumber());
                obj.put("category", contact.getCategory());
                obj.put("description", contact.getDescription());
                array.put(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        preferences.edit().putString(KEY_PERSONAL_CONTACTS, array.toString()).apply();
    }
}
