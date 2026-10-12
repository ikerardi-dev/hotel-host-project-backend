package com.hotelhost.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hotelhost.backend.dto.UpdateCleaningRequest;
import com.hotelhost.backend.entity.Apartment;
import com.hotelhost.backend.entity.Building;
import com.hotelhost.backend.entity.Cleaning;
import com.hotelhost.backend.entity.CleaningChecklistItem;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.exception.ApiException;
import com.hotelhost.backend.repository.BookingRepository;
import com.hotelhost.backend.repository.CleaningRepository;
import com.hotelhost.backend.security.CurrentUser;

// Test unitario del flujo de las limpiezas y de sus permisos: los repositorios son mocks
@ExtendWith(MockitoExtension.class)
class CleaningServiceTest {

	private static final Long CLEANING_ID = 20L;
	private static final Long ITEM_ID = 100L;
	private static final CurrentUser MARIA = new CurrentUser(5L, false);
	private static final CurrentUser JAVIER = new CurrentUser(6L, false);
	private static final CurrentUser ADMIN = new CurrentUser(1L, true);

	@Mock
	private CleaningRepository cleaningRepository;

	@Mock
	private BookingRepository bookingRepository;

	@Mock
	private EmployeeService employeeService;

	@InjectMocks
	private CleaningService cleaningService;

	private Apartment apartment;
	private User maria;
	private CleaningChecklistItem item;

	@BeforeEach
	void setUp() {
		Building building = mock(Building.class);
		lenient().when(building.getName()).thenReturn("Legazpi");
		apartment = mock(Apartment.class);
		lenient().when(apartment.getId()).thenReturn(10L);
		lenient().when(apartment.getCode()).thenReturn("A");
		lenient().when(apartment.getCapacity()).thenReturn(2);
		lenient().when(apartment.getBuilding()).thenReturn(building);

		maria = employee(MARIA.id(), "María");
		item = mock(CleaningChecklistItem.class);
		lenient().when(item.getId()).thenReturn(ITEM_ID);

		// Próxima entrada del apartamento: no la hay
		lenient().when(bookingRepository.findFirstByApartmentIdAndStatusAndCheckInGreaterThanEqualOrderByCheckInAsc(
				any(), any(), any())).thenReturn(Optional.empty());
	}

	@Test
	void startingAPendingCleaningRecordsWhenItStarted() {
		Cleaning cleaning = stored(CleaningStatus.PENDING);

		cleaningService.updateStatus(CLEANING_ID, CleaningStatus.IN_PROGRESS, MARIA);

		assertThat(cleaning.getStatus()).isEqualTo(CleaningStatus.IN_PROGRESS);
		assertThat(cleaning.getStartedAt()).isNotNull();
	}

	@Test
	void finishingACleaningRecordsWhenItFinished() {
		Cleaning cleaning = stored(CleaningStatus.IN_PROGRESS);

		cleaningService.updateStatus(CLEANING_ID, CleaningStatus.DONE, MARIA);

		assertThat(cleaning.getStatus()).isEqualTo(CleaningStatus.DONE);
		assertThat(cleaning.getFinishedAt()).isNotNull();
	}

	@Test
	void aCleaningCannotSkipStepsOrGoBack() {
		stored(CleaningStatus.PENDING);
		assertThatThrownBy(() -> cleaningService.updateStatus(CLEANING_ID, CleaningStatus.DONE, MARIA))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("INVALID_STATUS_TRANSITION");

		stored(CleaningStatus.DONE);
		assertThatThrownBy(() -> cleaningService.updateStatus(CLEANING_ID, CleaningStatus.IN_PROGRESS, MARIA))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("INVALID_STATUS_TRANSITION");
	}

	@Test
	void anEmployeeCannotTouchAnotherEmployeesCleaning() {
		Cleaning cleaning = stored(CleaningStatus.PENDING);

		assertThatThrownBy(() -> cleaningService.updateStatus(CLEANING_ID, CleaningStatus.IN_PROGRESS, JAVIER))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("CLEANING_NOT_ASSIGNED");
		assertThat(cleaning.getStatus()).isEqualTo(CleaningStatus.PENDING);
	}

