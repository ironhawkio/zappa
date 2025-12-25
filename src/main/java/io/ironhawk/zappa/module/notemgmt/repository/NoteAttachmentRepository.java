package io.ironhawk.zappa.module.notemgmt.repository;

import io.ironhawk.zappa.module.notemgmt.entity.Note;
import io.ironhawk.zappa.module.notemgmt.entity.NoteAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteAttachmentRepository extends JpaRepository<NoteAttachment, UUID> {

    /**
     * Find all attachments for a specific note
     */
    List<NoteAttachment> findByNoteOrderByUploadedAtDesc(Note note);

    /**
     * Find all attachments for a specific note by note ID
     */
    @Query("SELECT na FROM NoteAttachment na WHERE na.note.id = :noteId ORDER BY na.uploadedAt DESC")
    List<NoteAttachment> findByNoteIdOrderByUploadedAtDesc(@Param("noteId") UUID noteId);

    /**
     * Find attachment by filename
     */
    Optional<NoteAttachment> findByFilename(String filename);

    /**
     * Find attachments by MIME type
     */
    List<NoteAttachment> findByMimeTypeOrderByUploadedAtDesc(String mimeType);

    /**
     * Find Word documents
     */
    @Query("SELECT na FROM NoteAttachment na WHERE na.mimeType IN ('application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/msword') ORDER BY na.uploadedAt DESC")
    List<NoteAttachment> findWordDocumentsOrderByUploadedAtDesc();

    /**
     * Find PDF documents
     */
    @Query("SELECT na FROM NoteAttachment na WHERE na.mimeType = 'application/pdf' ORDER BY na.uploadedAt DESC")
    List<NoteAttachment> findPdfDocumentsOrderByUploadedAtDesc();

    /**
     * Find attachments larger than specified size
     */
    @Query("SELECT na FROM NoteAttachment na WHERE na.fileSize > :size ORDER BY na.fileSize DESC")
    List<NoteAttachment> findByFileSizeGreaterThanOrderByFileSizeDesc(@Param("size") Long size);

    /**
     * Count attachments for a specific note
     */
    @Query("SELECT COUNT(na) FROM NoteAttachment na WHERE na.note.id = :noteId")
    Long countByNoteId(@Param("noteId") UUID noteId);

    /**
     * Get total file size for a specific note
     */
    @Query("SELECT COALESCE(SUM(na.fileSize), 0) FROM NoteAttachment na WHERE na.note.id = :noteId")
    Long getTotalFileSizeByNoteId(@Param("noteId") UUID noteId);

    /**
     * Find attachments by original filename containing text (case insensitive)
     */
    @Query("SELECT na FROM NoteAttachment na WHERE LOWER(na.originalFilename) LIKE LOWER(CONCAT('%', :filename, '%')) ORDER BY na.uploadedAt DESC")
    List<NoteAttachment> findByOriginalFilenameContainingIgnoreCaseOrderByUploadedAtDesc(@Param("filename") String filename);

    /**
     * Delete all attachments for a specific note
     */
    void deleteByNote(Note note);

    /**
     * Check if attachment exists by stored filename
     */
    boolean existsByFilename(String filename);
}