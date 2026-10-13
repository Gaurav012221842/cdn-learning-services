package com.learningplatform.course.search;

import com.learningplatform.common.dto.ApiResponse;
import com.learningplatform.course.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Course>>> searchCourses(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved", searchService.searchCourses(q)));
    }
}
