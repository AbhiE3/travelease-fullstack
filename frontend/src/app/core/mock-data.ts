export type TripStatus = "planning" | "upcoming" | "ongoing" | "completed";
export type TripType = "Solo" | "Couple" | "Family" | "Friends" | "Corporate";

export interface Trip {
  id: string;
  name: string;
  type: TripType;
  source: string;
  destination: string;
  area: string;
  startDate: string;
  endDate: string;
  budgetPerPerson: number;
  members: number;
  currentCost: number;
  status: TripStatus;
  image: string;
  progress: number;
}

export const trips: Trip[] = [
  {
    id: "goa-2026",
    name: "Goa Beach Escape",
    type: "Friends",
    source: "Bengaluru",
    destination: "Goa",
    area: "Baga Beach",
    startDate: "2026-07-12",
    endDate: "2026-07-16",
    budgetPerPerson: 18000,
    members: 6,
    currentCost: 64200,
    status: "upcoming",
    image:
      "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?auto=format&fit=crop&w=1600&q=70",
    progress: 65,
  },
  {
    id: "manali-winter",
    name: "Manali Winter Trek",
    type: "Friends",
    source: "Delhi",
    destination: "Manali",
    area: "Old Manali",
    startDate: "2026-12-22",
    endDate: "2026-12-28",
    budgetPerPerson: 22000,
    members: 4,
    currentCost: 18400,
    status: "planning",
    image:
      "https://images.unsplash.com/photo-1626621341517-bbf3d9990a23?auto=format&fit=crop&w=1600&q=70",
    progress: 25,
  },
  {
    id: "kerala-family",
    name: "Kerala Backwaters",
    type: "Family",
    source: "Chennai",
    destination: "Alleppey",
    area: "Punnamada Lake",
    startDate: "2026-04-02",
    endDate: "2026-04-07",
    budgetPerPerson: 25000,
    members: 5,
    currentCost: 124500,
    status: "completed",
    image:
      "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?auto=format&fit=crop&w=1600&q=70",
    progress: 100,
  },
];

export const members = [
  { id: "1", name: "Sarathy R", email: "sarathy@example.com", status: "Accepted", role: "Organizer", avatar: "S" },
  { id: "2", name: "Raj Patel", email: "raj@example.com", status: "Accepted", role: "Traveler", avatar: "R" },
  { id: "3", name: "Arun Kumar", email: "arun@example.com", status: "Accepted", role: "Traveler", avatar: "A" },
  { id: "4", name: "Priya Sharma", email: "priya@example.com", status: "Pending", role: "Traveler", avatar: "P" },
  { id: "5", name: "Neha Singh", email: "neha@example.com", status: "Pending", role: "Traveler", avatar: "N" },
  { id: "6", name: "Vikram Das", email: "vikram@example.com", status: "Rejected", role: "Traveler", avatar: "V" },
] as const;

export const buses = [
  {
    id: "b1",
    name: "Volvo Multi-Axle Sleeper",
    operator: "VRL Travels",
    departure: "21:30",
    arrival: "08:45",
    seats: 12,
    price: 1850,
    rating: 4.6,
  },
  {
    id: "b2",
    name: "Mercedes Multi-Axle",
    operator: "SRS Travels",
    departure: "22:00",
    arrival: "09:30",
    seats: 8,
    price: 2100,
    rating: 4.4,
  },
  {
    id: "b3",
    name: "AC Sleeper (2+1)",
    operator: "Orange Tours",
    departure: "20:15",
    arrival: "07:45",
    seats: 4,
    price: 1600,
    rating: 4.1,
  },
];

export const hotels = [
  {
    id: "h1",
    name: "Sea Breeze Resort",
    area: "Baga Beach",
    distance: "0.3 km",
    capacity: 8,
    price: 4800,
    rating: 4.7,
    rooms: 3,
    image: "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=1200&q=70",
  },
  {
    id: "h2",
    name: "Calangute Grand",
    area: "Calangute",
    distance: "1.2 km",
    capacity: 6,
    price: 5400,
    rating: 4.5,
    rooms: 2,
    image: "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=70",
  },
  {
    id: "h3",
    name: "Vagator Beach Stay",
    area: "Vagator",
    distance: "0.8 km",
    capacity: 10,
    price: 3900,
    rating: 4.3,
    rooms: 4,
    image: "https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=1200&q=70",
  },
];

