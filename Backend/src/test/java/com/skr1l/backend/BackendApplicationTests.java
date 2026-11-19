package com.skr1l.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
  "spring.datasource.url=jdbc:h2:mem:contexttest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
  "spring.datasource.driver-class-name=org.h2.Driver",
  "spring.datasource.username=sa",
  "spring.datasource.password=",
  "spring.jpa.hibernate.ddl-auto=create-drop",
  "spring.liquibase.enabled=false",
  "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
  "vision.base-url=http://localhost:8000",
  "vision.image-directory=target/test-camera-images"
})
class BackendApplicationTests {

  @Test
  void contextLoads() {}
}
