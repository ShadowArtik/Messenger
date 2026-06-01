package com.example.view;

import com.example.network.ServerApi;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ImageStore {

    // =================== Cache ===================

    private ImageStore() {
    }

    private static final ServerApi API = new ServerApi();
    private static final Map<Integer, Image> CACHE = new ConcurrentHashMap<>();

    public static Image get(int attachmentId) {
        Image cached = CACHE.get(attachmentId);
        if (cached != null) {
            return cached;
        }

        byte[] data = API.downloadFile(attachmentId);
        if (data == null || data.length == 0) {
            return null;
        }

        Image image = new Image(new ByteArrayInputStream(data));
        CACHE.put(attachmentId, image);
        return image;
    }
}
