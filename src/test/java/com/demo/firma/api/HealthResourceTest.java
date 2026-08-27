package com.demo.firma.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class HealthResourceTest {
    @Test
    void healthEndpointWorks() {
        given().when().get("/api/health").then().statusCode(200).body("status", is("OK")).body("service", is("firma-api-demo"));
    }
}
