package com.swari.sewa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import swari.sewa.SwariSewaApplication;

@SpringBootTest(classes = SwariSewaApplication.class, properties = "spring.profiles.active=test")
class SwariSewaBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
