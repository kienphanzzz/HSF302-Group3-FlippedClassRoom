package com.example.fcms.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
        
        // Strict backend extension validation
        if (!ext.equals("txt") && !ext.equals("pdf") && !ext.equals("docx") && !ext.equals("pptx")
                && !ext.equals("zip") && !ext.equals("png") && !ext.equals("jpg") && !ext.equals("jpeg")) {
            throw new IllegalArgumentException("Unsupported file type. Please upload TXT, PDF, DOCX, PPTX, ZIP, PNG, JPG, or JPEG.");
        }
        
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
                case "pptx":
                    extractedText = extractPptx(file);
                    break;
                case "zip":
                    extractedText = extractZip(file);
                    break;
                case "png":
                case "jpg":
                case "jpeg":
                    extractedText = extractImagePlaceholder(file);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported file type. Please upload TXT, PDF, DOCX, PPTX, ZIP, PNG, JPG, or JPEG.");
            }
        } catch (IllegalArgumentException e) {
            // Rethrow validation errors from extractZip/validation
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error reading file content: " + e.getMessage(), e);
        }

        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new IllegalArgumentException("Could not extract readable text from this file.");
        }

        // Limit to 10000 characters
        if (extractedText.length() > 10000) {
            extractedText = extractedText.substring(0, 10000);
        }
        return extractedText;
    }

    public String extractTxt(MultipartFile file) throws IOException {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    public String extractPdf(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            return extractPdfFromStream(is);
        }
    }

    private String extractPdfFromStream(InputStream is) throws IOException {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public String extractDocx(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            return extractDocxFromStream(is);
        }
    }

    private String extractDocxFromStream(InputStream is) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    public String extractPptx(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            return extractPptxFromStream(is);
        }
    }

    private String extractPptxFromStream(InputStream is) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow(is)) {
            StringBuilder sb = new StringBuilder();
            for (XSLFSlide slide : ppt.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        String text = textShape.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            sb.append(text).append("\n");
                        }
                    }
                }
            }
            return sb.toString();
        }
    }

    public String extractZip(MultipartFile file) throws IOException {
        StringBuilder sb = new StringBuilder();
        int filesProcessed = 0;
        java.nio.file.Path tempDir = java.nio.file.Paths.get("temp").toAbsolutePath();

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();

                // 1. Path traversal / Zip Slip protection
                if (name.contains("..")) {
                    continue;
                }
                java.nio.file.Path targetPath = tempDir.resolve(name).normalize();
                if (!targetPath.startsWith(tempDir)) {
                    continue;
                }

                // 2. Identify extension (nested zip/unsupported ignored)
                int lastDot = name.lastIndexOf('.');
                if (lastDot == -1) {
                    continue;
                }
                String ext = name.substring(lastDot + 1).toLowerCase();
                if (!ext.equals("txt") && !ext.equals("pdf") && !ext.equals("docx") && !ext.equals("pptx")) {
                    continue;
                }

                // 3. Process limit: max 10 files
                if (filesProcessed >= 10) {
                    break;
                }

                // 4. Read the zip entry fully into byte[]
                byte[] entryBytes = readEntryFully(zis);
                if (entryBytes.length == 0) {
                    continue;
                }

                // 5. Parse content from ByteArrayInputStream
                try (ByteArrayInputStream bais = new ByteArrayInputStream(entryBytes)) {
                    String extractedText = null;
                    if (ext.equals("txt")) {
                        extractedText = new String(entryBytes, StandardCharsets.UTF_8);
                    } else if (ext.equals("pdf")) {
                        extractedText = extractPdfFromStream(bais);
                    } else if (ext.equals("docx")) {
                        extractedText = extractDocxFromStream(bais);
                    } else if (ext.equals("pptx")) {
                        extractedText = extractPptxFromStream(bais);
                    }

                    if (extractedText != null && !extractedText.trim().isEmpty()) {
                        sb.append("--- File: ").append(name).append(" ---\n");
                        sb.append(extractedText).append("\n\n");
                        filesProcessed++;
                    }
                } catch (Exception e) {
                    // Ignore individual parse failures, keep processing others
                }
            }
        }

        if (filesProcessed == 0) {
            throw new IllegalArgumentException("No supported lesson files found inside this ZIP. Please include TXT, PDF, DOCX, or PPTX files.");
        }

        return sb.toString();
    }

    public String extractImagePlaceholder(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        return "Image uploaded: " + originalFileName + ". OCR is not enabled in this demo. Please paste the image text or use a PDF/DOCX/PPTX file for better question generation.";
    }

    private byte[] readEntryFully(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = zis.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        return bos.toByteArray();
    }
}

