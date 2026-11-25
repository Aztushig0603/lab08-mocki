package mn.edu.must.sqat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock
    StudentRepository repository;

    @Mock
    EmailSender emailSender;

    CourseRegistrationService service;

    private static final String STUDENT_ID = "S001";
    private static final String COURSE_ID = "CS313";
    private static final Student ACTIVE_STUDENT =
            new Student(STUDENT_ID, "student@example.com", true, List.of("CS101"));

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    @Test
    void activeStudentWithCompletedPrerequisites_registersSuccessfully() {
        org.mockito.Mockito.when(repository.findStudent(STUDENT_ID)).thenReturn(ACTIVE_STUDENT);
        org.mockito.Mockito.when(repository.courseExists(COURSE_ID)).thenReturn(true);
        org.mockito.Mockito.when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS101"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq("student@example.com"), anyString(), contains(COURSE_ID));
    }

    @Test
    void unknownStudent_registersNothingAndSendsNoEmail() {
        org.mockito.Mockito.when(repository.findStudent(STUDENT_ID)).thenReturn(null);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    void inactiveStudent_doesNotInteractWithFurtherDependencies() {
        Student inactiveStudent = new Student(STUDENT_ID, "student@example.com", false, List.of());
        org.mockito.Mockito.when(repository.findStudent(STUDENT_ID)).thenReturn(inactiveStudent);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verifyNoInteractions(emailSender);
        verify(repository, never()).courseExists(anyString());
        verify(repository, never()).prerequisites(anyString());
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    @Test
    void missingPrerequisite_returnsErrorWithoutSavingRegistration() {
        org.mockito.Mockito.when(repository.findStudent(STUDENT_ID)).thenReturn(ACTIVE_STUDENT);
        org.mockito.Mockito.when(repository.courseExists(COURSE_ID)).thenReturn(true);
        org.mockito.Mockito.when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS101", "CS201"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    void emailFailure_registrationStillSucceedsAndIsSaved() {
        org.mockito.Mockito.when(repository.findStudent(STUDENT_ID)).thenReturn(ACTIVE_STUDENT);
        org.mockito.Mockito.when(repository.courseExists(COURSE_ID)).thenReturn(true);
        org.mockito.Mockito.when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS101"));
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(anyString(), anyString(), anyString());
    }

    @Test
    void nullStudentIdOrCourseId_throwsIllegalArgumentExceptionWithoutDependencyCalls() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, COURSE_ID));
        assertThrows(IllegalArgumentException.class, () -> service.register(STUDENT_ID, null));

        verifyNoInteractions(repository, emailSender);
    }

    @Test
    void successfulRegistration_emailBodyContainsCourseCode() {
        org.mockito.Mockito.when(repository.findStudent(STUDENT_ID)).thenReturn(ACTIVE_STUDENT);
        org.mockito.Mockito.when(repository.courseExists(COURSE_ID)).thenReturn(true);
        org.mockito.Mockito.when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS101"));
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(emailSender).send(eq("student@example.com"), anyString(), bodyCaptor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(bodyCaptor.getValue().contains(COURSE_ID));
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
    }

    @ParameterizedTest(name = "register with {0} returns {1}")
    @CsvSource({
            "NO_STUDENT, ERROR_NO_STUDENT",
            "INACTIVE_STUDENT, ERROR_INACTIVE_STUDENT",
            "NO_COURSE, ERROR_NO_COURSE"
    })
    void invalidRegistrationState_returnsExpectedError(String scenario, String expectedResult) {
        // Parameterized мөр бүр өөр замаар явдаг тул тухайн мөрөнд ашиглагдахгүй stub-ийг lenient болгов.
        lenient().when(repository.findStudent(STUDENT_ID)).thenReturn(
                "NO_STUDENT".equals(scenario) ? null :
                        new Student(STUDENT_ID, "student@example.com", !"INACTIVE_STUDENT".equals(scenario), List.of("CS101")));
        lenient().when(repository.courseExists(COURSE_ID)).thenReturn(!"NO_COURSE".equals(scenario));
        lenient().when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS101"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals(expectedResult, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}
