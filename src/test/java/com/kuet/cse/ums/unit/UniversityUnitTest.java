package com.kuet.cse.ums.unit;

import com.kuet.cse.ums.dto.CourseDto;
import com.kuet.cse.ums.dto.EnrollmentDto;
import com.kuet.cse.ums.dto.ResultDto;
import com.kuet.cse.ums.entity.*;
import com.kuet.cse.ums.exception.BadRequestException;
import com.kuet.cse.ums.exception.UnauthorizedException;
import com.kuet.cse.ums.repository.CourseRepository;
import com.kuet.cse.ums.repository.EnrollmentRepository;
import com.kuet.cse.ums.repository.ResultRepository;
import com.kuet.cse.ums.repository.StudentRepository;
import com.kuet.cse.ums.repository.TeacherRepository;
import com.kuet.cse.ums.service.CourseService;
import com.kuet.cse.ums.service.EnrollmentService;
import com.kuet.cse.ums.service.ResultService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test Suite for University Management System.
 * Contains exactly 6 isolated Unit Tests using Mockito (no Spring context, no database).
 */
@ExtendWith(MockitoExtension.class)
public class UniversityUnitTest {

    // --- Mocks for CourseService ---
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @InjectMocks
    private CourseService courseService;

    // --- Mocks for EnrollmentService ---
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @InjectMocks
    private EnrollmentService enrollmentService;

    // --- Mocks for ResultService ---
    @Mock
    private ResultRepository resultRepository;
    @InjectMocks
    private ResultService resultService;

    // ==========================================
    // UNIT TEST 1: Course Creation Success
    // ==========================================
    @Test
    @DisplayName("Unit Test 1: CourseService - successfully creates a course when code is unique")
    void testCreateCourse_Success() {
        CourseDto inputDto = new CourseDto();
        inputDto.setCourseCode("CSE 4101");
        inputDto.setCourseTitle("Software Testing & Quality Assurance");
        inputDto.setCredits(3.0);
        inputDto.setDescription("Verification, validation, and automated testing.");

        when(courseRepository.existsByCourseCode("CSE 4101")).thenReturn(false);

        Course savedCourse = new Course();
        savedCourse.setId(101L);
        savedCourse.setCourseCode("CSE 4101");
        savedCourse.setCourseTitle("Software Testing & Quality Assurance");
        savedCourse.setCredits(3.0);
        savedCourse.setDescription("Verification, validation, and automated testing.");

        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);

        CourseDto result = courseService.createCourse(inputDto);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals("CSE 4101", result.getCourseCode());
        assertEquals("Software Testing & Quality Assurance", result.getCourseTitle());
        assertEquals(3.0, result.getCredits());
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    // ==========================================
    // UNIT TEST 2: Course Creation Duplicate Code
    // ==========================================
    @Test
    @DisplayName("Unit Test 2: CourseService - throws BadRequestException when course code already exists")
    void testCreateCourse_DuplicateCode_ThrowsBadRequestException() {
        CourseDto inputDto = new CourseDto();
        inputDto.setCourseCode("CSE 3220");
        inputDto.setCourseTitle("Duplicate Course");
        inputDto.setCredits(1.5);

        when(courseRepository.existsByCourseCode("CSE 3220")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> courseService.createCourse(inputDto));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(courseRepository, never()).save(any(Course.class));
    }

