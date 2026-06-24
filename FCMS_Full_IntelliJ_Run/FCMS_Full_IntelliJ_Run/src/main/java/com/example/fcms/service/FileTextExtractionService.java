package com.example.fcms.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class FileTextExtractionService {

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty or missing.");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("Filename is missing.");
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            throw new IllegalArgumentException("Invalid file extension.");
        }
        String ext = fileName.substring(lastDot + 1).toLowerCase();
        
        String extractedText;
        try {
            switch (ext) {
                case "txt":
                    extractedText = extractTxt(file);
                    break;
                case "pdf":
                    extractedText = extractPdf(file);
                    break;
                case "docx":
                    extractedText = extractDocx(file);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported file type: ." + ext + ". Only .txt, .pdf, and .docx are supported.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error reading file content: " + e.getMessage(), e);
        }

        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new IllegalArgumentException("Could not extract readable text from this file.");
        }

        // Limit to 8000 characters
        if (extractedText.length() > 8000) {
            extractedText = extractedText.substring(0, 8000);
        }
        return extractedText;
    }

    public String extractTxt(MultipartFile file) throws IOException {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    public String extractPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public String extractDocx(MultipartFile file) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
