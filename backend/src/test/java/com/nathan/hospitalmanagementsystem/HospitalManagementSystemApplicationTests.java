package com.nathan.hospitalmanagementsystem;

import support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
public class HospitalManagementSystemApplicationTests {

    @Test
    void contextLoads() {

    }
}
