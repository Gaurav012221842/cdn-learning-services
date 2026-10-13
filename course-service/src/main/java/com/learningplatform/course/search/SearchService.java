package com.learningplatform.course.search;

import com.learningplatform.course.Course;
import com.learningplatform.course.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final CourseRepository courseRepository;

    public List<Course> searchCourses(String query) {
        String lowerQuery = query.toLowerCase();
        return courseRepository.findAll().stream()
                .filter(course -> course.getTitle().toLowerCase().contains(lowerQuery)
                        || (course.getDescription() != null && course.getDescription().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());
    }
}
