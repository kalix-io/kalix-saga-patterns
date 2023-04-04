package com.example.cinema.application;

import com.example.cinema.domain.SeatStatus;
import com.example.cinema.domain.ShowByReservation;
import com.example.cinema.domain.ShowCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class ShowCalls {

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.ofSeconds(10);

  public ResponseEntity<Void> createShow(String showId, String title) {
    int maxSeats = 100;

    return webClient.post().uri("/cinema-show/" + showId)
      .bodyValue(new ShowCommand.CreateShow(title, maxSeats))
      .retrieve()
      .toBodilessEntity()
      .block();
  }

  public ResponseEntity<SeatStatus> getSeatStatus(String showId, int seatNumber) {
    return webClient.get().uri("/cinema-show/" + showId + "/seat-status/" + seatNumber)
      .retrieve()
      .toEntity(SeatStatus.class)
      .block(timeout);
  }

  public ResponseEntity<Void> reserveSeat(String showId, String walletId, String reservationId, int seatNumber) {
    return webClient.patch().uri("/cinema-show/" + showId + "/reserve")
      .bodyValue(new ShowCommand.ReserveSeat(walletId, reservationId, seatNumber))
      .retrieve()
      .toBodilessEntity()
      .block(timeout);
  }

  public ResponseEntity<Void> cancelSeatReservation(String showId, String reservationId) {
    return webClient.patch().uri("/cinema-show/" + showId + "/cancel-reservation/" + reservationId)
      .retrieve()
      .toBodilessEntity()
      .block(timeout);
  }

  public ResponseEntity<ShowByReservation> getShowByReservation(String reservationId) {
    return webClient.get().uri("/show/by-reservation-id/" + reservationId)
      .retrieve()
      .toEntity(ShowByReservation.class)
      .onErrorResume(WebClientResponseException.class, error -> {
        if (error.getStatusCode().is4xxClientError()) {
          return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } else {
          return Mono.error(error);
        }
      })
      .block();
  }

}
