package com.example.demo.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

/**
 * Service to handle all interactions with AWS S3.
 * It encapsulates the logic for uploading, deleting, and generating file URLs.
 */
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    /**
     * Uploads a file to a specified S3 folder and returns the public URL.
     *
     * @param file The file to upload.
     * @param folderName The S3 folder where the file will be stored.
     * @return The public URL of the uploaded file.
     * @throws IOException if there is an error reading the file.
     * @throws S3Exception if there is an S3-related error.
     */
    public String uploadFile(MultipartFile file, String folderName) throws IOException, S3Exception {
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID() + fileExtension;
        String s3Key = folderName + "/" + newFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(s3Key)).toString();
    }

    /**
     * Deletes a file from S3 using its full URL.
     *
     * @param fileUrl The full URL of the file to delete.
     * @return true if deletion was successful, false otherwise.
     */
    public boolean deleteFile(String fileUrl) {
        try {
            String key = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key("profile-pictures/" + key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            return true;
        } catch (S3Exception e) {
            System.err.println("Failed to delete file from S3: " + e.getMessage());
            return false;
        }
    }
}