    // ==========================================
    // UNIT TEST 3: Student Enrollment Success
    // ==========================================
    @Test
    @DisplayName("Unit Test 3: EnrollmentService - successfully enrolls a student in a course")
    void testEnrollStudent_Success() {
        EnrollmentDto inputDto = new EnrollmentDto();
        inputDto.setStudentId(1L);
        inputDto.setCourseId(2L);
        inputDto.setSemester("Spring 2026");

        User studentUser = new User("student_test", "pass", "Test Student", "test@kuet.ac.bd", Role.ROLE_STUDENT);
        Student student = new Student(studentUser, "2007010", "CSE", 2022, "+8801700000000");
        student.setId(1L);

        Course course = new Course("CSE 3200", "Database Systems", 3.0, "DB course", null);
        course.setId(2L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndSemester(1L, 2L, "Spring 2026")).thenReturn(false);

        Enrollment savedEnrollment = new Enrollment(student, course, "Spring 2026");
        savedEnrollment.setId(50L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(savedEnrollment);

        EnrollmentDto result = enrollmentService.enrollStudent(inputDto);

        assertNotNull(result);
        assertEquals(50L, result.getId());
        assertEquals("Spring 2026", result.getSemester());
        assertEquals(1L, result.getStudentId());
        assertEquals(2L, result.getCourseId());
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    // ==========================================
    // UNIT TEST 4: Duplicate Enrollment Validation
    // ==========================================
    @Test
    @DisplayName("Unit Test 4: EnrollmentService - throws BadRequestException on duplicate enrollment")
    void testEnrollStudent_AlreadyEnrolled_ThrowsBadRequestException() {
        EnrollmentDto inputDto = new EnrollmentDto();
        inputDto.setStudentId(1L);
        inputDto.setCourseId(2L);
        inputDto.setSemester("Fall 2026");

        Student student = new Student();
        student.setId(1L);
        Course course = new Course();
        course.setId(2L);
        course.setCourseCode("CSE 3220");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndSemester(1L, 2L, "Fall 2026")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> enrollmentService.enrollStudent(inputDto));

        assertTrue(ex.getMessage().contains("already enrolled"));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    // ==========================================
    // UNIT TEST 5: Result Grading & GPA Computation
    // ==========================================
    @Test
    @DisplayName("Unit Test 5: ResultService - correctly computes letter grade 'A+' and GPA 4.00 for marks >= 80")
    void testSaveOrUpdateResult_ComputesGradeAndPointCorrectly() {
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", "admin123", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        Course course = new Course("CSE 3220", "Software Dev Lab", 1.5, "Desc", null);
        Enrollment enrollment = new Enrollment(new Student(), course, "Fall 2026");
        enrollment.setId(10L);

        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(enrollment));
        when(resultRepository.findByEnrollmentId(10L)).thenReturn(Optional.empty());

        when(resultRepository.save(any(Result.class))).thenAnswer(invocation -> {
            Result r = invocation.getArgument(0);
            r.setId(99L);
            return r;
        });

        ResultDto inputDto = new ResultDto();
        inputDto.setEnrollmentId(10L);
        inputDto.setMarks(85.5);
        inputDto.setRemarks("Excellent performance");

        ResultDto result = resultService.saveOrUpdateResult(inputDto, adminAuth);

        assertNotNull(result);
        assertEquals(85.5, result.getMarks());
        assertEquals("A+", result.getLetterGrade());
        assertEquals(4.00, result.getGradePoint());
        assertEquals("Excellent performance", result.getRemarks());
        verify(resultRepository, times(1)).save(any(Result.class));
    }

    // ==========================================
    // UNIT TEST 6: Result Grading Authorization Check
    // ==========================================
    @Test
    @DisplayName("Unit Test 6: ResultService - throws UnauthorizedException when unauthorized teacher grades course")
    void testSaveOrUpdateResult_UnauthorizedTeacher_ThrowsUnauthorizedException() {
        // Teacher user dr_karim assigned to the course
        User assignedUser = new User("dr_karim", "pass", "Dr Karim", "karim@kuet.ac.bd", Role.ROLE_TEACHER);
        Teacher assignedTeacher = new Teacher(assignedUser, "CSE", "Professor", "+8801700000000");

        Course course = new Course("CSE 3220", "Software Dev Lab", 1.5, "Desc", assignedTeacher);
        Enrollment enrollment = new Enrollment(new Student(), course, "Fall 2026");
        enrollment.setId(10L);

        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(enrollment));

        // Authenticated user is a different teacher: dr_other
        Authentication teacherAuth = new UsernamePasswordAuthenticationToken(
                "dr_other", "pass", Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );

        ResultDto inputDto = new ResultDto();
        inputDto.setEnrollmentId(10L);
        inputDto.setMarks(75.0);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> resultService.saveOrUpdateResult(inputDto, teacherAuth));

        assertTrue(ex.getMessage().contains("Forbidden"));
        verify(resultRepository, never()).save(any(Result.class));
    }
}
