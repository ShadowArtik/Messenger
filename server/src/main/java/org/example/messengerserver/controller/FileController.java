package org.example.messengerserver.controller;

import org.example.messengerserver.repository.AttachmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
public class FileController {

    // =================== Endpoints ===================

    private final AttachmentRepository attachmentRepository;

    public FileController(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    @PostMapping
    public int upload(
            @RequestBody byte[] data,
            @RequestHeader(value = "X-Content-Type", required = false) String contentType
    ) {
        return attachmentRepository.save(null, contentType, data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(@PathVariable int id) {
        AttachmentRepository.Attachment attachment = attachmentRepository.get(id);

        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = attachment.contentType() != null
                ? attachment.contentType()
                : "application/octet-stream";

        return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .body(attachment.data());
    }
}
