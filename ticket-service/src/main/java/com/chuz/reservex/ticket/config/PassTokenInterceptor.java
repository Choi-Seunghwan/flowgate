package com.chuz.reservex.ticket.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Pass Token 검증 인터셉터
 */
@Component
@RequiredArgsConstructor
public class PassTokenInterceptor implements HandlerInterceptor {

  private final PassTokenValidator passTokenValidator;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    throws Exception {

  // HandlerMethod가 아니면 통과
  if (!(handler instanceof HandlerMethod handlerMethod)) {
    return true;
  }

  // @RequirePassToken 어노테이션이 없으면 통과
  RequirePassToken annotation = handlerMethod.getMethodAnnotation(RequirePassToken.class);
  if (annotation == null) {
    return true;
  }

  // Pass Token 검증
  String eventIdStr = request.getParameter("eventId");
  String clientId = request.getParameter("clientId");
  String passToken = request.getHeader("X-Pass-Token");

  if (eventIdStr == null || clientId == null || passToken == null) {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json");
    response.getWriter().write("{\"error\":\"Missing eventId, clientId, or X-Pass-Token header\"}");
    return false;
  }

  Long eventId;
  try {
    eventId = Long.parseLong(eventIdStr);
  } catch (NumberFormatException e) {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.setContentType("application/json");
    response.getWriter().write("{\"error\":\"Invalid eventId\"}");
    return false;
  }

  boolean valid = passTokenValidator.validateAndConsume(eventId, clientId, passToken);

  if (!valid) {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json");
    response.getWriter().write("{\"error\":\"Invalid or expired Pass Token\"}");
    return false;
  }

  return true;
  }
}