	@Test
	void theAdminCanWorkOnAnyCleaning() {
		Cleaning cleaning = stored(CleaningStatus.PENDING);

		cleaningService.updateStatus(CLEANING_ID, CleaningStatus.IN_PROGRESS, ADMIN);

		assertThat(cleaning.getStatus()).isEqualTo(CleaningStatus.IN_PROGRESS);
	}

	@Test
	void checklistCanOnlyBeMarkedWhileTheCleaningIsInProgress() {
		stored(CleaningStatus.PENDING);

		assertThatThrownBy(() -> cleaningService.updateChecklistItem(CLEANING_ID, ITEM_ID, true, MARIA))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("CLEANING_NOT_IN_PROGRESS");
		verify(item, never()).setDone(true);
	}

	@Test
	void markingATaskOfACleaningInProgress() {
		stored(CleaningStatus.IN_PROGRESS);

		cleaningService.updateChecklistItem(CLEANING_ID, ITEM_ID, true, MARIA);

		verify(item).setDone(true);
	}

	@Test
	void aTaskFromAnotherCleaningIsNotFound() {
		stored(CleaningStatus.IN_PROGRESS);

		assertThatThrownBy(() -> cleaningService.updateChecklistItem(CLEANING_ID, 999L, true, MARIA))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("CHECKLIST_ITEM_NOT_FOUND");
	}

	@Test
	void anEmptyIncidentIsStoredAsNoIncident() {
		Cleaning cleaning = stored(CleaningStatus.IN_PROGRESS);
		cleaning.setIncidentNotes("Bombilla fundida");

		cleaningService.updateIncident(CLEANING_ID, "   ", MARIA);

		assertThat(cleaning.getIncidentNotes()).isNull();
	}

	@Test
	void adminReassignsAPendingCleaningToAnotherEmployeeAndTimeSlot() {
		Cleaning cleaning = stored(CleaningStatus.PENDING);
		User javier = employee(JAVIER.id(), "Javier");
		when(employeeService.findActiveCleaner(JAVIER.id())).thenReturn(javier);

		cleaningService.update(CLEANING_ID, new UpdateCleaningRequest(JAVIER.id(), LocalTime.of(10, 0), LocalTime.of(12, 0)));

		assertThat(cleaning.getAssignedTo()).isSameAs(javier);
		assertThat(cleaning.getStartTime()).isEqualTo(LocalTime.of(10, 0));
		assertThat(cleaning.getEndTime()).isEqualTo(LocalTime.of(12, 0));
	}

	@Test
	void aStartedCleaningCannotBeReassigned() {
		Cleaning cleaning = stored(CleaningStatus.IN_PROGRESS);

		assertThatThrownBy(() -> cleaningService.update(CLEANING_ID,
				new UpdateCleaningRequest(null, LocalTime.of(10, 0), LocalTime.of(12, 0))))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("CLEANING_NOT_EDITABLE");
		assertThat(cleaning.getAssignedTo()).isSameAs(maria);
	}

	// Una limpieza de María guardada con el estado indicado y una tarea en la checklist
	private Cleaning stored(CleaningStatus status) {
		Cleaning cleaning = new Cleaning();
		cleaning.setApartment(apartment);
		cleaning.setAssignedTo(maria);
		cleaning.setCleaningDate(LocalDate.of(2030, 5, 14));
		cleaning.setStartTime(LocalTime.of(12, 0));
		cleaning.setEndTime(LocalTime.of(14, 0));
		cleaning.setStatus(status);
		cleaning.addChecklistItem(item);
		when(cleaningRepository.findById(CLEANING_ID)).thenReturn(Optional.of(cleaning));
		return cleaning;
	}

	private static User employee(Long id, String firstName) {
		User user = mock(User.class);
		lenient().when(user.getId()).thenReturn(id);
		lenient().when(user.getFirstName()).thenReturn(firstName);
		lenient().when(user.getLastName()).thenReturn("Test");
		return user;
	}
}