export const expenses = [
  { id: "e1", name: "Water Sports", amount: 3000, paidBy: "Sarathy", participants: ["Raj", "Arun", "Sarathy"], status: "Pending" },
  { id: "e2", name: "Dinner at Britto's", amount: 4200, paidBy: "Raj", participants: ["Raj", "Arun", "Sarathy", "Priya"], status: "Pending" },
  { id: "e3", name: "Scooter Rental", amount: 1500, paidBy: "Arun", participants: ["Arun", "Sarathy"], status: "Paid" },
  { id: "e4", name: "Bus Tickets", amount: 11100, paidBy: "Sarathy", participants: ["Raj", "Arun", "Sarathy", "Priya", "Neha", "Vikram"], status: "Pending" },
];

export const itinerary = [
  { day: 1, date: "Jul 12", title: "Arrival & Check-in", items: [{ time: "09:00", name: "Arrive at Goa", location: "Madgaon" }, { time: "12:00", name: "Hotel Check-in", location: "Sea Breeze Resort" }, { time: "18:00", name: "Sunset Walk", location: "Baga Beach" }] },
  { day: 2, date: "Jul 13", title: "Beach Day", items: [{ time: "08:00", name: "Breakfast", location: "Hotel" }, { time: "10:00", name: "Beach Visit", location: "Calangute" }, { time: "20:00", name: "Beach Shack Dinner", location: "Britto's" }] },
  { day: 3, date: "Jul 14", title: "Adventure", items: [{ time: "09:00", name: "Water Sports", location: "Baga Beach" }, { time: "15:00", name: "Spice Plantation Tour", location: "Ponda" }] },
  { day: 4, date: "Jul 15", title: "Departure", items: [{ time: "11:00", name: "Hotel Check-out", location: "Sea Breeze Resort" }, { time: "21:30", name: "Bus to Bengaluru", location: "Madgaon" }] },
];

export const alerts = [
  { id: "a1", level: "Critical" as const, title: "Bus Delayed by 180 Minutes", desc: "VRL Travels has reported a 3-hour delay due to heavy rains on NH-48.", impact: "Arrival pushed to 11:45. Day 1 sunset walk likely missed.", action: "Reschedule sunset walk to Day 2 evening." },
  { id: "a2", level: "Medium" as const, title: "Weather Advisory — Baga Beach", desc: "Possible thunderstorms forecast for Jul 13 afternoon.", impact: "Water sports may be paused 2–5 PM.", action: "Move water sports to morning slot." },
  { id: "a3", level: "Low" as const, title: "Hotel Confirmation Pending", desc: "Sea Breeze Resort confirmation expected by 6 PM.", impact: "No action required yet.", action: "Auto-retry in 2 hours." },
];

export const notifications = [
  { id: "n1", type: "invitation", title: "Trip Invitation", desc: "Sarathy invited you to Goa Beach Escape.", time: "2h ago" },
  { id: "n2", type: "expense", title: "Expense Settlement", desc: "Raj marked ₹700 as paid.", time: "5h ago" },
  { id: "n3", type: "budget", title: "Budget Warning", desc: "Goa trip at 89% of total budget.", time: "1d ago" },
  { id: "n4", type: "delay", title: "Delay Alert", desc: "Bus delayed by 180 minutes.", time: "1d ago" },
  { id: "n5", type: "booking", title: "Booking Confirmation", desc: "Sea Breeze Resort booking confirmed.", time: "2d ago" },
];

export const invitations = [
  { id: "i1", trip: "Goa Beach Escape", organizer: "Sarathy R", dates: "Jul 12 – Jul 16", members: 6 },
  { id: "i2", trip: "Pondicherry Weekend", organizer: "Anjali V", dates: "Aug 8 – Aug 10", members: 4 },
];

