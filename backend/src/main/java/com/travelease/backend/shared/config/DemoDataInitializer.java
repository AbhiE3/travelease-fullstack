package com.travelease.backend.shared.config;

import com.travelease.backend.admin.entity.Destination;
import com.travelease.backend.admin.repository.DestinationRepository;
import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.busbooking.entity.Bus;
import com.travelease.backend.busbooking.entity.BusSchedule;
import com.travelease.backend.busbooking.entity.Route;
import com.travelease.backend.busbooking.entity.enums.BusType;
import com.travelease.backend.busbooking.repository.BusRepository;
import com.travelease.backend.busbooking.repository.BusScheduleRepository;
import com.travelease.backend.busbooking.repository.RouteRepository;
import com.travelease.backend.expense.entity.Expense;
import com.travelease.backend.expense.entity.ExpenseParticipant;
import com.travelease.backend.expense.repository.ExpenseRepository;
import com.travelease.backend.settlement.entity.Settlement;
import com.travelease.backend.settlement.entity.SettlementStatus;
import com.travelease.backend.settlement.repository.SettlementRepository;
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.entity.TravelerTripStatus;
import com.travelease.backend.trip.entity.TripMember;
import com.travelease.backend.trip.entity.TripMemberStatus;
import com.travelease.backend.trip.repository.TripMemberRepository;
import com.travelease.backend.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements CommandLineRunner {

    public static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final UUID ALICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID BOB_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID CARA_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID TRANSPORT_PROVIDER_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    public static final UUID HOTEL_PROVIDER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    public static final UUID ACTIVITY_PROVIDER_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    public static final UUID TRIP_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    public static final UUID EXPENSE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    public static final UUID SETTLEMENT_BOB_TO_ALICE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    public static final UUID SETTLEMENT_CARA_TO_ALICE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final PasswordEncoder passwordEncoder;
    
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final BusScheduleRepository busScheduleRepository;
    private final DestinationRepository destinationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedDestinations();

        User admin = getOrCreateUser(ADMIN_ID, "Admin User", "admin@travelease.test", "9000000000", Role.ROLE_ADMIN, null);
        User alice = getOrCreateUser(ALICE_ID, "Alice Traveler", "alice@travelease.test", "9000000001", Role.ROLE_TRAVELER, null);
        User bob = getOrCreateUser(BOB_ID, "Bob Traveler", "bob@travelease.test", "9000000002", Role.ROLE_TRAVELER, null);
        User cara = getOrCreateUser(CARA_ID, "Cara Traveler", "cara@travelease.test", "9000000003", Role.ROLE_TRAVELER, null);
        getOrCreateUser(TRANSPORT_PROVIDER_ID, "Priya Transport Provider", "provider@travelease.test", "9000000004", Role.ROLE_PROVIDER, 1L);
        getOrCreateUser(HOTEL_PROVIDER_ID, "Rahul Hotel Provider", "hotelprovider@travelease.test", "9000000005", Role.ROLE_HOTEL_PROVIDER, 1L);
        getOrCreateUser(ACTIVITY_PROVIDER_ID, "Meera Activity Provider", "activityprovider@travelease.test", "9000000006", Role.ROLE_ACTIVITY_PROVIDER, 1L);

        seedExpenseData(alice, bob, cara);
        seedBusBookingData(admin);
    }

    private void seedDestinations() {
        if (destinationRepository.count() > 0) {
            return;
        }
        saveDestination("Mumbai", "Maharashtra", "India", "Financial capital of India, gateway to the Arabian Sea.");
        saveDestination("Goa", "Goa", "India", "Beach paradise on India's west coast.");
        saveDestination("Manali", "Himachal Pradesh", "India", "Himalayan hill station popular for adventure sports.");
        saveDestination("Jaipur", "Rajasthan", "India", "The Pink City, known for its forts and palaces.");
        saveDestination("Alleppey", "Kerala", "India", "Backwaters and houseboat cruises.");
        saveDestination("Chennai", "Tamil Nadu", "India", "Cultural capital of South India.");
        saveDestination("Coorg", "Karnataka", "India", "Coffee plantations in the Western Ghats.");
    }

    private void saveDestination(String name, String state, String country, String description) {
        Destination destination = new Destination();
        destination.setDestinationName(name);
        destination.setState(state);
        destination.setCountry(country);
        destination.setDescription(description);
        destinationRepository.save(destination);
    }

    private void seedExpenseData(User alice, User bob, User cara) {
        if (tripRepository.existsById(TRIP_ID)) {
            return;
        }

        Trip trip = new Trip();
        trip.setId(TRIP_ID);
        trip.setTripName("Demo Goa Trip");
        trip.setOrganizer(alice);
        trip.setSourceLocation("Mumbai");
        trip.setDestinationId(2);
        trip.setBudgetAmount(new BigDecimal("1000.00"));
        trip.setCategoryId(1);
        trip.setStartDate(LocalDate.now().plusDays(10));
        trip.setEndDate(LocalDate.now().plusDays(14));
        trip.setStatus(TravelerTripStatus.PLANNING);
        trip = tripRepository.save(trip);

        addTripMember(trip, alice, "1000.00", "100.00");
        addTripMember(trip, bob, "1000.00", "100.00");
        addTripMember(trip, cara, "1000.00", "100.00");

        Expense dinner = new Expense();
        dinner.setId(EXPENSE_ID);
        dinner.setTrip(trip);
        dinner.setPayer(alice);
        dinner.setAmount(new BigDecimal("300.00"));
        dinner.setCategory("Food");
        dinner.setExpenseDate(LocalDate.now());
        dinner.setDescription("Demo shared dinner expense");
        addExpenseParticipant(dinner, alice, "100.00");
        addExpenseParticipant(dinner, bob, "100.00");
        addExpenseParticipant(dinner, cara, "100.00");
        expenseRepository.save(dinner);

        addSettlement(SETTLEMENT_BOB_TO_ALICE_ID, trip, bob, alice, "100.00");
        addSettlement(SETTLEMENT_CARA_TO_ALICE_ID, trip, cara, alice, "100.00");
    }

    private void seedBusBookingData(User admin) {
        if (busRepository.count() > 0) {
            return;
        }

        Route route = new Route();
        route.setSource("Mumbai");
        route.setDestination("Pune");
        route.setDistanceKm(150.0);
        route.setDurationHours(3.5);
        route = routeRepository.save(route);

        Bus bus = new Bus();
        bus.setBusNumber("MH-01-AB-1234");
        bus.setBusName("Volvo Demo Sleeper");
        bus.setTotalSeats(40);
        bus.setBusType(BusType.AC_SLEEPER);
        bus.setProviderId(1L); // Mock provider ID for testing
        bus.setAmenities(List.of("WiFi", "Blanket", "Water Bottle"));
        bus = busRepository.save(bus);

        BusSchedule schedule = new BusSchedule();
        schedule.setBus(bus);
        schedule.setRoute(route);
        schedule.setTravelDate(LocalDate.now().plusDays(5));
        schedule.setDepartureTime(LocalTime.of(8, 0));
        schedule.setArrivalTime(LocalTime.of(11, 30));
        schedule.setFare(500.0);
        schedule.setAvailableSeats(bus.getTotalSeats());
        busScheduleRepository.save(schedule);
    }

    private User getOrCreateUser(UUID id, String name, String email, String phone, Role role, Long providerId) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setId(id);
            user.setName(name);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPasswordHash(passwordEncoder.encode("password123"));
            user.setRole(role);
            user.setProviderId(providerId);
            return userRepository.save(user);
        });
    }

    private void addTripMember(Trip trip, User user, String budgetAmount, String spentAmount) {
        TripMember member = new TripMember();
        member.setTrip(trip);
        member.setUser(user);
        member.setMemberStatus(TripMemberStatus.ACCEPTED);
        member.setJoinedDate(LocalDateTime.now());
        member.setBudgetAmount(new BigDecimal(budgetAmount));
        member.setSpentAmount(new BigDecimal(spentAmount));
        tripMemberRepository.save(member);
    }

    private void addExpenseParticipant(Expense expense, User user, String shareAmount) {
        ExpenseParticipant participant = new ExpenseParticipant();
        participant.setExpense(expense);
        participant.setUser(user);
        participant.setShareAmount(new BigDecimal(shareAmount));
        expense.getParticipants().add(participant);
    }

    private void addSettlement(UUID id, Trip trip, User payer, User receiver, String amount) {
        Settlement settlement = new Settlement();
        settlement.setId(id);
        settlement.setTrip(trip);
        settlement.setPayer(payer);
        settlement.setReceiver(receiver);
        settlement.setAmount(new BigDecimal(amount));
        settlement.setStatus(SettlementStatus.PENDING);
        settlementRepository.save(settlement);
    }
}
