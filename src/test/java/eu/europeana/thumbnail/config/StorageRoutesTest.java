package eu.europeana.thumbnail.config;

import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.service.impl.IiifImageServerImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SpringBootTest(classes = {StorageRoutes.class})
@TestPropertySource("classpath:testroutes.properties")
public class StorageRoutesTest {

    @Autowired
    private StorageRoutes storageRoutes;

    @Test
    public void testExactMatch() {
        // first route, first name
        testFirstStorage(storageRoutes.getStorageService("unittest1"));

        // first route, second name
        testFirstStorage(storageRoutes.getStorageService("localhost:8081"));

        // second route
        testSecondStorage(storageRoutes.getStorageService("unittest2.europeana.eu"));

        // third route
        testThirdStorage(storageRoutes.getStorageService("unitest3"));
    }

    @Test
    public void testPartialMatch() {
        // matches with route2
        testSecondStorage(storageRoutes.getStorageService("my-unittest2-route"));
    }

    @Test
    public void testNoMatch() {
        // this doesn't match with route2 because we use contains on the configured name
        testFirstStorage(storageRoutes.getStorageService("test2"));
    }

    private void testFirstStorage(List<MediaStorageService> services) {
        assertNotNull(services);
        assertEquals(4, services.size());
        assertEquals("default", services.get(0).getName());
        assertEquals("prod1", services.get(1).getName());
        assertEquals("prod2", services.get(2).getName());
        assertEquals(IiifImageServerImpl.STORAGE_NAME, services.get(3).getName());
    }

    private void testSecondStorage(List<MediaStorageService> services) {
        assertNotNull(services);
        assertEquals(1, services.size());
        assertEquals("test2", services.get(0).getName());
    }

    private void testThirdStorage(List<MediaStorageService> services) {
        assertNotNull(services);
        assertEquals(1, services.size());
        assertEquals("default", services.get(0).getName());
    }
}