export const activities = [
  { id: "ac1", name: "Paragliding", destination: "Goa", duration: "1 hr", price: 2500, rating: 4.7, image: "https://images.unsplash.com/photo-1601024445121-e5b82f020549?auto=format&fit=crop&w=800&q=60" },
  { id: "ac2", name: "Scuba Diving", destination: "Goa", duration: "3 hrs", price: 4500, rating: 4.8, image: "https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=800&q=60" },
  { id: "ac3", name: "Jet Ski Ride", destination: "Goa", duration: "30 min", price: 1500, rating: 4.5, image: "https://images.unsplash.com/photo-1530541930197-ff16ac917b0e?auto=format&fit=crop&w=800&q=60" },
  { id: "ac4", name: "Banana Boat Ride", destination: "Goa", duration: "20 min", price: 800, rating: 4.3, image: "https://images.unsplash.com/photo-1502933691298-84fc14542831?auto=format&fit=crop&w=800&q=60" },
  { id: "ac5", name: "Spice Plantation Tour", destination: "Goa", duration: "4 hrs", price: 1200, rating: 4.4, image: "https://images.unsplash.com/photo-1532465614-6cc8d45f647f?auto=format&fit=crop&w=800&q=60" },
  { id: "ac6", name: "Dolphin Cruise", destination: "Goa", duration: "2 hrs", price: 900, rating: 4.2, image: "https://images.unsplash.com/photo-1568430462989-44163eb1752f?auto=format&fit=crop&w=800&q=60" },
];

export const routeAnalytics = [
  { route: "Chennai → Goa", bookings: 245, revenue: 450000, cancellation: 4, duration: "14h 30m" },
  { route: "Bengaluru → Goa", bookings: 312, revenue: 578000, cancellation: 3, duration: "11h 15m" },
  { route: "Delhi → Manali", bookings: 198, revenue: 412000, cancellation: 6, duration: "12h 45m" },
  { route: "Chennai → Pondicherry", bookings: 287, revenue: 198000, cancellation: 2, duration: "3h 20m" },
  { route: "Mumbai → Pune", bookings: 421, revenue: 295000, cancellation: 3, duration: "3h 10m" },
  { route: "Hyderabad → Hampi", bookings: 142, revenue: 184000, cancellation: 5, duration: "8h 30m" },
  { route: "Delhi → Jaipur", bookings: 256, revenue: 224000, cancellation: 4, duration: "5h 10m" },
  { route: "Bengaluru → Coorg", bookings: 89, revenue: 95000, cancellation: 9, duration: "6h 00m" },
  { route: "Kolkata → Darjeeling", bookings: 67, revenue: 142000, cancellation: 11, duration: "13h 20m" },
];

export const funnelStages = [
  { stage: "Search Destination", users: 1000, dropReason: "—" },
  { stage: "Trip Created", users: 700, dropReason: "Abandoned planning" },
  { stage: "Hotel Selected", users: 480, dropReason: "Hotel Unavailable / High Cost" },
  { stage: "Bus Selected", users: 360, dropReason: "Seat Unavailable" },
  { stage: "Booking Completed", users: 250, dropReason: "Payment Drop-off" },
];

export const dropReasons = [
  { reason: "High Cost", pct: 38 },
  { reason: "Seat Unavailable", pct: 22 },
  { reason: "Hotel Unavailable", pct: 18 },
  { reason: "User Abandoned", pct: 14 },
  { reason: "Payment Failure", pct: 8 },
];

export const hotelPartners = [
  { id: "hp1", name: "Sea Breeze Resort", city: "Goa", bookings: 184, cancellation: 3, rating: 4.7, revenue: 942000, status: "Active" },
  { id: "hp2", name: "Calangute Grand", city: "Goa", bookings: 142, cancellation: 5, rating: 4.5, revenue: 798000, status: "Active" },
  { id: "hp3", name: "Vagator Beach Stay", city: "Goa", bookings: 96, cancellation: 8, rating: 4.3, revenue: 412000, status: "Active" },
  { id: "hp4", name: "Manali Pine Lodge", city: "Manali", bookings: 124, cancellation: 4, rating: 4.6, revenue: 612000, status: "Active" },
  { id: "hp5", name: "Backwater Villa", city: "Alleppey", bookings: 58, cancellation: 12, rating: 3.8, revenue: 214000, status: "Review" },
];

export const transportPartners = [
  { id: "tp1", name: "VRL Travels", city: "Bengaluru", bookings: 412, cancellation: 4, rating: 4.6, revenue: 1240000, status: "Active" },
  { id: "tp2", name: "SRS Travels", city: "Chennai", bookings: 298, cancellation: 5, rating: 4.4, revenue: 894000, status: "Active" },
  { id: "tp3", name: "Orange Tours", city: "Hyderabad", bookings: 184, cancellation: 7, rating: 4.1, revenue: 512000, status: "Active" },
  { id: "tp4", name: "Kallada Travels", city: "Cochin", bookings: 76, cancellation: 14, rating: 3.6, revenue: 218000, status: "Review" },
];

