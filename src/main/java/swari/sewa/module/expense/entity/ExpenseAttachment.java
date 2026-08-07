package swari.sewa.module.expense.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "expense_attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "file_type", nullable = false)
    private String fileType;
    
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
    
    // Manual getters for basic fields
    public Long getId() {
        return id;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}
