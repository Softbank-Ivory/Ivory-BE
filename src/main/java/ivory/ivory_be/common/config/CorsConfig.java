package ivory.ivory_be.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${ivory-fe.url}")
    private String IVORY_FE_URL;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000",IVORY_FE_URL) // 허용할 도메인 지정
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "HEAD", "OPTIONS","PUT") // 허용할 HTTP 메서드 지정
                .allowedHeaders("*") // 허용할 헤더 지정
                .exposedHeaders("Authorization")
                .allowCredentials(true);
    }
}
