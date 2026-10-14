package com.manguonmo.popworld.dev;

import com.manguonmo.popworld.e2e.base.BaseE2ETest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {
    "server.port=8080",
    "app.init-demo-data=true"
})
public class LocalServerRunner extends BaseE2ETest {

    @Test
    void runServer() throws Exception {
        System.out.println("=================================================");
        System.out.println(" POPWORLD LOCAL SERVER READY ON http://localhost:8080");
        System.out.println("=================================================");
        Thread.sleep(600_000); // 10 minutes
    }
}
