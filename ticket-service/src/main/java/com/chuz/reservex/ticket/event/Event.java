package com.chuz.reservex.ticket.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 이벤트 엔티티 (콘서트, 공연, 스포츠 경기 등)
 */
@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name; // 이벤트명

  @Column(columnDefinition = "TEXT")
  private String description; // 설명

  @Column(nullable = false)
  private LocalDateTime eventDate; // 이벤트 날짜

  @Column(nullable = false)
  private String venue; // 장소

  @Column(nullable = false)
  private LocalDateTime saleStartAt; // 판매 시작 시간

  @Column(nullable = false)
  private LocalDateTime saleEndAt; // 판매 종료 시간

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EventStatus status; // 상태

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (status == null) {
      status = EventStatus.SCHEDULED;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum EventStatus {
    SCHEDULED,   // 예정
    SELLING,     // 판매중
    SOLD_OUT,    // 매진
    ENDED,       // 종료
    CANCELLED    // 취소
  }
}
