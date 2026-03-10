package com.bookmyshow.event.service.impl;

import com.bookmyshow.event.constant.EventConstants;
import com.bookmyshow.event.dto.TheaterRequest;
import com.bookmyshow.event.entity.Theater;
import com.bookmyshow.event.exception.EventServiceException;
import com.bookmyshow.event.repository.TheaterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TheaterServiceImplTest {

	@Mock
	private TheaterRepository theaterRepository;

	@InjectMocks
	private TheaterServiceImpl theaterService;

	@Test
	void createTheater_validRequest_shouldSaveTheater() {
		// Given
		TheaterRequest request = TheaterRequest.builder().name("PVR").address("Addr").build();
		Theater saved = Theater.builder().id(1L).name("PVR").address("Addr").build();
		when(theaterRepository.save(any(Theater.class))).thenReturn(saved);

		ArgumentCaptor<Theater> captor = ArgumentCaptor.forClass(Theater.class);

		// When
		Theater result = theaterService.createTheater(request);

		// THEN
		assertThat(result.getId()).isEqualTo(1L);
		verify(theaterRepository).save(captor.capture());
		assertThat(captor.getValue().getName()).isEqualTo("PVR");
	}

	@Test
	void getAllTheaters_shouldReturnRepositoryResults() {
		// Given
		when(theaterRepository.findAll()).thenReturn(List.of(Theater.builder().id(1L).name("PVR").build()));

		// When
		List<Theater> theaters = theaterService.getAllTheaters();

		// THEN
		assertThat(theaters).hasSize(1);
	}

	@Test
	void getTheaterById_missing_shouldThrowEventServiceException() {
		// Given
		when(theaterRepository.findById(10L)).thenReturn(Optional.empty());

		// When
		Throwable thrown = catchThrowable(() -> theaterService.getTheaterById(10L));

		// THEN
		assertThat(thrown).isInstanceOf(EventServiceException.class)
				.hasMessage(EventConstants.ERROR_THEATER_NOT_FOUND);
	}
}

