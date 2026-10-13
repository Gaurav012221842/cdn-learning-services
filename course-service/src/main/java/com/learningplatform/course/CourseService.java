package com.learningplatform.course;

import com.learningplatform.course.dto.ChapterDTO;
import com.learningplatform.course.dto.CourseResponse;
import com.learningplatform.course.dto.CreateCourseRequest;
import com.learningplatform.course.dto.LessonDTO;
import com.learningplatform.course.dto.UpdateCourseRequest;
import com.learningplatform.course.lesson.Lesson;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final JdbcTemplate jdbcTemplate;

    @Cacheable(value = "courses_all", key = "'all'")
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream()
                .filter(course -> !"DELETED".equalsIgnoreCase(course.getStatus()))
                .map(courseMapper::toResponse)
                .collect(Collectors.toList());
    }

    public CourseResponse getCourseById(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found for ID: " + id));

        if ("DELETED".equalsIgnoreCase(course.getStatus())) {
            throw new RuntimeException("Course not found for ID: " + id);
        }

        return courseMapper.toResponse(course);
    }

    public CourseResponse getCourseById(UUID id, UUID studentId, String email, boolean isAdmin) {
        return getCourseById(id);
    }

    @CacheEvict(value = {"courses_all", "course_details"}, allEntries = true)
    public CourseResponse createCourse(CreateCourseRequest request) {
        UUID courseId = UUID.randomUUID();
        List<Chapter> chapterEntities = new ArrayList<>();

        if (request.getChapters() != null) {
            int chapSeq = 1;
            for (ChapterDTO cDto : request.getChapters()) {
                UUID chapterId = UUID.randomUUID();
                List<Lesson> lessonEntities = new ArrayList<>();

                if (cDto.getLessons() != null) {
                    int lesSeq = 1;
                    for (LessonDTO lDto : cDto.getLessons()) {
                        Lesson lesson = Lesson.builder()
                                .id(UUID.randomUUID())
                                .courseId(courseId)
                                .chapterId(chapterId)
                                .title(lDto.getTitle() != null ? lDto.getTitle() : "Lesson " + lesSeq)
                                .description(lDto.getDescription())
                                .lessonType(lDto.getLessonType() != null ? lDto.getLessonType() : "VIDEO")
                                .contentUrl(lDto.getContentUrl())
                                .videoThumbnailUrl(lDto.getVideoThumbnailUrl())
                                .quizData(lDto.getQuizData())
                                .sequenceOrder(lDto.getSequenceOrder() != null ? lDto.getSequenceOrder() : lesSeq++)
                                .build();
                        lessonEntities.add(lesson);
                    }
                }

                Chapter chapter = Chapter.builder()
                        .id(chapterId)
                        .courseId(courseId)
                        .title(cDto.getTitle() != null ? cDto.getTitle() : "Chapter " + chapSeq)
                        .sequenceOrder(cDto.getSequenceOrder() != null ? cDto.getSequenceOrder() : chapSeq++)
                        .lessons(lessonEntities)
                        .build();

                chapterEntities.add(chapter);
            }
        }

        Course course = Course.builder()
                .id(courseId)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory() != null ? request.getCategory() : "Full Stack")
                .thumbnailUrl(request.getThumbnailUrl())
                .instructorId(request.getInstructorId())
                .price(request.getPrice())
                .status("PUBLISHED")
                .chapters(chapterEntities)
                .build();

        return courseMapper.toResponse(courseRepository.save(course));
    }

    @CacheEvict(value = {"courses_all", "course_details"}, allEntries = true)
    public CourseResponse updateCourse(UUID id, UpdateCourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        if (request.getTitle() != null) course.setTitle(request.getTitle());
        if (request.getDescription() != null) course.setDescription(request.getDescription());
        if (request.getCategory() != null) course.setCategory(request.getCategory());
        if (request.getPrice() != null) course.setPrice(request.getPrice());
        if (request.getStatus() != null) course.setStatus(request.getStatus());
        if (request.getThumbnailUrl() != null) course.setThumbnailUrl(request.getThumbnailUrl());

        if (request.getChapters() != null) {
            course.getChapters().clear();
            int chapSeq = 1;
            for (ChapterDTO cDto : request.getChapters()) {
                UUID chapterId = cDto.getId() != null ? cDto.getId() : UUID.randomUUID();
                List<Lesson> lessonEntities = new ArrayList<>();

                if (cDto.getLessons() != null) {
                    int lesSeq = 1;
                    for (LessonDTO lDto : cDto.getLessons()) {
                        Lesson lesson = Lesson.builder()
                                .id(lDto.getId() != null ? lDto.getId() : UUID.randomUUID())
                                .courseId(id)
                                .chapterId(chapterId)
                                .title(lDto.getTitle() != null ? lDto.getTitle() : "Lesson " + lesSeq)
                                .description(lDto.getDescription())
                                .lessonType(lDto.getLessonType() != null ? lDto.getLessonType() : "VIDEO")
                                .contentUrl(lDto.getContentUrl())
                                .videoThumbnailUrl(lDto.getVideoThumbnailUrl())
                                .quizData(lDto.getQuizData())
                                .sequenceOrder(lDto.getSequenceOrder() != null ? lDto.getSequenceOrder() : lesSeq++)
                                .build();
                        lessonEntities.add(lesson);
                    }
                }

                Chapter chapter = Chapter.builder()
                        .id(chapterId)
                        .courseId(id)
                        .title(cDto.getTitle() != null ? cDto.getTitle() : "Chapter " + chapSeq)
                        .sequenceOrder(cDto.getSequenceOrder() != null ? cDto.getSequenceOrder() : chapSeq++)
                        .lessons(lessonEntities)
                        .build();

                course.getChapters().add(chapter);
            }
        }

        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Transactional
    @CacheEvict(value = {"courses_all", "course_details", "course_lessons", "course_pricing", "wishlist_check"}, allEntries = true)
    public void deleteCourse(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if ("DELETED".equalsIgnoreCase(course.getStatus())) {
            return;
        }

        course.setStatus("DELETED");
        courseRepository.save(course);
    }

    public com.learningplatform.course.dto.QuizEvaluationResponse evaluateQuiz(UUID courseId, com.learningplatform.course.dto.QuizSubmissionRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Lesson quizLesson = null;
        if (request != null && request.getLessonId() != null) {
            if (course.getChapters() != null) {
                for (Chapter chap : course.getChapters()) {
                    if (chap.getLessons() != null) {
                        for (Lesson l : chap.getLessons()) {
                            if (request.getLessonId().equals(l.getId())) {
                                quizLesson = l;
                                break;
                            }
                        }
                    }
                    if (quizLesson != null) break;
                }
            }
        }

        // Fallback: search for first lesson with quizData
        if (quizLesson == null && course.getChapters() != null) {
            for (Chapter chap : course.getChapters()) {
                if (chap.getLessons() != null) {
                    for (Lesson l : chap.getLessons()) {
                        if (l.getQuizData() != null && !l.getQuizData().trim().isEmpty()) {
                            quizLesson = l;
                            break;
                        }
                    }
                }
                if (quizLesson != null) break;
            }
        }

        if (quizLesson == null || quizLesson.getQuizData() == null || quizLesson.getQuizData().trim().isEmpty()) {
            throw new RuntimeException("No quiz data found for this lesson or course.");
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(quizLesson.getQuizData());

            String title = rootNode.has("title") ? rootNode.get("title").asText() : "Course Quiz";
            int passingScore = rootNode.has("passingScore") ? rootNode.get("passingScore").asInt() : 70;

            List<com.learningplatform.course.dto.QuizEvaluationResponse.QuestionResult> results = new ArrayList<>();
            int correctCount = 0;

            if (rootNode.has("questions") && rootNode.get("questions").isArray()) {
                int qIdx = 0;
                for (com.fasterxml.jackson.databind.JsonNode qNode : rootNode.get("questions")) {
                    String question = qNode.has("question") ? qNode.get("question").asText() : "";
                    List<String> options = new ArrayList<>();
                    if (qNode.has("options") && qNode.get("options").isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode optNode : qNode.get("options")) {
                            options.add(optNode.asText());
                        }
                    }
                    int correctIndex = qNode.has("correctIndex") ? qNode.get("correctIndex").asInt() : 0;
                    String explanation = qNode.has("explanation") ? qNode.get("explanation").asText() : "";

                    // Find student answer
                    Integer selectedOption = null;
                    if (request != null && request.getAnswers() != null) {
                        if (request.getAnswers().containsKey(String.valueOf(qIdx))) {
                            selectedOption = request.getAnswers().get(String.valueOf(qIdx));
                        }
                    }

                    boolean isCorrect = (selectedOption != null && selectedOption == correctIndex);
                    if (isCorrect) {
                        correctCount++;
                    }

                    results.add(com.learningplatform.course.dto.QuizEvaluationResponse.QuestionResult.builder()
                            .questionIndex(qIdx)
                            .question(question)
                            .options(options)
                            .selectedOption(selectedOption)
                            .correctIndex(correctIndex)
                            .isCorrect(isCorrect)
                            .explanation(explanation)
                            .build());

                    qIdx++;
                }
            }

            int totalQuestions = results.size();
            int scorePercentage = totalQuestions > 0 ? (int) Math.round(((double) correctCount / totalQuestions) * 100) : 0;
            int strikes = request != null && request.getStrikes() != null ? request.getStrikes() : 0;
            boolean isDisqualified = request != null && Boolean.TRUE.equals(request.getIsDisqualified());
            boolean isPassed = !isDisqualified && scorePercentage >= passingScore;

            return com.learningplatform.course.dto.QuizEvaluationResponse.builder()
                    .title(title)
                    .totalQuestions(totalQuestions)
                    .correctCount(correctCount)
                    .scorePercentage(scorePercentage)
                    .passingScore(passingScore)
                    .strikes(strikes)
                    .isDisqualified(isDisqualified)
                    .isPassed(isPassed)
                    .results(results)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate quiz: " + e.getMessage(), e);
        }
    }
}