export const activityPartners = [
  { id: "ap1", name: "Goa Watersports Co.", city: "Goa", bookings: 312, cancellation: 3, rating: 4.8, revenue: 624000, status: "Active" },
  { id: "ap2", name: "Manali Adventure Hub", city: "Manali", bookings: 198, cancellation: 5, rating: 4.6, revenue: 416000, status: "Active" },
  { id: "ap3", name: "Backwater Cruises", city: "Alleppey", bookings: 142, cancellation: 4, rating: 4.5, revenue: 298000, status: "Active" },
  { id: "ap4", name: "Spice Tours Pvt", city: "Goa", bookings: 58, cancellation: 11, rating: 3.9, revenue: 86000, status: "Review" },
];

export const pendingApprovals = [
  { id: "pa1", name: "Coral Reef Resort", type: "Hotel", registered: "2026-06-08", documents: 4, city: "Goa" },
  { id: "pa2", name: "MountainLine Buses", type: "Transport", registered: "2026-06-10", documents: 3, city: "Manali" },
  { id: "pa3", name: "SkyHigh Paragliding", type: "Activity", registered: "2026-06-11", documents: 5, city: "Bir" },
  { id: "pa4", name: "Sunset Stays", type: "Hotel", registered: "2026-06-12", documents: 4, city: "Pondicherry" },
  { id: "pa5", name: "ScubaWorld Goa", type: "Activity", registered: "2026-06-13", documents: 6, city: "Goa" },
];

export const hotelBookings = [
  { id: "hb1", guest: "Sarathy R", room: "Deluxe Sea View", checkIn: "Jul 12", checkOut: "Jul 16", guests: 2, total: 19200, status: "Confirmed" },
  { id: "hb2", guest: "Raj Patel", room: "Family Suite", checkIn: "Jul 12", checkOut: "Jul 16", guests: 4, total: 32800, status: "Confirmed" },
  { id: "hb3", guest: "Anjali V", room: "Standard Double", checkIn: "Jul 18", checkOut: "Jul 20", guests: 2, total: 7200, status: "Pending" },
  { id: "hb4", guest: "Vikram Das", room: "Deluxe Sea View", checkIn: "Jul 22", checkOut: "Jul 24", guests: 2, total: 9600, status: "Confirmed" },
];

export const rooms = [
  { id: "r1", type: "Deluxe Sea View", total: 12, available: 4, price: 4800 },
  { id: "r2", type: "Family Suite", total: 6, available: 1, price: 8200 },
  { id: "r3", type: "Standard Double", total: 18, available: 9, price: 3600 },
  { id: "r4", type: "Premium Villa", total: 4, available: 2, price: 12500 },
];

export const vehicles = [
  { id: "v1", name: "Volvo Multi-Axle Sleeper", reg: "KA-01-AB-1234", capacity: 36, status: "Active" },
  { id: "v2", name: "Mercedes Multi-Axle", reg: "KA-05-BD-9921", capacity: 32, status: "Active" },
  { id: "v3", name: "AC Sleeper 2+1", reg: "TN-22-CF-4412", capacity: 30, status: "Maintenance" },
  { id: "v4", name: "Volvo B11R", reg: "KA-03-EG-7765", capacity: 40, status: "Active" },
];

export const partnerRoutes = [
  { id: "pr1", route: "Bengaluru → Goa", departures: 14, occupancy: 88, revenue: 412000 },
  { id: "pr2", route: "Bengaluru → Coorg", departures: 7, occupancy: 64, revenue: 142000 },
  { id: "pr3", route: "Bengaluru → Hampi", departures: 5, occupancy: 72, revenue: 96000 },
  { id: "pr4", route: "Chennai → Bengaluru", departures: 21, occupancy: 91, revenue: 524000 },
];

export const providerActivities = [
  { id: "pa1", name: "Paragliding", slots: 12, booked: 9, price: 2500, rating: 4.7 },
  { id: "pa2", name: "Scuba Diving", slots: 8, booked: 6, price: 4500, rating: 4.8 },
  { id: "pa3", name: "Jet Ski Ride", slots: 20, booked: 14, price: 1500, rating: 4.5 },
  { id: "pa4", name: "Banana Boat", slots: 24, booked: 11, price: 800, rating: 4.3 },
  { id: "pa5", name: "Dolphin Cruise", slots: 30, booked: 22, price: 900, rating: 4.2 },
];
