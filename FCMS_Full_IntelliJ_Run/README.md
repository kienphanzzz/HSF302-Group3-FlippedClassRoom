# FCMS Full Spring Boot Thymeleaf IntelliJ Project

Project này đã gộp 2 bộ UI Figma export thành một Spring Boot MVC + Thymeleaf project chạy được trong IntelliJ.

## Màn đã có

- Login: `/login`
- Register: `/register`
- Teacher Dashboard: `/teacher/dashboard`
- Student Learning Path: `/student/learning-path`
- AI Assistant: `/student/ai-assistant`
- Assignment Submission: `/student/assignment`

## Cách chạy

1. Giải nén zip.
2. IntelliJ IDEA → File → Open.
3. Chọn đúng thư mục `FCMS_Full_IntelliJ_Run`.
4. Đợi Maven load dependency.
5. Mở `src/main/java/com/example/fcms/FcmsApplication.java`.
6. Bấm nút tam giác xanh cạnh `main()`.
7. Mở trình duyệt: `http://localhost:8080`.

## Ghi chú

- Chưa cần MySQL, chưa cần login thật. Form login/register chỉ redirect demo theo role.
- Sau này có database thì giữ nguyên templates, thêm Entity/Repository/Service/Controller xử lý thật.
