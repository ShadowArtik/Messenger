package com.example.storage;

import com.example.model.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatStorage {

    private static final String FILE_NAME = "chats.json";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void saveChats(Map<String, List<Message>> chats) {

        try (FileWriter writer = new FileWriter(FILE_NAME)) {

            gson.toJson(chats, writer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, List<Message>> loadChats() {

        try (FileReader reader = new FileReader(FILE_NAME)) {

            Type type = new TypeToken<Map<String, List<Message>>>() {}.getType();

            return gson.fromJson(reader, type);

        } catch (Exception e) {

            return new HashMap<>();
        }
    }
}