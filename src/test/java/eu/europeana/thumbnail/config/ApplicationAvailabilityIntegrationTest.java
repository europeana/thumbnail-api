package eu.europeana.thumbnail.config;

import eu.europeana.thumbnail.health.S3HealthChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.AFTER_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by luthien on 05/06/2023.
 */
// Disable the S3 health check bean in this integration test environment

@SpringBootTest(properties = "management.endpoint.health.validate-group-membership=false")
@AutoConfigureMockMvc
@TestPropertySource("classpath:testroutes.properties")
@SuppressWarnings("java:S5786")
public class ApplicationAvailabilityIntegrationTest {

    // 1. Inject a Mock version of your S3 health indicator bean named "s3"
    @MockitoBean(name = "s3")
    private S3HealthChecker s3HealthChecker;

    @BeforeEach
    void setUp() {
        // 2. Instruct the mock bean to always return an UP health status during the test
        Mockito.when(s3HealthChecker.health())
                .thenReturn(Health.up().withDetail("status", "Mocked for integration testing").build());
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationContext context;
    @Autowired private ApplicationAvailability applicationAvailability;

    @Test
    public void givenApplication_whenStarted_thenShouldBeAbleToRetrieveReadinessAndLiveness() {
        assertThat(applicationAvailability.getLivenessState()).isEqualTo(LivenessState.CORRECT);
        assertThat(applicationAvailability.getReadinessState()).isEqualTo(ReadinessState.ACCEPTING_TRAFFIC);
        assertThat(applicationAvailability.getState(ReadinessState.class)).isEqualTo(ReadinessState.ACCEPTING_TRAFFIC);
    }

    @Test
    @DirtiesContext(methodMode = AFTER_METHOD)
    public void givenCorrectState_whenPublishingTheEvent_thenShouldTransitToBrokenState() throws Exception {
        assertThat(applicationAvailability.getLivenessState()).isEqualTo(LivenessState.CORRECT);
        mockMvc.perform(get("/actuator/health/liveness"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("UP"));

        AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);

        assertThat(applicationAvailability.getLivenessState()).isEqualTo(LivenessState.BROKEN);
        mockMvc.perform(get("/actuator/health/liveness"))
               .andExpect(status().isServiceUnavailable())
               .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    @DirtiesContext(methodMode = AFTER_METHOD)
    public void givenAcceptingState_whenPublishingTheEvent_thenShouldTransitToRefusingState() throws Exception {
        assertThat(applicationAvailability.getReadinessState()).isEqualTo(ReadinessState.ACCEPTING_TRAFFIC);
        mockMvc.perform(get("/actuator/health/readiness"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("UP"));

        AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);

        assertThat(applicationAvailability.getReadinessState()).isEqualTo(ReadinessState.REFUSING_TRAFFIC);
        mockMvc.perform(get("/actuator/health/readiness"))
               .andExpect(status().isServiceUnavailable())
               .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
    }

}
