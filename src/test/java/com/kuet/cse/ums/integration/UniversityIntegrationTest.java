package com.kuet.cse.ums.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuet.cse.ums.dto.CourseDto;
import com.kuet.cse.ums.dto.EnrollmentDto;
import com.kuet.cse.ums.dto.LoginRequestDto;
import com.kuet.cse.ums.dto.RegisterRequestDto;
import com.kuet.cse.ums.dto.ResultDto;
import com.kuet.cse.ums.entity.Course;
import com.kuet.cse.ums.entity.Role;
import com.kuet.cse.ums.entity.Student;
import com.kuet.cse.ums.repository.CourseRepository;
import com.kuet.cse.ums.repository.EnrollmentRepository;
import com.kuet.cse.ums.repository.StudentRepository;
import com.kuet.cse.ums.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test Suite for University Management System.
 * Contains exactly 3 end-to-end Integration Tests using Spring Boot Test, MockMvc, and in-memory H2 DB.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class UniversityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // =========================================================================
    // INTEGRATION TEST 1: User Registration, Authentication & Role-Based Access
    // =========================================================================
    @Test
    @DisplayName("Integration Test 1: Full registration, login, and access control lifecycle")
    void testUserRegistrationAndAccessControlLifecycle() throws Exception {
        // Step 1: Register new student user
        RegisterRequestDto regRequest = new RegisterRequestDto();
        regRequest.setUsername("integ_student");
        regRequest.setPassword("securePassword123");
        regRequest.setFullName("Integration Student Test");
        regRequest.setEmail("integ_student@kuet.ac.bd");
        regRequest.setRole(Role.ROLE_STUDENT);
        regRequest.setStudentRoll("2007088");
        regRequest.setDepartment("Computer Science & Engineering");
        regRequest.setSeries(2022);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("integ_student"))
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"));

        // Verify entity persisted in repository
        assertTrue(userRepository.existsByUsername("integ_student"));
        assertTrue(studentRepository.existsByStudentRoll("2007088"));

        // Step 2: Authenticate via login endpoint
        LoginRequestDto loginRequest = new LoginRequestDto("integ_student", "securePassword123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integ_student"))
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"));

        // Step 3: Unauthenticated request to protected endpoint should be blocked with 401
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        // Step 4: Login with bad credentials should return 401 Unauthorized
        LoginRequestDto badLogin = new LoginRequestDto("integ_student", "wrong_password");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());

        // Step 5: Student attempting to access admin-only endpoint receives 403 Forbidden
        mockMvc.perform(get("/api/users").with(user("integ_student").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // INTEGRATION TEST 2: Course Creation, Catalog Query, and Validation Flow
    // =========================================================================
    @Test
    @DisplayName("Integration Test 2: Course creation by Admin, retrieval in catalog, and duplicate rejection")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCourseCreationAndCatalogRetrievalLifecycle() throws Exception {
        // Step 1: Admin creates a new course
        CourseDto newCourse = new CourseDto();
        newCourse.setCourseCode("CSE 4205");
        newCourse.setCourseTitle("Distributed Systems & Cloud Computing");
        newCourse.setCredits(3.0);
        newCourse.setDescription("Consensus algorithms, microservices, and cloud architectures.");

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCourse)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.courseCode").value("CSE 4205"))
                .andExpect(jsonPath("$.credits").value(3.0));

        // Verify persisted in database
        assertTrue(courseRepository.existsByCourseCode("CSE 4205"));

        // Step 2: Retrieve course catalog and verify newly created course is present
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.courseCode == 'CSE 4205')].courseTitle").value(hasItem("Distributed Systems & Cloud Computing")));

        // Step 3: Attempting to create duplicate course with same code should return 400 Bad Request
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCourse)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    // =========================================================================
    // INTEGRATION TEST 3: Enrollment, Result Submission, and Grade Query Flow
    // =========================================================================
    @Test
    @DisplayName("Integration Test 3: End-to-end student enrollment, grading computation, and result verification")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testEnrollmentAndGradingLifecycle() throws Exception {
        // Fetch demo student and course
        Student student = studentRepository.findByUserUsername("student_tamim")
                .orElseThrow(() -> new IllegalStateException("student_tamim not found"));
        Course course = courseRepository.findByCourseCode("CSE 3200")
                .orElseThrow(() -> new IllegalStateException("CSE 3200 not found"));

        // Step 1: Admin enrolls student into course for Spring 2026 semester
        EnrollmentDto enrollDto = new EnrollmentDto();
        enrollDto.setStudentId(student.getId());
        enrollDto.setCourseId(course.getId());
        enrollDto.setSemester("Spring 2026");

        MvcResult enrollResult = mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.semester").value("Spring 2026"))
                .andReturn();

        JsonNode enrollNode = objectMapper.readTree(enrollResult.getResponse().getContentAsString());
        Long createdEnrollmentId = enrollNode.get("id").asLong();

        // Step 2: Submit grade for the created enrollment
        ResultDto resultDto = new ResultDto();
        resultDto.setEnrollmentId(createdEnrollmentId);
        resultDto.setMarks(88.5);
        resultDto.setRemarks("Excellent semester final and lab performance");

        mockMvc.perform(post("/api/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resultDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marks").value(88.5))
                .andExpect(jsonPath("$.letterGrade").value("A+"))
                .andExpect(jsonPath("$.gradePoint").value(4.0))
                .andExpect(jsonPath("$.remarks").value("Excellent semester final and lab performance"));

        // Step 3: Retrieve results for the course and verify graded record exists
        mockMvc.perform(get("/api/results/course/" + course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.enrollmentId == " + createdEnrollmentId + ")].letterGrade").value(hasItem("A+")))
                .andExpect(jsonPath("$[?(@.enrollmentId == " + createdEnrollmentId + ")].gradePoint").value(hasItem(4.0)));
    }
}
