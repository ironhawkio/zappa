package io.ironhawk.zappa.module.notemgmt.service;

import io.ironhawk.zappa.module.notemgmt.entity.Note;
import io.ironhawk.zappa.module.notemgmt.entity.NoteAttachment;
import io.ironhawk.zappa.module.notemgmt.repository.NoteAttachmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadPath;
    private final NoteAttachmentRepository attachmentRepository;
    private final Environment environment;

    // Allowed file types
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
        "application/msword", // .doc
        "application/pdf", // .pdf
        "image/jpeg", // .jpg, .jpeg
        "image/png", // .png
        "image/gif", // .gif
        "image/webp", // .webp
        "text/plain", // .txt
        "text/markdown" // .md
    );

    // Maximum file size: 50MB
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir,
                             NoteAttachmentRepository attachmentRepository,
                             Environment environment) {
        this.attachmentRepository = attachmentRepository;
        this.environment = environment;

        // Get active profile for environment-specific directory
        String[] activeProfiles = environment.getActiveProfiles();
        String envName = activeProfiles.length > 0 ? activeProfiles[0] : "default";

        this.uploadPath = Paths.get(uploadDir, envName, "note-attachments").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.uploadPath);
            logger.info("Upload directory created/verified: {}", this.uploadPath);
        } catch (IOException ex) {
            logger.error("Could not create upload directory: {}", this.uploadPath, ex);
            throw new RuntimeException("Could not create upload directory!", ex);
        }
    }

    /**
     * Store a file and create attachment record
     */
    public NoteAttachment storeFile(MultipartFile file, Note note) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String storedFilename = generateUniqueFilename(originalFilename);

        try {
            // Check for path traversal attack
            if (originalFilename.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence: " + originalFilename);
            }

            Path targetLocation = this.uploadPath.resolve(storedFilename);

            // Copy file to target location
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // Create attachment record
            NoteAttachment attachment = new NoteAttachment(
                note,
                originalFilename,
                storedFilename,
                targetLocation.toString(),
                file.getSize(),
                file.getContentType()
            );

            NoteAttachment savedAttachment = attachmentRepository.save(attachment);
            logger.info("File stored successfully: {} -> {}", originalFilename, storedFilename);

            return savedAttachment;

        } catch (IOException ex) {
            logger.error("Failed to store file: {}", originalFilename, ex);
            throw new RuntimeException("Failed to store file: " + originalFilename, ex);
        }
    }

    /**
     * Load file as resource
     */
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.uploadPath.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + filename);
            }
        } catch (MalformedURLException ex) {
            logger.error("Malformed URL for file: {}", filename, ex);
            throw new RuntimeException("File not found: " + filename, ex);
        }
    }

    /**
     * Delete file from filesystem and database
     */
    public void deleteFile(NoteAttachment attachment) {
        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Files.deleteIfExists(filePath);

            attachmentRepository.delete(attachment);
            logger.info("File deleted successfully: {}", attachment.getFilename());

        } catch (IOException ex) {
            logger.error("Failed to delete file: {}", attachment.getFilename(), ex);
            throw new RuntimeException("Failed to delete file: " + attachment.getFilename(), ex);
        }
    }

    /**
     * Get all attachments for a note
     */
    public List<NoteAttachment> getAttachmentsByNote(Note note) {
        return attachmentRepository.findByNoteOrderByUploadedAtDesc(note);
    }

    /**
     * Get attachment by ID
     */
    public NoteAttachment getAttachmentById(UUID id) {
        return attachmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + id));
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new RuntimeException("Cannot store empty file");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds maximum allowed size of " +
                (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new RuntimeException("File type not allowed: " + contentType +
                ". Allowed types: " + String.join(", ", ALLOWED_MIME_TYPES));
        }
    }

    /**
     * Generate unique filename to avoid conflicts
     */
    private String generateUniqueFilename(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
            originalFilename = originalFilename.substring(0, lastDotIndex);
        }

        // Sanitize filename
        String sanitizedName = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

        return String.format("%s_%s_%s%s", timestamp, uuid, sanitizedName, extension);
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String filename) {
        Path filePath = this.uploadPath.resolve(filename);
        return Files.exists(filePath);
    }

    /**
     * Get upload directory path
     */
    public Path getUploadPath() {
        return uploadPath;
    }
}