# Лаборатори №8 — Mockito mock объектууд

**Хичээл:** F.CSA313 — Программ хангамжийн чанарын баталгаа ба тест  
**Оюутны нэр:** Азтүшиг
**Оюутны код:** B232270014

## 1. Ашигласан орчин

```text
java -version
java version "23.0.2" 2025-01-21
Java(TM) SE Runtime Environment (build 23.0.2+7-58)
Java HotSpot(TM) 64-Bit Server VM (build 23.0.2+7-58, mixed mode, sharing)

mvn -version
Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: C:\apache-maven-3.9.9
Java version: 23.0.2, vendor: Oracle Corporation, runtime: C:\jdk-23.0.2
Default locale: en_US, platform encoding: UTF-8
OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"
```

## 2. Тестийн тоо

CourseRegistrationServiceTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS

**Нийт 10 тест** (7 энгийн тест + A8 параметржүүлсэн тестийн 3 мөр).

Тестийн жагсаалт:

| № | Тест | Хэрэгсэл |
| --- | ------ | ---------- |
| A1 | `activeStudentWithCompletedPrerequisites_registersSuccessfully` | `when` + `verify` |
| A2 | `unknownStudent_registersNothingAndSendsNoEmail` | `verify(never())` |
| A3 | `inactiveStudent_doesNotInteractWithFurtherDependencies` | `verifyNoInteractions` |
| A4 | `missingPrerequisite_returnsErrorWithoutSavingRegistration` | `when` + `never` |
| A5 | `emailFailure_registrationStillSucceedsAndIsSaved` | `doThrow(...).when(...)` |
| A6 | `nullStudentIdOrCourseId_throwsIllegalArgumentExceptionWithoutDependencyCalls` | `assertThrows` |
| A7 | `successfulRegistration_emailBodyContainsCourseCode` | `ArgumentCaptor` |
| A8 | `invalidRegistrationState_returnsExpectedError` (3 мөр) | `@ParameterizedTest` + `@CsvSource` |

## 3. A8 — lenient() тайлбар

`@ParameterizedTest`-ийн мөр бүр өөр замаар явдаг (NO_STUDENT → эхний шалгалтад унана, NO_COURSE → гурав дахь шалгалтад унана) тул тухайн мөрөнд ашиглагдахгүй stub-ууд Mockito-гийн strict stubs горимд `UnnecessaryStubbingException` өгдөг. `lenient()` нь зөвхөн эдгээр нөхцөлд stub-ийг "заавал ашиглах" шаардлагаас чөлөөлж, тестийг унахгүйгээр ажиллуулах боломж олгоно.

## 4. Хэсэг B — Mutation testing

Мутант бүрийн лог файл `results/mvn-test-mutant-1..3.txt`-д хадгалагдсан.

| Мутант | Эвдрэл (эх код → мутант) | Унасан тестүүд | BUILD | Лог файл |
| -------- | -------------------------- | ---------------- | ------- | ---------- |
| **1** | `if (!student.active()) return "ERROR_INACTIVE_STUDENT";` мөрийг коммент болгосон | `inactiveStudent_doesNotInteractWithFurtherDependencies`, `invalidRegistrationState_returnsExpectedError[2]` (нийт 2 failure) | FAILURE | `results/mvn-test-mutant-1.txt` |
| **2** | `repository.saveRegistration(studentId, courseId);` мөрийг коммент болгосон | `activeStudentWithCompletedPrerequisites_registersSuccessfully`, `successfulRegistration_emailBodyContainsCourseCode`, `emailFailure_registrationStillSucceedsAndIsSaved` (нийт 3 failure) | FAILURE | `results/mvn-test-mutant-2.txt` |
| **3** | `if (studentId == null \|\| courseId == null)` → `if (studentId == null)` болгосон | `nullStudentIdOrCourseId_throwsIllegalArgumentExceptionWithoutDependencyCalls` (нийт 1 failure) | FAILURE | `results/mvn-test-mutant-3.txt` |

### Дүгнэлт (Хэсэг B)

Гурван мутант бүгд тестийн багцад **баригдсан (killed)** — мутант лог бүрд `Failures ≥ 1` ба `BUILD FAILURE` байна. Энэ нь тестүүд зөвхөн "ногоон" биш, харин бодит алдааг илрүүлэх чадвартай гэдгийг баталж байна.

Мутант 3-ыг анх бичихдээ A6 тест зөвхөн `studentId = null`-ийг шалгадаг байсан тул мутант амьд үлдэх эрсдэлтэй байсан. A6-г **хоёр аргументын null-ийг тус тусад нь шалгадаг** болгож зассаны дараа (`service.register(null, COURSE_ID)` ба `service.register(STUDENT_ID, null)`) мутант амжилттай баригдсан. Энэ нь тестийн цоорхойг илрүүлж, засах жишээ болсон.

Мутацийн дараа `CourseRegistrationService.java`-г `finally` блокоор **заавал сэргээсэн** бөгөөд эцсийн `mvn test` нь `BUILD SUCCESS` гарсан.

## 5. Дүгнэлт

Mock объект нь гадаад хамаарлыг (өгөгдлийн сан, SMTP сервер) бодит хэрэгжүүлэлтгүйгээр орлуулж, үйлчилгээний логикийг бүрэн тусгаарлагдсан нэгжийн түвшинд тестлэх боломж олгодог. Гараар бичсэн fake-ээс ялгаатай нь Mockito нь stub (`when/thenReturn`), верификаци (`verify/never`), алдаа дуурайлгах (`doThrow`), аргумент барих (`ArgumentCaptor`) зэрэг боломжийг нэг дор өгдөг тул тестийн код богино, уншихад хялбар, засварлахад уян хатан байдаг. Гэхдээ бүх зүйлийг mock хийх ёсгүй: `Student` гэх мэт энгийн утгын объект (value object), `List`, `String` зэрэг стандарт классуудыг mock хийх нь илүүц бөгөөд тестийг эмзэг болгодог — эдгээрийг бодит объектоор ашиглах хэрэгтэй. Мөн `verify`-г хэтрүүлэн ашиглавал тест нь хэрэгжүүлэлтийн дэлгэрэнгүй мэдээлэлд хэт холбогдож, рефакторинг хийхэд эвдрэмтгий болдог — зөвхөн бизнес үр дүнд чухал харилцааг л шалгах хэрэгтэй. Мутацийн тест нь тестүүд үнэхээр ажиллаж байгааг батлах хамгийн энгийн арга бөгөөд ирээдүйд үүнийг автоматжуулж (PIT, mutation testing) ашиглах нь тестийн чанарыг системтэйгээр хангах боломж олгоно. Энэхүү лабораторийн ажлын үр дүнд Mockito-гийн бүх үндсэн хэрэгслийг практикт ашиглаж, тестийн чанарыг мутацийн аргаар баталгаажуулах туршлага хуримтлууллаа.
