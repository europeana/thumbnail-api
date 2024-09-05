package eu.europeana.thumbnail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic test for loading context
 */
@SpringBootTest
@TestPropertySource("classpath:testroutes.properties")
class ThumbnailApplicationTest {

    @SuppressWarnings("java:S1186") // we are aware that this test doesn't have any assertion
    @Test
    void contextLoads() {
    }

}
