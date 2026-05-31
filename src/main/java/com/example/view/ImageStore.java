package com.example.view;

import com.example.network.ServerApi;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Downloads and caches chat image attachments by id, so the message list does
 * not re-fetch the same image on every cell re-render.
 */
public final class ImageStore {

    private ImageStore() {
    }

    private static final ServerApi API = new ServerApi();
    private static final Map<Integer, Image> CACHE = new ConcurrentHashMap<>();

    /** @return the cached image for this attachment id, or {@code null} if it cannot be loaded. */
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
