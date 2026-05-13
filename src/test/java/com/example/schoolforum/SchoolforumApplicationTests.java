package com.example.schoolforum;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SchoolforumApplicationTests {

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void contextLoads() {
    }

}
