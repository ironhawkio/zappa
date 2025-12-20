package io.ironhawk.zappa.module.notemgmt.controller;

import io.ironhawk.zappa.module.notemgmt.entity.Note;
import io.ironhawk.zappa.module.notemgmt.entity.NoteAttachment;
import io.ironhawk.zappa.module.notemgmt.service.FileStorageService;
import io.ironhawk.zappa.module.notemgmt.service.NoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.UUID;

@Controller
@RequestMapping("/attachments")
public class FileAttachmentController {

    private static final Logger logger = LoggerFactory.getLogger(FileAttachmentController.class);

    private final FileStorageService fileStorageService;
    private final NoteService noteService;

    public FileAttachmentController(FileStorageService fileStorageService, NoteService noteService) {
        this.fileStorageService = fileStorageService;
        this.noteService = noteService;
    }

    /**
     * Upload file attachment to a note
     */
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                           @RequestParam("noteId") UUID noteId,
                           RedirectAttributes redirectAttributes) {
        try {
            // Validate note exists
            Note note = noteService.getNoteById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found with id: " + noteId));

            // Store file
            NoteAttachment attachment = fileStorageService.storeFile(file, note);

            redirectAttributes.addFlashAttribute("successMessage",
                "File uploaded successfully: " + attachment.getOriginalFilename());

            logger.info("File uploaded successfully for note {}: {}", noteId, attachment.getOriginalFilename());

        } catch (Exception ex) {
            logger.error("Failed to upload file for note {}: {}", noteId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                "Failed to upload file: " + ex.getMessage());
        }

        return "redirect:/notes/" + noteId;
    }

    /**
     * Download file attachment
     */
    @GetMapping("/download/{attachmentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID attachmentId) {
        try {
            // Get attachment record
            NoteAttachment attachment = fileStorageService.getAttachmentById(attachmentId);

            // Load file as resource
            Resource resource = fileStorageService.loadFileAsResource(attachment.getFilename());

            // Determine content type
            String contentType = attachment.getMimeType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + attachment.getOriginalFilename() + "\"")
                .body(resource);

        } catch (Exception ex) {
            logger.error("Failed to download file with attachment id {}: {}", attachmentId, ex.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * View file inline (for images, PDFs, etc.)
     */
    @GetMapping("/view/{attachmentId}")
    public ResponseEntity<Resource> viewFile(@PathVariable UUID attachmentId) {
        try {
            // Get attachment record
            NoteAttachment attachment = fileStorageService.getAttachmentById(attachmentId);

            // Load file as resource
            Resource resource = fileStorageService.loadFileAsResource(attachment.getFilename());

            // Determine content type
            String contentType = attachment.getMimeType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + attachment.getOriginalFilename() + "\"")
                .body(resource);

        } catch (Exception ex) {
            logger.error("Failed to view file with attachment id {}: {}", attachmentId, ex.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete file attachment
     */
    @PostMapping("/delete/{attachmentId}")
    public String deleteFile(@PathVariable UUID attachmentId,
                           @RequestParam("noteId") UUID noteId,
                           RedirectAttributes redirectAttributes) {
        try {
            // Get attachment record
            NoteAttachment attachment = fileStorageService.getAttachmentById(attachmentId);

            // Verify attachment belongs to the specified note
            if (!attachment.getNote().getId().equals(noteId)) {
                throw new RuntimeException("Attachment does not belong to the specified note");
            }

            // Delete file
            fileStorageService.deleteFile(attachment);

            redirectAttributes.addFlashAttribute("successMessage",
                "File deleted successfully: " + attachment.getOriginalFilename());

            logger.info("File deleted successfully: {}", attachment.getOriginalFilename());

        } catch (Exception ex) {
            logger.error("Failed to delete attachment {}: {}", attachmentId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                "Failed to delete file: " + ex.getMessage());
        }

        return "redirect:/notes/" + noteId;
    }

    /**
     * API endpoint to get attachment info
     */
    @GetMapping("/info/{attachmentId}")
    @ResponseBody
    public ResponseEntity<AttachmentInfo> getAttachmentInfo(@PathVariable UUID attachmentId) {
        try {
            NoteAttachment attachment = fileStorageService.getAttachmentById(attachmentId);

            AttachmentInfo info = new AttachmentInfo(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getFormattedFileSize(),
                attachment.getMimeType(),
                attachment.getFileExtension(),
                attachment.getUploadedAt().toString()
            );

            return ResponseEntity.ok(info);

        } catch (Exception ex) {
            logger.error("Failed to get attachment info for {}: {}", attachmentId, ex.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // DTO for attachment information
    public static class AttachmentInfo {
        private UUID id;
        private String originalFilename;
        private String formattedFileSize;
        private String mimeType;
        private String fileExtension;
        private String uploadedAt;

        public AttachmentInfo(UUID id, String originalFilename, String formattedFileSize,
                            String mimeType, String fileExtension, String uploadedAt) {
            this.id = id;
            this.originalFilename = originalFilename;
            this.formattedFileSize = formattedFileSize;
            this.mimeType = mimeType;
            this.fileExtension = fileExtension;
            this.uploadedAt = uploadedAt;
        }

        // Getters and setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getOriginalFilename() { return originalFilename; }
        public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

        public String getFormattedFileSize() { return formattedFileSize; }
        public void setFormattedFileSize(String formattedFileSize) { this.formattedFileSize = formattedFileSize; }

        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }

        public String getFileExtension() { return fileExtension; }
        public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

        public String getUploadedAt() { return uploadedAt; }
        public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }
    }
}