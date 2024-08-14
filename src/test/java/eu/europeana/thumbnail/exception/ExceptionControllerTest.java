package eu.europeana.thumbnail.exception;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test json error responses
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class ExceptionControllerTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
    }

    /**
     *  Test if 404s return json response (and not default Spring Boot whitelist error)
     */
    @Test
    public void test404Json() {
        String path = "/not-exists";
        JsonPath response = given().
                header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).get(path).
                then().contentType(ContentType.JSON).extract().response().jsonPath();

        assertEquals("ErrorResponse", response.getString("type"));
        assertEquals(Boolean.FALSE, response.getBoolean("success"));
        assertEquals("404", response.getString("status"));
        assertNotNull(response.getString("timestamp"));
        assertTrue(response.getString("timestamp").length() > 0);
    }

}
