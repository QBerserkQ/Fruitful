package volodea.fruitful;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import volodea.fruitful.config.security.RestAccessDeniedHandler;
import volodea.fruitful.config.security.RestAuthenticationEntryPoint;
import volodea.fruitful.config.security.SecurityErrorWriter;

class SecurityHandlersTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final SecurityErrorWriter writer = new SecurityErrorWriter(objectMapper);
    private final RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(writer);
    private final RestAccessDeniedHandler deniedHandler = new RestAccessDeniedHandler(writer);

    @Test
    void entryPoint_returns401JsonWithBearerChallenge() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/users/me");
        var response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("secret internal detail"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");

        String body = response.getContentAsString();
        assertThat((Integer) JsonPath.read(body, "$.status")).isEqualTo(401);
        assertThat((String) JsonPath.read(body, "$.message")).isEqualTo("Authentication required");
        assertThat(body).doesNotContain("secret internal detail");
    }

    @Test
    void accessDeniedHandler_returns403Json() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/admin/users");
        var response = new MockHttpServletResponse();

        deniedHandler.handle(request, response, new AccessDeniedException("nope"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");

        String body = response.getContentAsString();
        assertThat((Integer) JsonPath.read(body, "$.status")).isEqualTo(403);
        assertThat((String) JsonPath.read(body, "$.message")).isEqualTo("Access denied");
    }
}