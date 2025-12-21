package kr.hhplus.be.server.common.security.user;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserUuidResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserUuid.class)
                && parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        // SecurityUtils에서 Optional을 반환하더라도 여기서 최종적으로 String으로 강제한다.
        // 토큰이 없거나 인증이 안 된 경우는 401로 내려주는 게 맞으므로 예외를 던진다.
        return SecurityUtils.getCurrentUserUuid()
                .orElseThrow(() -> new IllegalStateException("Invalid or missing token"));
    }
}