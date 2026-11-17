package com.neusoft.cloudbrain;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BcryptHashGenerator {

    @Test
    void printHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("admin123=" + encoder.encode("admin123"));
        System.out.println("doctor123=" + encoder.encode("doctor123"));
        System.out.println("patient123=" + encoder.encode("patient123"));
    }
}
