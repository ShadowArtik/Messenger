package com.example.service;

import com.example.repository.ContactRepository;

import java.util.List;

public class ContactService {

    private final ContactRepository contactRepository = new ContactRepository();

    public void saveContact(String name) {
        contactRepository.saveContact(name);
    }

    public List<String> getAllContacts() {
        return contactRepository.getAllContacts();
    }

    public void deleteContact(String name) {
        contactRepository.deleteContact(name);
    }

    public void renameContact(String oldName, String newName) {
        contactRepository.renameContact(oldName, newName);
    }

    public void saveBotContact(String name) {
        contactRepository.saveBotContact(name);
    }

    public boolean isBotContact(String name) {
        return contactRepository.isBotContact(name);
    }
}